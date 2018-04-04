#include <jni.h>
#include <string>
#include <fftw3.h>


class FourierExecutor {
public:
    FourierExecutor(int bufferSize) {
        this->bufferSize = bufferSize;
        this->inputBuffer = (fftw_complex *) fftw_malloc(sizeof(fftw_complex) * bufferSize);
        this->outputBuffer = (fftw_complex *) fftw_malloc(sizeof(fftw_complex) * bufferSize);
        this->plan = fftw_plan_dft_1d(bufferSize, inputBuffer, outputBuffer, FFTW_FORWARD,
                                      FFTW_ESTIMATE);
    }

    fftw_complex *execute(fftw_complex *input) {
        this->inputBuffer = input;
        // execute the plan
        fftw_execute(plan);
        return this->outputBuffer;
    }

    ~FourierExecutor() {
        fftw_destroy_plan(plan);
        fftw_free(inputBuffer);
        fftw_free(outputBuffer);
    }

    int getBufferSize() {
        return bufferSize;
    }

private:
    int bufferSize;
    fftw_complex *inputBuffer;
    fftw_complex *outputBuffer;
    fftw_plan plan;
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
Java_de_voicegym_voicegym_FourierExecutorWrapper_initializeExecutor(JNIEnv *env, jobject instance,
                                                                    jint bufferSize) {
    env->SetLongField(instance, getMemoryLocationOfExecutor(env, instance),
                      (jlong) new FourierExecutor(bufferSize));
}

/*----------------------------------------------------------------------------------------------
 *
 ----------------------------------------------------------------------------------------------*/
extern "C"
JNIEXPORT void JNICALL
Java_de_voicegym_voicegym_FourierExecutorWrapper_execute(JNIEnv *env, jobject instance) {
    FourierExecutor *executor = (FourierExecutor *) env->GetLongField(instance,
                                                                      getMemoryLocationOfExecutor(
                                                                              env, instance));
    //TODO PASS IN BUFFER with correct size
    //TODO check BUFFER FITS with EXECUTOR
    fftw_complex *input;
    executor->execute(input);
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
