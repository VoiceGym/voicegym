package de.voicegym.voicegym

import de.voicegym.voicegym.recordActivity.views.util.ExponentialScalingFunction
import de.voicegym.voicegym.recordActivity.views.util.LinearScalingFunction
import de.voicegym.voicegym.recordActivity.views.util.PixelFrequencyPair
import junit.framework.Assert.assertEquals
import org.junit.Test


class ScalingFunctionTest {

    @Test
    fun testLinearScalingFunction() {
        val from = PixelFrequencyPair(100, 10.0)
        val till = PixelFrequencyPair(200, 100.0)

        val testObject = LinearScalingFunction(from, till)

        assertEquals(10.0, testObject.valueFromPixel(100))
        assertEquals(32.5, testObject.valueFromPixel(125))
        assertEquals(55.0, testObject.valueFromPixel(150))
        assertEquals(77.5, testObject.valueFromPixel(175))
        assertEquals(100.0, testObject.valueFromPixel(200))

        assertEquals(100, testObject.valueFromFrequency(10.0))
        assertEquals(125, testObject.valueFromFrequency(32.5))
        assertEquals(150, testObject.valueFromFrequency(55.0))
        assertEquals(175, testObject.valueFromFrequency(77.5))
        assertEquals(200, testObject.valueFromFrequency(100.0))

    }


    @Test
    fun testInverseLinearScalingFunction() {
        val from = PixelFrequencyPair(200, 10.0)
        val till = PixelFrequencyPair(100, 100.0)

        val testObject = LinearScalingFunction(from, till)

        assertEquals(10.0, testObject.valueFromPixel(200))
        assertEquals(32.5, testObject.valueFromPixel(175))
        assertEquals(55.0, testObject.valueFromPixel(150))
        assertEquals(77.5, testObject.valueFromPixel(125))
        assertEquals(100.0, testObject.valueFromPixel(100))

        assertEquals(200, testObject.valueFromFrequency(10.0))
        assertEquals(175, testObject.valueFromFrequency(32.5))
        assertEquals(150, testObject.valueFromFrequency(55.0))
        assertEquals(125, testObject.valueFromFrequency(77.5))
        assertEquals(100, testObject.valueFromFrequency(100.0))

    }


    @Test
    fun testExponentialScalingFunction() {
        val from = PixelFrequencyPair(100, 10.0)
        val till = PixelFrequencyPair(900, 2000.0)

        val testObject = ExponentialScalingFunction(from, till)

        assertEquals(10.0, testObject.valueFromPixel(100), 0.00000001)
        assertEquals(2000.0, testObject.valueFromPixel(900), 0.00000001)

        assertEquals(100, testObject.valueFromFrequency(10.0))
        assertEquals(900, testObject.valueFromFrequency(2000.0))
    }


    @Test
    fun testInverseExponentialScalingFunction() {
        val from = PixelFrequencyPair(900, 10.0)
        val till = PixelFrequencyPair(100, 2000.0)

        val testObject = ExponentialScalingFunction(from, till)

        assertEquals(2000.0, testObject.valueFromPixel(100), 0.00000001)
        assertEquals(10.0, testObject.valueFromPixel(900), 0.00000001)

        assertEquals(900, testObject.valueFromFrequency(10.0))
        assertEquals(100, testObject.valueFromFrequency(2000.0))
    }


}
