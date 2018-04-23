package de.voicegym.voicegym.FourierHelper

import java.util.Observable
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.concurrent.thread


class AsyncTransformer(val fourierHelper: FourierHelper) : Observable() {

    val lock = java.lang.Object()

    private val workerThread: Thread = thread(isDaemon = true, priority = 9, block = {

        while (true) {
            if (inputQueue.isNotEmpty()) {
                // do work
                val fftJob = inputQueue.poll()
                val raw = fourierHelper.fft(PCMUtil.getDoubleArrayFromShortArray(1.0, fftJob.pcmData))
                when (fftJob) {
                    is AmplitudeJob -> {
                        System.arraycopy(fourierHelper.amplitudeArray(), 0, fftJob.amplitude, 0, fourierHelper.blockSize)
                        addResultToResultQueue(fftJob.copy(processed = true))
                    }
                    is PhaseJob -> {
                        System.arraycopy(fourierHelper.phaseArray(), 0, fftJob.phase, 0, fourierHelper.blockSize)
                        addResultToResultQueue(fftJob.copy(processed = true))
                    }
                    is AmplitudeAndPhaseJob -> {
                        System.arraycopy(fourierHelper.amplitudeArray(), 0, fftJob.amplitude, 0, fourierHelper.blockSize)
                        System.arraycopy(fourierHelper.phaseArray(), 0, fftJob.phase, 0, fourierHelper.blockSize)
                        addResultToResultQueue(fftJob.copy(processed = true))
                    }
                    is ComplexJob -> {
                        System.arraycopy(raw, 0, fftJob.complexResult, 0, 2 * fourierHelper.blockSize)
                        addResultToResultQueue(fftJob.copy(processed = true))
                    }
                }
            } else {
                // go to sleep
                lock.wait()
            }
        }
    })

    private val inputQueue: ConcurrentLinkedQueue<FFTJob> = ConcurrentLinkedQueue()
    private val resultQueue: ConcurrentLinkedQueue<FFTJob> = ConcurrentLinkedQueue()

    fun addNewFFTJob(job: FFTJob) {
        inputQueue.add(job)
        lock.notifyAll()
    }

    private fun addResultToResultQueue(result: FFTJob) {
        resultQueue.add(result)
        this.setChanged()
        this.notifyObservers()
    }

    private fun getResult(): FFTJob? = if (resultQueue.isNotEmpty()) resultQueue.poll() else null

}
