package de.voicegym.voicegym

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


}
