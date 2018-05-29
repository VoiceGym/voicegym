package de.voicegym.voicegym.util.math

import org.jtransforms.fft.DoubleFFT_1D

/**
 * This needs a doc comment!
 */
class FourierHelper(
        // binning refers to the number of samples averaged into one block
        val blockSize: Int,
        // blockSize is value of the block used together with the FFT
        private val binning: Int, collectedSamples: Int, private val sampleRate: Int
) {

    private val fftTransformer = DoubleFFT_1D((blockSize).toLong())
    private val fftData = DoubleArray(2 * (blockSize))
    private val calculationBuffer = DoubleArray(blockSize)

    init {
        //he blockSize * binning must be equal to the number of collected samples
        require(!(collectedSamples / binning != blockSize) && !(collectedSamples % binning != 0)) {
            "Make sure your number of collected samples fit into the fourier transform block"
        }
        require(isPowerOf2(blockSize)) {
            "Blocksize wasn't chosen to be a power of two. FFT needs a blockSize the power of two."
        }
    }

    /**
     *
     * @param inputFrame
     * @return the DoubleArray containing the result of the transformation
     */
    fun fft(inputFrame: DoubleArray): DoubleArray {
        // delete last run
        fftData.fill(0.0, 0, fftData.size - 1)

        if (binning == 1) {
            // use the unbinned input // inputFrame needs to fit into fftData
            System.arraycopy(inputFrame, 0, fftData, 0, blockSize)
        } else {
            // bin the samples from inputFrame into the fftData Array
            binInputToOutputArray(inputFrame, binning, fftData)
        }

        // perform the transformation
        fftTransformer.realForwardFull(fftData)
        return fftData
    }

    /**
     * Calculates the Amplitudes for the complex values in the current FFT Buffer
     *
     * @return a DoubleArray filled with amplitudes / magnitude
     */
    fun amplitudeArray(): DoubleArray {
        for (i in 0 until fftData.size / 2) {
            calculationBuffer[i] = Math.sqrt(Math.pow(fftData[2 * i], 2.0) + Math.pow(fftData[2 * i + 1], 2.0))
        }
        return calculationBuffer
    }

    /**
     * Calculates the Phases for the complex values in the current FFT Buffer
     *
     * @return a DoubleArray filled with phases
     */
    fun phaseArray(): DoubleArray {
        for (i in 0 until fftData.size / 2) {
            calculationBuffer[i] = Math.atan(fftData[2 * i] / fftData[2 * i + 1])
        }
        return calculationBuffer
    }

    /**
     * The frequencies corresponding to the array index positions in the amplitude and phase arrays.
     *
     * @return The corresponding frequencies (Hz) of the indeces.
     */
    fun frequencyArray() = DoubleArray(blockSize, { it * sampleRate.toDouble() / (blockSize * binning) })

    /**
     * @return The frequency spacing as a Double between array cells in Hz.
     */
    fun deltaFrequency(): Double = sampleRate.toDouble() / (blockSize * binning)

    /**
     * @see <a href="https://en.wikipedia.org/wiki/Nyquist_frequency">The Nyquist Frequency</a><br/>
     * <br/>
     * In practical terms the maximum sensible frequency we can use to display our results, for the given blocksize and binning settings.
     * @return the Nyquist Frequency as a Double (Hz)
     */
    fun nyquistFrequency(): Double {
        return sampleRate.toDouble() / (2 * binning)
    }

    companion object {

        /**
         * Checks if a given number is a power of 2
         */
        fun isPowerOf2(number: Int): Boolean {
            var n = if (number > 0) number else -number
            var count = 0
            while (n > 0) {
                if (n and 1 == 1) {
                    count++
                }
                n = n.shr(1)
            }
            return (count == 1)
        }

        /**
         * This function averages n_bins samples from the inputArray into the outputArray.
         *
         * ATTENTION: for performance reason array sizes are not checked. Take extra care of those.
         *
         * @param inputArray the data source
         * @param n_bins the number of samples to be averaged from the data source
         * @param outputArray the averaged data samples will be copied here. Size must be inputArray.size/n_bins
         */
        fun binInputToOutputArray(inputArray: DoubleArray, n_bins: Int, outputArray: DoubleArray) {
            var targetN = 0
            for (sourceN in 0 until inputArray.size step n_bins) {
                for (i in sourceN until sourceN + n_bins) {
                    outputArray[targetN] += inputArray[i]
                }
                outputArray[targetN] = outputArray[targetN] / n_bins
                targetN++
            }
        }
    }
}
