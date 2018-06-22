package de.voicegym.voicegym.recordActivity.views.util

import kotlin.math.roundToInt

data class PixelFrequencyPair(val pixelPosition: Int, val correspondingFrequency: Double)

abstract class ScalingFunction(val from: PixelFrequencyPair, val until: PixelFrequencyPair) {
    abstract fun valueFromPixel(pixelPosition: Int): Double
    abstract fun valueFromFrequency(frequency: Double): Int
}

/**
 * The used function is y = mx+b, where x are pixels and y are frequencies
 */
class LinearScalingFunction(from: PixelFrequencyPair, until: PixelFrequencyPair) : ScalingFunction(from, until) {

    private val m: Double = (from.correspondingFrequency - until.correspondingFrequency) /
            (from.pixelPosition - until.pixelPosition)
    private val b: Double = until.correspondingFrequency - this.m * until.pixelPosition

    override fun valueFromPixel(pixelPosition: Int): Double = m * pixelPosition + b
    override fun valueFromFrequency(frequency: Double): Int = ((frequency - b) / m).roundToInt()
}

/**
 * The used function here is y=C*a**x where x are pixels and y are frequencies
 */
class ExponentialScalingFunction(from: PixelFrequencyPair, until: PixelFrequencyPair) : ScalingFunction(from, until) {


    private val a: Double
    private val C: Double

    init {
        val (p1, f1) = from
        val (p2, f2) = until
        a = Math.exp(Math.log(f2 / f1) / (p2 - p1))
        C = f1 * Math.pow(a, -p1.toDouble())
    }

    override fun valueFromPixel(pixelPosition: Int): Double = C * Math.pow(a, pixelPosition.toDouble())

    override fun valueFromFrequency(frequency: Double): Int = (Math.log(frequency / C) / Math.log(a)).roundToInt()

}
