package de.voicegym.voicegym.FourierHelper

import org.jtransforms.fft.DoubleFFT_1D

class FourierHelper(blockSize: Int, binning: Int, collectedSamples: Int, sampleRate: Int) {
    // binning refers to the number of samples averaged into one block
    val binning = binning
    // blockSize is value of the block used together with the FFT
    val blockSize = blockSize
    // the sampleRate
    val sampleRate = sampleRate

    private val fftTransformer = DoubleFFT_1D((blockSize).toLong())
    val fftData = DoubleArray(2 * (blockSize))

    init {
        if ((collectedSamples / binning != blockSize) || (collectedSamples % binning != 0)) {
            /**
             * the blockSize * binning must be equal to the number of collected samples
             */
            throw RuntimeException("Make sure your number of collected samples fit into the fourier transform block")
        }

        if (!isPowerOf2(blockSize)) {
            throw RuntimeException("Blocksize wasn't chosen to be a power of two. FFT needs a blockSize the power of two.")
        }

    }

    /**
     *
     * @param inputFrame
     * @return the DoubleArray containing the result of the transformation
     */
    fun fft(inputFrame: DoubleArray): DoubleArray {
        if (binning == 1) {
            // use the unbinned input // inputFrame needs to fit into fftData
            System.arraycopy(inputFrame, 0, fftData, 0, blockSize)
        } else {
            // bin the samples from inputFrame into the fftData Array
            binInputToOutputArray(inputFrame, binning, fftData)
        }

        // delete imaginary parts from last transform
        fftData.fill(0.toDouble(), blockSize, 2 * blockSize - 1)
        // perform the transformation
        fftTransformer.realForwardFull(fftData)
        return fftData
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