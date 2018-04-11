package de.voicegym.voicegym.FourierHelper

object PCMUtil {
    fun getDoubleArrayFromShortArray(normalization: Double, inputArray: ShortArray): DoubleArray =
            inputArray.toList().map {
                if (it >= 0)
                    normalization * (it / Short.MAX_VALUE)
                else
                    -normalization * (it / Short.MIN_VALUE)
            }.toDoubleArray()
}