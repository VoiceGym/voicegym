package de.voicegym.voicegym.recordActivity.fragments

import java.util.Arrays

data class RawSpectrogramSettings(
        val originatingArray: DoubleArray,
        val copyIsFromIndex: Int,
        val copyIsToIndex: Int,
        val rangeFrequencyArray: DoubleArray
) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as RawSpectrogramSettings

        if (!Arrays.equals(originatingArray, other.originatingArray)) return false
        if (copyIsFromIndex != other.copyIsFromIndex) return false
        if (copyIsToIndex != other.copyIsToIndex) return false
        if (!Arrays.equals(rangeFrequencyArray, other.rangeFrequencyArray)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = Arrays.hashCode(originatingArray)
        result = 31 * result + copyIsFromIndex
        result = 31 * result + copyIsToIndex
        result = 31 * result + Arrays.hashCode(rangeFrequencyArray)
        return result
    }
}
