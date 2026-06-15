#include <jni.h>
#include <string>

extern "C" JNIEXPORT jstring JNICALL
Java_com_neoterminal_core_TerminalActivity_stringFromJNI(
        JNIEnv* env,
        jobject /* this */) {
    std::string hello = "Native C++ Backend Initialized Successfully!";
    return env->NewStringUTF(hello.c_str());
}
