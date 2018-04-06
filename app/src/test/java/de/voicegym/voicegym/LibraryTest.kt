package de.voicegym.voicegym

import de.voicegym.voicegym.SoundFiles.WavFile
import junit.framework.Assert.assertEquals
import junit.framework.Assert.fail
import org.junit.Test

import java.io.File
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class LibraryTest {
    fun getByteArray(file: File): ByteArray {
        val stream: FileInputStream = FileInputStream(file)
        val data: ByteArray = ByteArray(file.length().toInt())
        stream.read(data)
        return data
    }


    @Test
    fun testLoadWavFile() {
        val file: File = File("src/test/resources/frame1.wav")

        // 1 ms of data
        val expected = arrayOf(10465, 10210, 8824, 8648, 7875, 6984, 7424, 7095, 6985, 7293, 6472, 5845, 5337, 4336, 3898, 3571, 2953, 2143, 1343, 164, -1122, -1646, -2091, -2374, -1920, -2124, -2494, -2715, -3811, -4253, -4284, -4270, -3350, -2124, -887, 679, 1997, 3171, 4476, 5262, 5893, 6686, 6882, 7169)

        WavFile(file).getTimeFrame(1).forEachIndexed { i, pcm ->
            assertEquals(expected.get(i).toShort(), pcm)
        }

    }


    /**
     * A native method that is implemented by the 'native-lib' native library,
     * which is packaged with this application.
     */

}
