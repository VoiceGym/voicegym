package de.voicegym.voicegym.Activities

import android.content.pm.ActivityInfo
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import de.voicegym.voicegym.Activities.InstrumentViews.HotGradientColorPicker
import de.voicegym.voicegym.FourierHelper.FourierHelper
import de.voicegym.voicegym.FourierHelper.PCMUtil
import de.voicegym.voicegym.R
import de.voicegym.voicegym.SoundFiles.WavFile
import kotlinx.android.synthetic.main.activity_record.dummyView
import kotlinx.android.synthetic.main.activity_record.floatingActionButton
import org.apache.commons.math3.analysis.interpolation.LinearInterpolator


class RecordActivity : AppCompatActivity() {

    val fourierHelper = FourierHelper(4096, 4, 16384, 44100)
    val interpolator = LinearInterpolator()

    val fromFrequency: Double = 10.0
    val tillFrequency: Double = 1000.0
    val frequencyArray: DoubleArray = fourierHelper.frequencyArray()
    var shortArray: ShortArray? = null

    init {
        if (fromFrequency > tillFrequency) throw RuntimeException("fromFrequency must be smaller than tillFrequency")
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN

        setContentView(R.layout.activity_record)

        dummyView.xDataPoints = 50

        val wavFile = WavFile(resources.openRawResource(R.raw.twosounds))
        shortArray = wavFile.getPCMBlock(16384)


        floatingActionButton.setOnClickListener({
            dummyView.insertColorLine(getColorArray())
            dummyView.invalidate()
            floatingActionButton.invalidate()
        })
    }

    private fun getColorArray(): IntArray {
        if (shortArray != null) {
            fourierHelper.fft(PCMUtil.getDoubleArrayFromShortArray(1.0, shortArray!!))
            val spectrum = fourierHelper.amplitudeArray()
            val interpolatingFunction = interpolator.interpolate(frequencyArray, spectrum)
            val colors = IntArray(dummyView.getDrawAreaHeight().toInt())
            val deltaFrequency = (tillFrequency - fromFrequency) / colors.size
            for (i in 0 until colors.size) {
                colors[i] = HotGradientColorPicker.pickColor((interpolatingFunction.value(fromFrequency + i * deltaFrequency) / 200).toFloat())
            }
            return colors
        } else throw RuntimeException("Resource Not Available")
    }
}
