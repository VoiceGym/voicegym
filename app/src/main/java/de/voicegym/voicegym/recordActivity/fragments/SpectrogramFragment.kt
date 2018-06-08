package de.voicegym.voicegym.recordActivity.fragments

import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import de.voicegym.voicegym.R
import de.voicegym.voicegym.recordActivity.views.SpectrogramView
import de.voicegym.voicegym.recordActivity.views.util.HotGradientColorPicker
import de.voicegym.voicegym.util.audio.getDezibelFromAmplitude
import org.apache.commons.math3.analysis.interpolation.LinearInterpolator


class SpectrogramFragment() : Fragment() {

    val userSpectrogramSettings = UserSpectrogramSettings(10.0, 1000.0, 200)
    var frequencyArray: DoubleArray? = null
    var deltaFrequency: Double = 0.0

    var internalSpectrogramSettings: RawSpectrogramSettings? = null

    private val interpolator = LinearInterpolator()

    var spectrogramView: SpectrogramView? = null


    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        frequencyArray = arguments?.getDoubleArray("frequencyArray")
        if (frequencyArray == null) throw Error("Didn't pass required arguments to fragments, frequencyArray missing")

        onRangeChanged()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_spectrogram, container, false)

        spectrogramView = view.findViewById<SpectrogramView>(R.id.spectrogramView)
        spectrogramView?.let {
            it.xDataPoints = userSpectrogramSettings.numberDataPoints
            it.invalidate()
        }
        return view
    }

    fun insertNewAmplitudes(spectrum: DoubleArray) {
        spectrogramView?.let {
            val colors = calculateColorArrayForSpectrum(spectrum, normalizationConstant = 55.0)
            it.insertNewDataPoint(colors)
            it.invalidate()
        }
        this.view?.invalidate()
    }

    private fun calculateColorArrayForSpectrum(amplitude: DoubleArray, normalizationConstant: Double): IntArray {
        var colors: IntArray? = null
        if (spectrogramView == null) {
            throw Error("Spectrogram not correctly initialized")
        }

        internalSpectrogramSettings?.let {
            val interpolatingFunction = interpolator.interpolate(it.rangeFrequencyArray, amplitude.copyOfRange(it.copyIsFromIndex, it.copyIsToIndex))

            colors = IntArray(spectrogramView!!.getDrawAreaHeight().toInt())
            colors?.let {
                deltaFrequency = (userSpectrogramSettings.tillFrequency - userSpectrogramSettings.fromFrequency) / it.size
                for (i in 0 until it.size) {
                    val amplitudeVal = interpolatingFunction.value(userSpectrogramSettings.fromFrequency + i * deltaFrequency)
                    it[i] = HotGradientColorPicker.pickColor(getDezibelFromAmplitude(amplitudeVal) / normalizationConstant)
                }
            }
        }

        if (colors == null) {
            throw Error("Error calculating color array.")
        }
        return colors!!
    }

    fun onRangeChanged() {
        if (userSpectrogramSettings.fromFrequency > userSpectrogramSettings.tillFrequency) throw RuntimeException("fromFrequency must be smaller than tillFrequency")
        if (frequencyArray!![0] > userSpectrogramSettings.fromFrequency) throw RuntimeException("you choose a frequency below available range")
        if (frequencyArray!![frequencyArray!!.size - 1] < userSpectrogramSettings.tillFrequency) throw RuntimeException("you choose a frequency above available range")

        // find range for which values are displayed
        var idx = 0
        while (frequencyArray!![idx] < userSpectrogramSettings.fromFrequency) idx++
        val fromIndexF = idx - 1 // just keep one data point outside of range
        while (frequencyArray!![idx] < userSpectrogramSettings.tillFrequency) idx++
        val tillIndexF = idx + 1 // just keep two data points outside of range
        val frequencyRangeArray = frequencyArray!!.copyOfRange(fromIndexF, tillIndexF)
        internalSpectrogramSettings = RawSpectrogramSettings(frequencyArray!!, fromIndexF, tillIndexF, frequencyRangeArray)
    }

}
