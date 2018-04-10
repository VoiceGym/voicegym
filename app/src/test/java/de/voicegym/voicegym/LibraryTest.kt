package de.voicegym.voicegym

//import com.google.common.base.Stopwatch
import de.voicegym.voicegym.SoundFiles.PCMHelper
import de.voicegym.voicegym.SoundFiles.WavFile
import junit.framework.Assert.assertEquals
import junit.framework.Assert.fail
import org.jtransforms.fft.DoubleFFT_1D
import org.junit.Test

import java.io.File
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.TimeUnit

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

    @Test
    fun testFireJtransformFourier() {
        val wavFile = WavFile(File("src/test/resources/frame1.wav"))

        val inputFrame = PCMHelper.getDoubleArrayFromShortArray(1.0, wavFile.getTimeFrame(25))

        val fftDo = DoubleFFT_1D(inputFrame.size.toLong())
        val fft = DoubleArray(2 * inputFrame.size)
        val zero = DoubleArray(inputFrame.size)
        val out = DoubleArray(inputFrame.size)

        //val stopwatch = Stopwatch.createUnstarted()
        //stopwatch.start()
        // ab hier 1000 ausführungen
        for (i in 0 until 1000) {
            System.arraycopy(inputFrame, 0, fft, 0, inputFrame.size)
            System.arraycopy(zero, 0, fft, inputFrame.size, inputFrame.size)
            fftDo.realForwardFull(fft)
            System.arraycopy(fft, 0, out, 0, inputFrame.size)
        }
        //stopwatch.stop()
        //println("${stopwatch.elapsed(TimeUnit.MILLISECONDS)} ms ")
        // bis hier

    }


    /**
     * A native method that is implemented by the 'native-lib' native library,
     * which is packaged with this application.
     */

}
