#include <jni.h>
#include <string>
#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>

extern "C" JNIEXPORT jstring JNICALL
Java_com_neoterminal_core_TerminalActivity_executeCommand(JNIEnv* env, jobject /* this */, jstring command) {
    const char *cmd_str = env->GetStringUTFChars(command, nullptr);
    std::string input_cmd(cmd_str);

    setenv("ARCH", "aarch64", 1);
    setenv("PATH", "/system/bin:/system/xbin:/vendor/bin", 1);

    std::string full_cmd;
    if (input_cmd.rfind("sh ", 0) == 0) {
        full_cmd = "export ARCH=aarch64 && " + input_cmd + " 2>&1";
    } else {
        full_cmd = input_cmd + " 2>&1";
    }

    char buffer[256];
    std::string result = "";
    FILE* pipe = popen(full_cmd.c_str(), "r");
    if (pipe) {
        while (fgets(buffer, sizeof(buffer), pipe) != nullptr) {
            result += buffer;
            // Prevent OOM from infinite loops or massive progress bars
            if (result.length() > 50000) {
                result += "\n...[Output Truncated to 50KB]...\n";
                break;
            }
        }
        pclose(pipe);
    } else {
        result = "Error: popen() failed.";
    }

    env->ReleaseStringUTFChars(command, cmd_str);

    // BULLETPROOF SANITIZATION: Prevent JNI NewStringUTF Fatal Abort
    for (char& c : result) {
        // Cast to unsigned char to handle ARM architecture default sign correctly
        if ((unsigned char)c > 127) {
            c = '?'; // Strip non-ASCII/invalid UTF-8 bytes
        }
    }

    return env->NewStringUTF(result.empty() ? "\n" : result.c_str());
}
