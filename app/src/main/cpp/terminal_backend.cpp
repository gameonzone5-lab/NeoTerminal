#include <jni.h>
#include <string>
#include <cstdio>
#include <iostream>
#include <vector>

extern "C" JNIEXPORT jstring JNICALL
Java_com_neoterminal_core_TerminalActivity_executeCommand(JNIEnv* env, jobject thiz, jstring command) {
    if (command == nullptr) {
        return env->NewStringUTF("Error: Null command");
    }

    const char* nativeCommand = env->GetStringUTFChars(command, nullptr);
    
    // Using popen to execute the command in the android shell
    FILE* pipe = popen(nativeCommand, "r");
    if (!pipe) {
        env->ReleaseStringUTFChars(command, nativeCommand);
        return env->NewStringUTF("Error: popen() failed to execute command.");
    }

    char buffer[128];
    std::string result = "";
    while (fgets(buffer, sizeof(buffer), pipe) != nullptr) {
        result += buffer;
    }

    pclose(pipe);
    env->ReleaseStringUTFChars(command, nativeCommand);

    if (result.empty()) {
        return env->NewStringUTF("Command executed (no output).\n");
    }

    return env->NewStringUTF(result.c_str());
}