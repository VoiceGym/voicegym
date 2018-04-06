package de.voicegym.voicegym

import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v7.app.AppCompatActivity
import com.google.common.base.Stopwatch
import de.voicegym.voicegym.SoundFiles.PCMHelper
import de.voicegym.voicegym.SoundFiles.WavFile

import kotlinx.android.synthetic.main.activity_measuring_fourier_transforms.*
import org.jtransforms.fft.DoubleFFT_1D
import java.io.File
import java.net.URI
import java.util.concurrent.TimeUnit


class MeasuringFourierTransforms : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_measuring_fourier_transforms)

        fab.setOnClickListener {
            statusText.setText("Measuring")
            /*
            val wavFile = WavFile(File(URI("android.resource://de.voicegym.voicegym/res/raw/frame1.wav")))

            val inputFrame = PCMHelper.getDoubleArrayFromShortArray(1.0, wavFile.getTimeFrame(25))

            val fftDo = DoubleFFT_1D(inputFrame.size.toLong())
            val fft = DoubleArray(2 * inputFrame.size)
            val zero = DoubleArray(inputFrame.size)
            val out = DoubleArray(inputFrame.size)

            val stopwatch = Stopwatch.createUnstarted()
            stopwatch.start()
            val n=100
            for (i in 0 until n) {
                System.arraycopy(inputFrame, 0, fft, 0, inputFrame.size)
                System.arraycopy(zero, 0, fft, inputFrame.size, inputFrame.size)
                fftDo.realForwardFull(fft)
                System.arraycopy(fft, 0, out, 0, inputFrame.size)
            }
            stopwatch.stop()
            jtransformResult.setText((stopwatch.elapsed(TimeUnit.MILLISECONDS).toDouble()/n).toString() + " ms")
            statusText.setText("Done")*/
        }

    }
}
