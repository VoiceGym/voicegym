package de.voicegym.voicegym.recordActivity.fragments

import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import de.voicegym.voicegym.R
import de.voicegym.voicegym.recordActivity.fragments.InstrumentState.LIVE_DISPLAY
import de.voicegym.voicegym.recordActivity.fragments.InstrumentState.RECORDING_DATA
import de.voicegym.voicegym.recordActivity.views.SpectrogramView
import de.voicegym.voicegym.recordActivity.views.util.HotGradientColorPicker
import de.voicegym.voicegym.util.audio.getDezibelFromAmplitude
import org.apache.commons.math3.analysis.interpolation.LinearInterpolator


class SpectrogramFragment() : AbstractInstrumentFragment() {

    override var userSettings: UserSettings? = null // TODO make not nullable

    var frequencyArray: DoubleArray? = null
    var deltaFrequency: Double = 0.0
    var internalSpectrogramSettings: RawSpectrogramSettings? = null

    private val interpolator = LinearInterpolator()

    var spectrogramView: SpectrogramView? = null

    override fun updateUserSettings(userSettings: UserSettings) {
        this.userSettings = userSettings
        spectrogramView?.xDataPoints = userSettings.numberDataPoints
        spectrogramView?.samplesPerDataPoint = userSettings.samplesPerDatapoint
        if (frequencyArray != null) onRangeChanged()
    }

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
            it.xDataPoints = userSettings?.numberDataPoints ?: 10
            it.invalidate()
        }
        return view
    }

    override fun insertNewAmplitudes(spectrum: DoubleArray) {
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
                deltaFrequency = (userSettings!!.tillFrequency - userSettings!!.fromFrequency) / it.size
                for (i in 0 until it.size) {
                    val amplitudeVal = interpolatingFunction.value(userSettings!!.fromFrequency + i * deltaFrequency)
                    it[i] = HotGradientColorPicker.pickColor(getDezibelFromAmplitude(amplitudeVal) / normalizationConstant)
                }
            }
        }

        if (colors == null) {
            throw Error("Error calculating color array.")
        }
        return colors!!
    }

    private fun onRangeChanged() {
        userSettings?.let {
            if (it.fromFrequency > it.tillFrequency) throw RuntimeException("fromFrequency must be smaller than tillFrequency")
            if (frequencyArray!![0] > it.fromFrequency) throw RuntimeException("you choose a frequency below available range")
            if (frequencyArray!![frequencyArray!!.size - 1] < it.tillFrequency) throw RuntimeException("you choose a frequency above available range")

            // find range for which values are displayed
            var idx = 0
            while (frequencyArray!![idx] < it.fromFrequency) idx++
            val fromIndexF = idx - 1 // just keep one data point outside of range
            while (frequencyArray!![idx] < it.tillFrequency) idx++
            val tillIndexF = idx + 1 // just keep two data points outside of range
            val frequencyRangeArray = frequencyArray!!.copyOfRange(fromIndexF, tillIndexF)
            internalSpectrogramSettings = RawSpectrogramSettings(frequencyArray!!, fromIndexF, tillIndexF, frequencyRangeArray)
        }
    }

    override fun getCurrentSamplePosition(): Int = spectrogramView?.currentDequePosition ?: 0

    override fun seekToSamplePosition(samplePosition: Int) {
        spectrogramView?.seekTo(samplePosition)
        spectrogramView?.invalidate()
    }

    override fun resetFragment() {
        spectrogramView?.let {
            it.clearBitmapAndBuffer()
            it.spectrogramViewState = LIVE_DISPLAY
            it.invalidate()
        }
    }

    override fun startRecording() {
        spectrogramView?.spectrogramViewState = RECORDING_DATA
    }

    override fun doneRecordingSwitchToPlayback() {
        spectrogramView?.let {
            it.rewindDequesToStart()
            it.clearBitmapAndBuffer()
            it.forwardWindDequesToEnd()
            it.spectrogramViewState = InstrumentState.PLAYBACK
            it.invalidate()
        }
    }

    override fun getInstrumentState(): InstrumentState = spectrogramView?.spectrogramViewState
            ?: throw Error("Fragment not correctly initialized, no spectrogramview present")
}
