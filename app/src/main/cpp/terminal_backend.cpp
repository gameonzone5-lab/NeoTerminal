#include <jni.h>
#include <string>
#include <stdexcept>
#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>

extern "C" JNIEXPORT jstring JNICALL
Java_com_neoterminal_core_TerminalActivity_executeCommand(JNIEnv* env, jobject /* this */, jstring command) {
    const char *cmd_str = env->GetStringUTFChars(command, nullptr);

    // 1. Setup Environment Variables
    std::string home_dir = "/data/data/com.neoterminal.core/files";
    setenv("HOME", home_dir.c_str(), 1);

    // CRITICAL FIX: EXPLICITLY SET SYSTEM PATH SO COMMANDS LIKE 'ls', 'ping', 'top' ARE FOUND
    setenv("PATH", "/sbin:/system/sbin:/system/bin:/system/xbin:/vendor/bin:/vendor/xbin", 1);
    chdir(home_dir.c_str());

    // 2. Append ' 2>&1' to capture stderr alongside stdout
    std::string full_cmd = std::string(cmd_str) + " 2>&1";
    char buffer[256];
    std::string result = "";

    FILE* pipe = popen(full_cmd.c_str(), "r");
    if (!pipe) {
        env->ReleaseStringUTFChars(command, cmd_str);
        return env->NewStringUTF("Backend Error: popen() failed!\n");
    }

    try {
        while (fgets(buffer, sizeof(buffer), pipe) != nullptr) {
            result += buffer;
        }
    } catch (...) {
        pclose(pipe);
        env->ReleaseStringUTFChars(command, cmd_str);
        return env->NewStringUTF("Backend Error: Execution interrupted!\n");
    }

    pclose(pipe);
    env->ReleaseStringUTFChars(command, cmd_str);

    if (result.empty()) {
        return env->NewStringUTF("\n");
    }

    return env->NewStringUTF(result.c_str());
}