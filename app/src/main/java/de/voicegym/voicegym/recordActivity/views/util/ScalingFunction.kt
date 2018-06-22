package de.voicegym.voicegym.recordActivity.views.util

import kotlin.math.roundToInt

data class PixelFrequencyPair(val pixelPosition: Int, val correspondingFrequency: Double)

abstract class ScalingFunction(val from: PixelFrequencyPair, val until: PixelFrequencyPair) {
    abstract fun valueFromPixel(pixelPosition: Int): Double
    abstract fun valueFromFrequency(frequency: Double): Int
}


class LinearScalingFunction(from: PixelFrequencyPair, until: PixelFrequencyPair) : ScalingFunction(from, until) {


    private val m: Double = (from.correspondingFrequency - until.correspondingFrequency) /
            (from.pixelPosition - until.pixelPosition)

    private val b: Double = until.correspondingFrequency - this.m * until.pixelPosition


    override fun valueFromPixel(pixelPosition: Int): Double = m * pixelPosition + b

    override fun valueFromFrequency(frequency: Double): Int = ((frequency - b) / m).roundToInt()

}
