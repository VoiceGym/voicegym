package de.voicegym.voicegym

import de.voicegym.voicegym.audioHelper.WavFile
import junit.framework.Assert.assertEquals
import junit.framework.Assert.assertFalse
import junit.framework.Assert.assertNotNull
import junit.framework.Assert.assertNull
import junit.framework.Assert.assertTrue
import org.junit.Test
import java.io.File


class TestFourierHelper {
    @Test
    //@DisplayName("IsPowerOf2 function")
    fun testIsPowerOfTwo() {
        val powersOfTwo = mutableSetOf<Int>()
        var i = 1
        assertTrue(FourierHelper.isPowerOf2(1))
        // positive test all Integers power of 2 above 0
        while (i > 0) {
            i *= 2
            powersOfTwo += i
            if (i > 0) assertTrue(FourierHelper.isPowerOf2(i))
        }
        // negative test a relevant range
        for (i in 2 until 100000) {
            if (i !in powersOfTwo) {
                assertFalse(FourierHelper.isPowerOf2(i))
            }
        }

    }

    @Test
    //@DisplayName("Binning function, tiny positive test")
    fun testBinning() {
        // short positive test
        val input = DoubleArray(10)
        input.fill(1.0, 0, 10)
        val output = DoubleArray(10)
        output.fill(0.0, 0, 10)
        FourierHelper.binInputToOutputArray(input, 2, output)

        for (i in 0 until 5) {
            assertEquals(1.0, output[i], 0.00000001)
        }

        for (i in 5 until 10) assertEquals(0.0, output[i], 0.00000001)


    }

    @Test
    //@DisplayName("FourierHelper")
    fun testFourierHelperInstanciation() {
        var exception: RuntimeException? = null;
        try {
            FourierHelper(1024, 2, 1024, 44100)
        } catch (e: RuntimeException) {
            exception = e;
        }
        assertNotNull(exception);
        exception = null;
        try {
            FourierHelper(512, 2, 1024, 44100)
        } catch (e: RuntimeException) {
            exception = e;
        }
        assertNull(exception);
        try {
            FourierHelper(500, 2, 1000, 44100)
        } catch (e: RuntimeException) {
            exception = e;
        }
        assertNotNull(exception)
    }

    @Test
    fun testFourierHelper() {
        val collectedSamples = 8192
        var helper = FourierHelper(4096, 2, collectedSamples, 44100)

        assertEquals(5.383, helper.deltaFrequency(), 0.001)

        // get the test samples
        val a0 = WavFile(File("src/test/resources/purewaves/027,5Hz-A0.wav"))
        val a1 = WavFile(File("src/test/resources/purewaves/055Hz-A1.wav"))
        val a2 = WavFile(File("src/test/resources/purewaves/110Hz-A2.wav"))
        val a3 = WavFile(File("src/test/resources/purewaves/220Hz-A3.wav"))
        val a4 = WavFile(File("src/test/resources/purewaves/440Hz-A4.wav"))
        val a5 = WavFile(File("src/test/resources/purewaves/880Hz-A5.wav"))

        var pcmSamples: ShortArray = WavFile(File("src/test/resources/purewaves/440Hz-A4.wav")).getPCMBlock(collectedSamples)
        assertEquals(collectedSamples, pcmSamples.size)

        helper.fft(PCMUtil.getDoubleArrayFromShortArray(1.0, a0.getPCMBlock(collectedSamples)))
        assertEquals(5, idxOfMax(helper.amplitudeArray()))
        assertEquals(27.5, helper.frequencyArray()[5], helper.deltaFrequency())

        helper.fft(PCMUtil.getDoubleArrayFromShortArray(1.0, a1.getPCMBlock(collectedSamples)))
        assertEquals(10, idxOfMax(helper.amplitudeArray()))
        assertEquals(55.0, helper.frequencyArray()[10], helper.deltaFrequency())

        helper.fft(PCMUtil.getDoubleArrayFromShortArray(1.0, a2.getPCMBlock(collectedSamples)))
        assertEquals(20, idxOfMax(helper.amplitudeArray()))
        assertEquals(110.0, helper.frequencyArray()[20], helper.deltaFrequency())

        helper.fft(PCMUtil.getDoubleArrayFromShortArray(1.0, a3.getPCMBlock(collectedSamples)))
        assertEquals(41, idxOfMax(helper.amplitudeArray()))
        assertEquals(220.0, helper.frequencyArray()[41], helper.deltaFrequency())

        helper.fft(PCMUtil.getDoubleArrayFromShortArray(1.0, a4.getPCMBlock(collectedSamples)))
        assertEquals(82, idxOfMax(helper.amplitudeArray()))
        assertEquals(440.0, helper.frequencyArray()[82], helper.deltaFrequency())

        helper.fft(PCMUtil.getDoubleArrayFromShortArray(1.0, a5.getPCMBlock(collectedSamples)))
        assertEquals(163, idxOfMax(helper.amplitudeArray()))
        assertEquals(880.0, helper.frequencyArray()[163], helper.deltaFrequency())


    }

    fun Double.format(digits: Int) = java.lang.String.format("%.${digits}f", this)

    fun idxOfMax(arr: DoubleArray): Int? {
        val max = arr.max()
        if (max != null && max != Double.NaN) return arr.indexOf(max) else return null
    }
}
