package de.voicegym.voicegym.activities

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import de.voicegym.voicegym.R
import de.voicegym.voicegym.fourierHelper.FourierHelper
import de.voicegym.voicegym.fourierHelper.PCMUtil
import de.voicegym.voicegym.soundFiles.WavFile
import kotlinx.android.synthetic.main.activity_measuring_fourier_transforms.fab
import kotlinx.android.synthetic.main.activity_measuring_fourier_transforms.jtransformResult
import kotlinx.android.synthetic.main.activity_measuring_fourier_transforms.statusText
import org.jtransforms.fft.DoubleFFT_1D

class MeasuringFourierTransforms : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_measuring_fourier_transforms)
        fab.setOnClickListener {
            statusText.text = "Measuring"


            val wavFile = WavFile(getResources().openRawResource(R.raw.testtone))
            val blocksize = 16384
            val lengthOfBlock = wavFile.getFrameLength(blocksize)
            val binning = 16
            val inputFrame = PCMUtil.getDoubleArrayFromShortArray(1.0, wavFile.getPCMBlock(blocksize))

            jtransformResult.text = "100 executions took" + (getJTransformsExecutionTime(inputFrame, binning)).toString() + " ms"
            statusText.text = "Done, blocklength was {$lengthOfBlock} ms. With {$blocksize} samples, binning was {$binning}"
        }

    }

    fun getJTransformsExecutionTime(inputFrame: DoubleArray, binning: Int): Long {
        val blocksize = inputFrame.size
        val fftDo = DoubleFFT_1D((blocksize / binning).toLong())
        val fft = DoubleArray(2 * (blocksize / binning))
        val zero = DoubleArray((blocksize / binning))
        val inBin = DoubleArray(blocksize)
        val out = DoubleArray((blocksize / binning))

        val n = 100
        val start = System.currentTimeMillis()
        for (i in 0 until n) {
            if (binning == 1) {
                System.arraycopy(inputFrame, 0, fft, 0, blocksize)
                System.arraycopy(zero, 0, fft, blocksize, blocksize)
                fftDo.realForwardFull(fft)
                System.arraycopy(fft, 0, out, 0, blocksize)
            } else {
                FourierHelper.binInputToOutputArray(inputFrame, binning, inBin)
                System.arraycopy(inBin, 0, fft, 0, blocksize / binning)
                System.arraycopy(zero, 0, fft, blocksize / binning, blocksize / binning)
                fftDo.realForwardFull(fft)
                System.arraycopy(fft, 0, out, 0, blocksize / binning)
            }
        }
        val stop = System.currentTimeMillis()

        return stop - start
    }
}
