package de.voicegym.voicegym.FourierHelper

object PCMUtil {
    fun getDoubleArrayFromShortArray(normalization: Double, inputArray: ShortArray): DoubleArray =
            inputArray.toList().map {
                if (it >= 0)
                    normalization * (it.toDouble() / Short.MAX_VALUE)
                else
                    -normalization * (it.toDouble() / Short.MIN_VALUE)
            }.toDoubleArray()
}