package de.voicegym.voicegym.fourierHelper

import de.voicegym.voicegym.fourierHelper.RequestedResultType.FFT_AMPLITUDE
import de.voicegym.voicegym.fourierHelper.RequestedResultType.FFT_AMPLITUDE_AND_PHASE
import de.voicegym.voicegym.fourierHelper.RequestedResultType.FFT_COMPLEX_RESULT
import de.voicegym.voicegym.fourierHelper.RequestedResultType.FFT_PHASE
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
                when (fftJob.requestedResult) {
                    FFT_AMPLITUDE           -> {
                        val amplitude = DoubleArray(fourierHelper.blockSize)
                        System.arraycopy(fourierHelper.amplitudeArray(), 0, amplitude, 0, fourierHelper.blockSize)
                        addResultToResultQueue(AmplitudeResult(amplitude))
                    }

                    FFT_PHASE               -> {
                        val phase = DoubleArray(fourierHelper.blockSize)
                        System.arraycopy(fourierHelper.phaseArray(), 0, phase, 0, fourierHelper.blockSize)
                        addResultToResultQueue(PhaseResult(phase))
                    }

                    FFT_AMPLITUDE_AND_PHASE -> {
                        val amplitude = DoubleArray(fourierHelper.blockSize)
                        val phase = DoubleArray(fourierHelper.blockSize)
                        System.arraycopy(fourierHelper.amplitudeArray(), 0, amplitude, 0, fourierHelper.blockSize)
                        System.arraycopy(fourierHelper.phaseArray(), 0, phase, 0, fourierHelper.blockSize)
                        addResultToResultQueue(AmplitudeAndPhaseResult(amplitude, phase))
                    }

                    FFT_COMPLEX_RESULT      -> {
                        val complexResult = DoubleArray(2 * fourierHelper.blockSize)
                        System.arraycopy(raw, 0, complexResult, 0, 2 * fourierHelper.blockSize)
                        addResultToResultQueue(ComplexResult(complexResult))
                    }
                }
            } else {
                // go to sleep
                lock.wait()
            }
        }
    })

    private val inputQueue: ConcurrentLinkedQueue<ResultTask> = ConcurrentLinkedQueue()
    private val resultQueue: ConcurrentLinkedQueue<RequestedResult> = ConcurrentLinkedQueue()

    fun addNewFFTJob(job: ResultTask) {
        inputQueue.add(job)
        lock.notifyAll()
    }

    private fun addResultToResultQueue(result: RequestedResult) {
        resultQueue.add(result)
        this.setChanged()
        this.notifyObservers()
    }

    private fun getResult(): RequestedResult? = if (resultQueue.isNotEmpty()) resultQueue.poll() else null

}
