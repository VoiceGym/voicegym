package de.voicegym.voicegym

import de.voicegym.voicegym.FourierHelper.FourierHelper
import de.voicegym.voicegym.FourierHelper.PCMUtil
import de.voicegym.voicegym.SoundFiles.WavFile
import junit.framework.Assert.*
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

        for (i in 5 until 10)
            assertEquals(0.0, output[i], 0.00000001)


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
        var helper = FourierHelper(4096, 2, 8192, 44100)

        var pcmSamples: ShortArray = WavFile(File("src/test/resources/purewaves/440Hz-A4.wav")).getPCMBlock(8192)
        assertEquals(8192, pcmSamples.size)
        var normalizedPcmSamples: DoubleArray = PCMUtil.getDoubleArrayFromShortArray(1.0, pcmSamples)
        helper.fft(normalizedPcmSamples)


        var fftArray = helper.amplitudeArray()
        for (i in 0 until 128) {
            print("${fftArray[i].format(3)}, ")
            if (i % 8 == 7) print("\n")
        }
    }

    fun Double.format(digits: Int) = java.lang.String.format("%.${digits}f", this)
}
