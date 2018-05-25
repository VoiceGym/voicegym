package de.voicegym.voicegym

//import com.google.common.base.Stopwatch

import de.voicegym.voicegym.audioHelper.WavFile
import de.voicegym.voicegym.fourierHelper.getDoubleArrayFromShortArray
import org.jtransforms.fft.DoubleFFT_1D
import org.junit.Test
import java.io.File
import java.io.FileInputStream

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
    fun testFireJtransformFourier() {
        val wavFile = WavFile(File("src/test/resources/frame1.wav"))

        val inputFrame = getDoubleArrayFromShortArray(1.0, wavFile.getTimeFrame(25))

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
