#include <jni.h>
#include <string>
#include <unistd.h>
#include <termios.h>
#include <fcntl.h>
#include <sys/stat.h>
#include <android/log.h>
#include <vector>

#define LOG_TAG "NeoTerminal_Native"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)

static int pty_master_fd = -1;

extern "C" JNIEXPORT jint JNICALL
Java_com_neoterminal_core_TerminalActivity_startPty(JNIEnv* env, jobject thiz) {
    pty_master_fd = posix_openpt(O_RDWR | O_NOCTTY, 0);
    if (pty_master_fd == -1) return -1;

    grantaccess(pty_master_fd, 0);

    pid_t pid = fork();
    if (pid == 0) { // Child process
        int slave_fd = ptsname_open(ptsname(pty_master_fd, NULL), O_RDWR);
        if (slave_fd == -1) _exit(1);

        setsid();
        dup2(slave_fd, 0);
        dup2(slave_fd, 1);
        dup2(slave_fd, 2);
        close(slave_fd);

        execl("/system/bin/sh", "/system/bin/sh", (char *)NULL);
        _exit(1);
    }

    return pty_master_fd;
}

extern "C" JNIEXPORT void JNICALL
Java_com_neoterminal_core_TerminalActivity_writeCommand(JNIEnv* env, jobject thiz, jstring cmd) {
    if (cmd == NULL) return;
    const char* str = env->GetStringUTFChars(cmd, NULL);
    if (pty_master_fd != -1) {
        write(pty_master_fd, str, strlen(str));
    }
    env->ReleaseStringUTFChars(cmd, str);
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_neoterminal_core_TerminalActivity_readOutput(JNIEnv* env, jobject thiz) {
    if (pty_master_fd == -1) return env->NewStringUTF("PTY not started");
    
    char buffer[4096];
    ssize_t n = read(pty_master_fd, buffer, sizeof(buffer) - 1);
    if (n <= 0) return env->NewStringUTF("");
    
    buffer[n] = '\0';
    return env->NewStringUTF(buffer);
}
