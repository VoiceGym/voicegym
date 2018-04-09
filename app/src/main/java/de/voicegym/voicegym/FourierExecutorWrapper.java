package de.voicegym.voicegym;

import org.apache.commons.math3.util.DoubleArray;

/**
 * Created by KayleeTheMech on 04.04.2018.
 */

public class FourierExecutorWrapper {
    private boolean destroyed = false;

    private long objPtr = 0;

    private double[] outBuffer;

    public FourierExecutorWrapper(int bufferSize) {
        initializeExecutor(bufferSize);
        destroyed = false;
        outBuffer = new double[bufferSize * 2];
    }

    public double[] fourierTransform(double[] input) {
        execute(input, outBuffer);
        return outBuffer;
    }

    public void destroy() {
        destroyExecutor();
        this.objPtr = 0;
        destroyed = true;
    }

    // FourierExecutor.cpp methods
    public native void initializeExecutor(int bufferSize);

    public native void execute(double[] inputFrame, double[] outBuffer);

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


