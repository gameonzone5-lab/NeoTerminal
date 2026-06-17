#include <jni.h>
#include <string>
#include <stdexcept>
#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>

extern "C" JNIEXPORT jstring JNICALL
Java_com_neoterminal_core_TerminalActivity_executeCommand(JNIEnv* env, jobject /* this */, jstring command) {
    const char *cmd_str = env->GetStringUTFChars(command, nullptr);

    // 1. Setup Perfect Android Environment
    std::string home_dir = "/data/data/com.neoterminal.core/files";
    setenv("HOME", home_dir.c_str(), 1);
    setenv("PATH", "/sbin:/system/sbin:/system/bin:/system/xbin:/vendor/bin:/vendor/xbin", 1);
    chdir(home_dir.c_str());

    // 2. Intercept 'apt' commands to give a clear explanation
    std::string input_cmd(cmd_str);
    if (input_cmd.rfind("apt", 0) == 0 || input_cmd.rfind("pkg", 0) == 0) {
        env->ReleaseStringUTFChars(command, cmd_str);
        return env->NewStringUTF("NeoTerminal uses the Native Android Shell.\nPackage managers like 'apt' or 'pkg' require a PRoot environment and are not supported here.\nTry native commands like: ls, pwd, top, logcat, ping.\n");
    }

    // 3. Execute Command safely
    std::string full_cmd = input_cmd + " 2>&1";
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