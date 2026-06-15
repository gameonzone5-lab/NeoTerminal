#include <jni.h>
#include <string>
#include <unistd.h>
#include <termios.h>
#include <fcntl.h>
#include <poll.h>
#include <android/log.h>

#define LOG_TAG "NeoTerminal_Native"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)

int pty_master_fd = -1;
int pty_slave_fd = -1;

extern "C" JNIEXPORT jint JNICALL
Java_com_neoterminal_core_TerminalActivity_startPty(JNIEnv* env, jobject thiz) {
    int master_fd = posix_openpt(O_RDWR, 0);
    if (master_fd == -1) return -1;

    granttaccess(master_fd, 0);
    
    pid_t pid = fork();
    if (pid == 0) { // Child
        int slave_fd = ptsname_open(ptsname(master_fd, NULL), O_RDWR);
        if (slave_fd == -1) _exit(1);
        
        setsid();
        dup2(slave_fd, 0);
        dup2(slave_fd, 1);
        dup2(slave_fd, 2);
        
        execl("/system/bin/sh", "/system/bin/sh", NULL);
        _exit(1);
    }
    
    pty_master_fd = master_fd;
    return pty_master_fd;
}

extern "C" JNIEXPORT void JNICALL
Java_com_neoterminal_core_TerminalActivity_writeCommand(JNIEnv* env, jobject thiz, jstring cmd) {
    const char* str = env->GetStringUTFChars(cmd, NULL);
    if (pty_master_fd != -1) {
        write(pty_master_fd, str, strlen(str));
    }
    env->ReleaseStringUTFChars(cmd, str);
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_neoterminal_core_TerminalActivity_readOutput(JNIEnv* env, jobject thiz) {
    char buffer[1024];
    if (pty_master_fd == -1) return env->NewStringUTF("PTY not initialized");
    
    ssize_t n = read(pty_master_fd, buffer, sizeof(buffer) - 1);
    if (n <= 0) return env->NewStringUTF("");
    
    buffer[n] = '\0';
    return env->NewStringUTF(buffer);
}
