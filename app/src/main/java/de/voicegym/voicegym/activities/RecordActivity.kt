package de.voicegym.voicegym.activities

import android.content.pm.ActivityInfo
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import de.voicegym.voicegym.R
import de.voicegym.voicegym.fourierHelper.FourierHelper
import de.voicegym.voicegym.fourierHelper.PCMUtil
import de.voicegym.voicegym.soundFiles.WavFile
import de.voicegym.voicegym.views.util.HotGradientColorPicker
import kotlinx.android.synthetic.main.activity_record.dummyView
import kotlinx.android.synthetic.main.activity_record.floatingActionButton
import org.apache.commons.math3.analysis.interpolation.LinearInterpolator


class RecordActivity : AppCompatActivity() {

    val fourierHelper = FourierHelper(8192, 2, 16384, 44100)
    val interpolator = LinearInterpolator()

    val fromFrequency: Double = 10.0
    val tillFrequency: Double = 1000.0
    val frequencyArray: DoubleArray = fourierHelper.frequencyArray()
    var fromIndexF: Int = 0
    var tillIndexF: Int = 0
    var shortArray: ShortArray? = null
    var frequencyRangeArray: DoubleArray? = null


    init {
        onRangeChanged()
    }

    fun onRangeChanged() {
        if (fromFrequency > tillFrequency) throw RuntimeException("fromFrequency must be smaller than tillFrequency")
        if (frequencyArray[0] > fromFrequency) throw RuntimeException("you choose a frequency below available range")
        if (frequencyArray[frequencyArray.size - 1] < tillFrequency) throw RuntimeException("you choose a frequency above available range")

        // find range for which values are displayed
        var idx = 0
        while (frequencyArray[idx] < fromFrequency) idx++
        fromIndexF = idx - 1 // just keep one datapoint outside of range
        while (frequencyArray[idx] < tillFrequency) idx++
        tillIndexF = idx + 1 // just keep two datapoints outside of range
        frequencyRangeArray = frequencyArray.copyOfRange(fromIndexF, tillIndexF)
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN

        setContentView(R.layout.activity_record)

        dummyView.xDataPoints = 50

        val wavFile = WavFile(resources.openRawResource(R.raw.testtone))
        shortArray = wavFile.getPCMBlock(16384)


        floatingActionButton.setOnClickListener {
            dummyView.insertColorLine(getDisplayData())
            dummyView.invalidate()
            floatingActionButton.invalidate()
        }
    }

    private fun getDisplayData(): IntArray {
        if (shortArray != null) {
            fourierHelper.fft(PCMUtil.getDoubleArrayFromShortArray(1.0, shortArray!!))
            val spectrum = fourierHelper.amplitudeArray()
            return calculateColorArrayForSpectrum(spectrum, 65.0)
        } else throw RuntimeException("Resource Not Available")
    }

    private fun calculateColorArrayForSpectrum(amplitude: DoubleArray, normalizationConstant: Double): IntArray {
        val interpolatingFunction = interpolator.interpolate(frequencyRangeArray, amplitude.copyOfRange(fromIndexF, tillIndexF))
        val colors = IntArray(dummyView.getDrawAreaHeight().toInt())
        val deltaFrequency = (tillFrequency - fromFrequency) / colors.size
        for (i in 0 until colors.size) {
            val amplitudeVal = interpolatingFunction.value(fromFrequency + i * deltaFrequency)
            colors[i] = HotGradientColorPicker.pickColor(getDezibelFromAmplitude(amplitudeVal) / normalizationConstant)
        }
        return colors
    }

    private fun getDezibelFromAmplitude(amplitude: Double): Double {
        return (20 * Math.log10(amplitude))
    }


}
