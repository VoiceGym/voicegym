package de.voicegym.voicegym

import de.voicegym.voicegym.SoundFiles.WavFile
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
    fun testLoad() {
        val file: File = File("src/test/resources/frame2.wav")
        val wav = WavFile(file)
        println(wav.toString())
        for (i in 0 until wav.getNumberOfPCMSamples()) {
            print(wav.getNextPCMSample().toString() + "\t")
            if ((i + 1) % 10 == 0) print("\n")
        }
    }


    /**
     * A native method that is implemented by the 'native-lib' native library,
     * which is packaged with this application.
     */

}
