#include <jni.h>
#include <string>
#include <stdexcept>
#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>

extern "C" JNIEXPORT jstring JNICALL
Java_com_neoterminal_core_TerminalActivity_executeCommand(JNIEnv* env, jobject /* this */, jstring command) {
    const char *cmd_str = env->GetStringUTFChars(command, nullptr);
    std::string home_dir = "/data/data/com.neoterminal.core/files";
    setenv("HOME", home_dir.c_str(), 1);
    setenv("PATH", "/sbin:/system/sbin:/system/bin:/system/xbin:/vendor/bin:/vendor/xbin", 1);
    chdir(home_dir.c_str());
    std::string input_cmd(cmd_str);

    if (input_cmd.rfind("apt", 0) == 0 || input_cmd.rfind("pkg", 0) == 0) {
        env->ReleaseStringUTFChars(command, cmd_str);
        return env->NewStringUTF("[-] apt/pkg requires PRoot. Try native commands: ls, ping, uname, top.\n");
    }

    // AUTO-TOYBOX WRAPPER: If command is common, prefix it with toybox automatically
    std::string final_cmd = input_cmd;
    if (input_cmd.rfind("ping", 0) == 0 || input_cmd.rfind("ls", 0) == 0 ||
        input_cmd.rfind("uname", 0) == 0 || input_cmd.rfind("top", 0) == 0 ||
        input_cmd.rfind("cat", 0) == 0 || input_cmd.rfind("grep", 0) == 0) {
        final_cmd = "toybox " + input_cmd;
    }

    std::string full_cmd = final_cmd + " 2>&1";
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
    }

    if (result.empty()) result = "\n";
    pclose(pipe);
    env->ReleaseStringUTFChars(command, cmd_str);
    return env->NewStringUTF(result.c_str());
}