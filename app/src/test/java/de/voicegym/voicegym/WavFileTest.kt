package de.voicegym.voicegym

import de.voicegym.voicegym.util.audio.WavFile
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.core.Is
import org.junit.Test
import java.io.File

class WavFileTest {

    @Test
    fun testLoadWavFile() {
        val file = File("src/test/resources/frame1.wav")

        // 1 ms of data
        val expected = arrayOf(
                10465, 10210,  8824,  8648,
                 7875,  6984,  7424,  7095,
                 6985,  7293,  6472,  5845,
                 5337,  4336,  3898,  3571,
                 2953,  2143,  1343,   164,
                -1122, -1646, -2091, -2374,
                -1920, -2124, -2494, -2715,
                -3811, -4253, -4284, -4270,
                -3350, -2124,  -887,   679,
                 1997,  3171,  4476,  5262,
                 5893,  6686,  6882,  7169)

        WavFile(file).getTimeFrame(1).forEachIndexed { i, pcm ->
            assertThat(pcm, Is(equalTo(expected[i].toShort())))
        }

    }
}
