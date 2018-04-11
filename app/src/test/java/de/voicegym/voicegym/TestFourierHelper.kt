package de.voicegym.voicegym

import de.voicegym.voicegym.FourierHelper.FourierHelper
import org.junit.Assert.*
import org.junit.Test

class TestFourierHelper {
    @Test
    fun testIsPowerOfTwo() {
        val set = HashSet<Int>()
        var i = 1
        assertTrue(FourierHelper.isPowerOf2(1))
        // positive test all Integers power of 2 above 0
        while (i > 0) {
            i *= 2
            set.add(i)
            if (i > 0) assertTrue(FourierHelper.isPowerOf2(i))
        }
        // negative test a relevant range
        for (i in 2 until 100000) {
            if (!set.contains(i)) {
                assertFalse(FourierHelper.isPowerOf2(i))
            }
        }

    }

    @Test
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
}
