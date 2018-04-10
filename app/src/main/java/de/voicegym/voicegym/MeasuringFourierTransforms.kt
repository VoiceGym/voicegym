package de.voicegym.voicegym

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import de.voicegym.voicegym.SoundFiles.PCMHelper
import de.voicegym.voicegym.SoundFiles.WavFile

import kotlinx.android.synthetic.main.activity_measuring_fourier_transforms.*
import org.jtransforms.fft.DoubleFFT_1D


class MeasuringFourierTransforms : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_measuring_fourier_transforms)
        fab.setOnClickListener {
            statusText.setText("Measuring")


            val wavFile = WavFile(getResources().openRawResource(R.raw.frame1))
            val inputFrame = PCMHelper.getDoubleArrayFromShortArray(1.0, wavFile.getTimeFrame(25))

            jtransformResult.setText((getJTransformsExecutionTime(inputFrame)).toString() + " ms")

            statusText.setText("Done")
        }

    }

    fun getJTransformsExecutionTime(inputFrame: DoubleArray): Long {
        val fftDo = DoubleFFT_1D(inputFrame.size.toLong())
        val fft = DoubleArray(2 * inputFrame.size)
        val zero = DoubleArray(inputFrame.size)
        val out = DoubleArray(inputFrame.size)

        val n = 100
        val start = System.currentTimeMillis();
        for (i in 0 until n) {
            System.arraycopy(inputFrame, 0, fft, 0, inputFrame.size)
            System.arraycopy(zero, 0, fft, inputFrame.size, inputFrame.size)
            fftDo.realForwardFull(fft)
            System.arraycopy(fft, 0, out, 0, inputFrame.size)
        }
        val stop = System.currentTimeMillis()

        return ((stop - start))
    }
}
