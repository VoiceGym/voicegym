package de.voicegym.voicegym.SoundFiles

object PCMHelper {
    fun getDoubleArrayFromShortArray(normalization: Double, inputArray: ShortArray): DoubleArray =
            inputArray.toList().map {
                if (it >= 0)
                    normalization * (it / Short.MAX_VALUE)
                else
                    -normalization * (it / Short.MIN_VALUE)
            }.toDoubleArray()

    /**
     * This function averages n_bins samples from the inputArray into the outputArray.
     *
     * ATTENTION: for performance reason array sizes are not checked. Take extra care of those.
     *
     * @param inputArray the data source
     * @param n_bins the number of samples to be averaged from the data source
     * @param outputArray the averaged data samples will be copied here. Size must be inputArray.size/n_bins
     */
    fun getBinnedDoubleArray(inputArray: DoubleArray, n_bins: Int, outputArray: DoubleArray) {
        var targetN = 0;
        for (sourceN in 0 until inputArray.size step n_bins) {
            for (i in sourceN until sourceN + n_bins) {
                outputArray[targetN] += inputArray[i]
            }
            outputArray[targetN] = outputArray[targetN] / n_bins
            targetN++
        }
    }
}