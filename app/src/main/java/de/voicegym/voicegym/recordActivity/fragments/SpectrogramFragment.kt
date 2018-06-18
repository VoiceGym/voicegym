package de.voicegym.voicegym.recordActivity.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import de.voicegym.voicegym.R
import de.voicegym.voicegym.menu.settings.FourierInstrumentViewSettings
import de.voicegym.voicegym.recordActivity.fragments.InstrumentState.LIVE_DISPLAY
import de.voicegym.voicegym.recordActivity.fragments.InstrumentState.RECORDING_DATA
import de.voicegym.voicegym.recordActivity.views.SpectrogramView
import de.voicegym.voicegym.recordActivity.views.util.HotGradientColorPicker
import de.voicegym.voicegym.util.audio.getDezibelFromAmplitude
import org.apache.commons.math3.analysis.interpolation.LinearInterpolator


class SpectrogramFragment : AbstractInstrumentFragment() {

    override var settings = FourierInstrumentViewSettings(4096, 2, 10.0, 1000.0, 100, false)

    var deltaFrequency = 0.0
    lateinit var spectrogramView: SpectrogramView
    private val interpolator = LinearInterpolator()
    private var frequencyArray: DoubleArray? = null
    private var internalSpectrogramSettings: RawSpectrogramSettings? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_spectrogram, container, false)

        spectrogramView = view.findViewById(R.id.spectrogramView)
        spectrogramView.let {
            it.xDataPoints = settings.displayedDatapoints
            it.invalidate()
        }
        return view
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        if (frequencyArray == null) throw Error("Didn't pass required arguments to fragments, frequencyArray missing")

        // all settings should have been after execution of activity onCreate
        onRangeChanged()
    }

    override fun updateFrequencyArray(frequencies: DoubleArray) {
        frequencyArray = frequencies
    }

    override fun updateInstrumentViewSettings(settings: FourierInstrumentViewSettings) {
        this.settings = settings
        spectrogramView.xDataPoints = settings.displayedDatapoints
        spectrogramView.samplesPerDataPoint = settings.samplesPerDatapoint
        if (frequencyArray != null) onRangeChanged()
    }

    override fun insertNewAmplitudes(spectrum: DoubleArray) {
        spectrogramView.let {
            val colors = calculateColorArrayForSpectrum(spectrum, NORMALIZATION_CONSTANT)
            it.insertNewDataPoint(colors)
            it.invalidate()
        }
        this.view?.invalidate()
    }

    private fun calculateColorArrayForSpectrum(amplitude: DoubleArray, normalizationConstant: Double): IntArray {
        var colors: IntArray? = null

        internalSpectrogramSettings?.let {
            val interpolatingFunction = interpolator.interpolate(it.rangeFrequencyArray, amplitude.copyOfRange(it.copyIsFromIndex, it.copyIsToIndex))

            colors = IntArray(spectrogramView.getDrawAreaHeight().toInt())
            colors?.let {
                deltaFrequency = (settings.tillFrequency - settings.fromFrequency) / it.size
                for (i in 0 until it.size) {
                    val amplitudeVal = interpolatingFunction.value(settings.fromFrequency + i * deltaFrequency)
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
        settings.let {
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

    override fun getCurrentSamplePosition(): Int =
            spectrogramView.currentDequePosition * settings.samplesPerDatapoint

    override fun seekToSamplePosition(samplePosition: Int) {
        spectrogramView.seekTo(samplePosition)
        spectrogramView.invalidate()
    }

    override fun resetFragment() {
        spectrogramView.let {
            it.clearBitmapAndBuffer()
            it.spectrogramViewState = LIVE_DISPLAY
            it.invalidate()
        }
    }

    override fun startRecording() {
        spectrogramView.spectrogramViewState = RECORDING_DATA
    }

    override fun doneRecordingSwitchToPlayback() {
        spectrogramView.let {
            it.rewindDequesToStart()
            it.clearBitmapAndBuffer()
            it.forwardWindDequesToEnd()
            it.spectrogramViewState = InstrumentState.PLAYBACK
            it.invalidate()
        }
    }

    override fun getInstrumentState(): InstrumentState =
            spectrogramView.spectrogramViewState

    companion object {
        const val NORMALIZATION_CONSTANT = 55.0
    }
}
