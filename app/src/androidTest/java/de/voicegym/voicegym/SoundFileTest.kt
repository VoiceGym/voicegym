package de.voicegym.voicegym

import android.support.test.InstrumentationRegistry
import android.support.test.runner.AndroidJUnit4
import android.util.Log
import de.voicegym.voicegym.util.audio.MP4Decoder
import de.voicegym.voicegym.util.audio.PCMStorage
import de.voicegym.voicegym.util.audio.SoundFile
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

@RunWith(AndroidJUnit4::class)
class SoundFileTest {

    @Test
    fun createSoundFile() {
        val appContext = InstrumentationRegistry.getTargetContext()
        val context = InstrumentationRegistry.getInstrumentation().context

        // copy file to device folder
        val inStream = context.assets.open("testfile2.m4a")
        //        val inputFile = "/input.pcm"
        val data=ByteArray(inStream.available())
        inStream.read(data)

        val outFile = File(appContext.filesDir, "testfile2.m4a")
        val out = FileOutputStream(outFile)
        out.write(data)
        out.close()

        val soundFile = SoundFile.create(outFile.absolutePath, object : SoundFile.ProgressListener {
            override fun reportProgress(fractionComplete: Double): Boolean {
                Log.d("SoundFileTest", "progress: " +  fractionComplete)
                return true
            }
        })!!
        val samples = ShortArray(soundFile.numSamples)
        soundFile.samples!!.get(samples)
        Log.d("SoundFileTest", "samples: " +  samples.toList().toString())

    }
}
