#include <jni.h>
#include <string>
#include <stdexcept>
#include <stdio.h>

extern "C" JNIEXPORT jstring JNICALL
Java_com_neoterminal_core_TerminalActivity_executeCommand(JNIEnv* env, jobject /* this */, jstring command) {
    const char *cmd = env->GetStringUTFChars(command, nullptr);
    char buffer[256];
    std::string result = "";
    FILE* pipe = popen(cmd, "r");
    if (!pipe) {
        env->ReleaseStringUTFChars(command, cmd);
        return env->NewStringUTF("popen() failed!");
    }
    try {
        while (fgets(buffer, sizeof buffer, pipe) != NULL) {
            result += buffer;
        }
    } catch (...) {
        env->ReleaseStringUTFChars(command, cmd);
        return env->NewStringUTF("Execution error!");
    }
    pclose(pipe);
    env->ReleaseStringUTFChars(command, cmd);
    return env->NewStringUTF(result.c_str());
}