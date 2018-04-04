package de.voicegym.voicegym;

/**
 * Created by KayleeTheMech on 04.04.2018.
 */

public class FourierExecutorWrapper {
    private boolean destroyed = false;

    private long objPtr = 0;

    public FourierExecutorWrapper(int bufferSize) {
        initializeExecutor(bufferSize);
        destroyed = false;
    }

    public void fourierTransform(){
        // TODO get BUFFER into native-lib
        // TODO execute
        // TODO get RESULT from native-lib
    }

    public void destroy() {
        destroyExecutor();
        this.objPtr = 0;
        destroyed = true;
    }

    // FourierExecutor.cpp methods
    public native void initializeExecutor(int bufferSize);

    public native void execute();

    public native void destroyExecutor();

    static {
        System.loadLibrary("native-lib");
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        if (!destroyed) {
            System.out.println("FourierExecutor wasn't closed properly");
            this.destroy();
        }
    }

}


