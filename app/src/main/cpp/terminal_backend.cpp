#include <jni.h>
#include <string>
#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>

extern "C" JNIEXPORT jstring JNICALL
Java_com_neoterminal_core_TerminalActivity_executeCommand(JNIEnv* env, jobject /* this */, jstring command) {
    const char *cmd_str = env->GetStringUTFChars(command, nullptr);
    std::string input_cmd(cmd_str);

    // FORCE ARM64 ENVIRONMENT
    setenv("ARCH", "aarch64", 1);
    setenv("PATH", "/system/bin:/system/sbin:/system/xbin:/vendor/bin:/vendor/xbin", 1);

    // If it's a bash/sh script, run it with explicit architecture export
    std::string full_cmd;
    if (input_cmd.rfind("sh ", 0) == 0) {
        full_cmd = "export ARCH=aarch64 && " + input_cmd + " 2>&1";
    } else {
        full_cmd = input_cmd + " 2>&1";
    }

    char buffer[128];
    std::string result = "";
    FILE* pipe = popen(full_cmd.c_str(), "r");
    if (pipe) {
        while (fgets(buffer, sizeof(buffer), pipe) != nullptr) {
            result += buffer;
        }
        pclose(pipe);
    }

    env->ReleaseStringUTFChars(command, cmd_str);
    return env->NewStringUTF(result.empty() ? "\n" : result.c_str());
}