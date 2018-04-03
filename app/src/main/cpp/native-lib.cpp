#include <jni.h>
#include <string>
#include <fftw3.h>

extern "C" JNIEXPORT jstring

JNICALL
Java_de_voicegym_voicegym_MainActivity_stringFromJNI(
        JNIEnv *env,
        jobject /* this */) {

    int N=1024;
    fftw_complex *in, *out;
    fftw_plan p;
    in = (fftw_complex*) fftw_malloc(sizeof(fftw_complex) * N);
    out = (fftw_complex*) fftw_malloc(sizeof(fftw_complex) * N);
    p = fftw_plan_dft_1d(N, in, out, FFTW_FORWARD, FFTW_ESTIMATE);

    fftw_destroy_plan(p);
    fftw_free(in); fftw_free(out);

    std::string hello = "Hello from C++";
    return env->NewStringUTF(hello.c_str());
}
