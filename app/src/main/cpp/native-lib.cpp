#include <jni.h>
#include <string>
#include <fftw3.h>
#include <iostream>

using namespace std;


class FourierExecutor {
public:
    FourierExecutor(int bufferSize, jdoubleArray outArr) {
        this->bufferSize = bufferSize;
        this->inputBuffer = (double *) fftw_malloc(sizeof(fftw_complex) * bufferSize);
        this->outputBuffer = (double *) fftw_malloc(sizeof(fftw_complex) * bufferSize * 2);
        this->plan = fftw_plan_r2r_1d(bufferSize, inputBuffer, outputBuffer,
                                      FFTW_R2HC, FFTW_MEASURE);
        this->output = outArr;
    }

    void execute(double *input, double *output) {
        copy(input, input + bufferSize, inputBuffer);
        fftw_execute(plan);
        copy(outputBuffer, outputBuffer + bufferSize * 2, output);
    }

    ~FourierExecutor() {
        fftw_destroy_plan(plan);
        fftw_free(inputBuffer);
        fftw_free(outputBuffer);
        std::cout << "Killing Executor";
    }

    int getBufferSize() {
        return bufferSize;
    }

    jdoubleArray getOutArray() {
        return this->output;
    }

private:
    int bufferSize;
    double *inputBuffer;
    double *outputBuffer;
    fftw_plan plan;
    jdoubleArray output;
};

/*----------------------------------------------------------------------------------------------
 *
 ----------------------------------------------------------------------------------------------*/
jfieldID getMemoryLocationOfExecutor(JNIEnv *env, jobject obj) {
    static jfieldID memoryAddress = 0;
    if (!memoryAddress) {
        jclass c = env->GetObjectClass(obj);
        memoryAddress = env->GetFieldID(c, "objPtr", "J");
        env->DeleteLocalRef(c);
    }
    return memoryAddress;
}

extern "C" JNIEXPORT jstring

JNICALL
Java_de_voicegym_voicegym_MainActivity_stringFromJNI(
        JNIEnv *env,
        jobject /* this */) {

    std::string hello = "Hello from C++";
    return env->NewStringUTF(hello.c_str());
}



/*----------------------------------------------------------------------------------------------
 *
 ----------------------------------------------------------------------------------------------*/
extern "C"
JNIEXPORT void JNICALL
Java_de_voicegym_voicegym_FourierExecutorWrapper_initializeExecutor(
        JNIEnv *env,
        jobject instance,
        jint bufferSize) {

    FourierExecutor *executor = new FourierExecutor(bufferSize,
                                                    env->NewDoubleArray(bufferSize * 2));

    env->SetLongField(instance,
                      getMemoryLocationOfExecutor(env, instance),
                      (jlong) executor);

}

/*----------------------------------------------------------------------------------------------
 *
 ----------------------------------------------------------------------------------------------*/

extern "C"
JNIEXPORT void JNICALL
Java_de_voicegym_voicegym_FourierExecutorWrapper_execute(JNIEnv *env, jobject instance,
                                                         jdoubleArray inputFrame,
                                                         jdoubleArray outBuffer) {
    FourierExecutor *executor = (FourierExecutor *) env->GetLongField(instance,
                                                                      getMemoryLocationOfExecutor(
                                                                              env, instance));

    //jsize len = env->GetArrayLength(inputFrame);
    double *input = env->GetDoubleArrayElements(inputFrame, JNI_FALSE);
    double *output = env->GetDoubleArrayElements(outBuffer, JNI_FALSE);
    executor->execute(input, output);
}

/*----------------------------------------------------------------------------------------------
 *
 ----------------------------------------------------------------------------------------------*/
extern "C"
JNIEXPORT jint JNICALL
Java_de_voicegym_voicegym_FourierExecutorWrapper_getBufferSizeOfCObject__(JNIEnv *env,
                                                                          jobject instance) {
    FourierExecutor *executor = (FourierExecutor *) env->GetLongField(instance,
                                                                      getMemoryLocationOfExecutor(
                                                                              env, instance));
    return executor->getBufferSize();
}

/*----------------------------------------------------------------------------------------------
 *
 ----------------------------------------------------------------------------------------------*/
extern "C"
JNIEXPORT void JNICALL
Java_de_voicegym_voicegym_FourierExecutorWrapper_destroyExecutor(JNIEnv *env, jobject instance) {
    FourierExecutor *executor = (FourierExecutor *) env->GetLongField(instance,
                                                                      getMemoryLocationOfExecutor(
                                                                              env, instance));
    delete executor;
}