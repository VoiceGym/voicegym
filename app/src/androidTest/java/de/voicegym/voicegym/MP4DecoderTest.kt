package de.voicegym.voicegym

import android.support.test.InstrumentationRegistry
import android.support.test.runner.AndroidJUnit4
import de.voicegym.voicegym.util.audio.MP4Decoder
import de.voicegym.voicegym.util.audio.PCMStorage
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.FileOutputStream

@RunWith(AndroidJUnit4::class)
class MP4DecoderTest {

    @Test
    fun useAppContext() {
        // Context of the app under test.
        val appContext = InstrumentationRegistry.getTargetContext()
        Assert.assertEquals("de.voicegym.voicegym", appContext.packageName)
    }

    @Test
    fun useGetPCMStorage() {
        val appContext = InstrumentationRegistry.getTargetContext()
        val context = InstrumentationRegistry.getInstrumentation().context


        // copy file to device folder
        val inStream = context.assets.open("testfile1.m4a")
        //        val inputFile = "/input.pcm"
        val data=ByteArray(inStream.available())
        inStream.read(data)

        val outFile = File(appContext.filesDir, "testfile1.m4a")
        val out = FileOutputStream(outFile)
        out.write(data)
        out.close()
        val decoder=MP4Decoder(4096)
        val pcmStorage=PCMStorage(44100)
        // start decoding without error
        decoder.addBufferListener(pcmStorage)
        decoder.startDecoding(outFile)
    }
}
