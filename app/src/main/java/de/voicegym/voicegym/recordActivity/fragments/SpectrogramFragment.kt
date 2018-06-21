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
import org.apache.commons.math3.analysis.interpolation.LinearInterpolator


class SpectrogramFragment : AbstractInstrumentFragment() {

    /**
     * this stores the current settings, initialized with bogus settings
     * requires a call of updateInstrumentViewSettings(settings) after creation
     */
    override var settings = FourierInstrumentViewSettings(4096, 2, 10.0, 1000.0, 100, false)

    /**
     * holds our spectrogram
     */
    lateinit var spectrogramView: SpectrogramView

    /**
     * holds the array that stores the frequencies of the relating to the positions in the amplitudeArray
     */
    private var frequencyArray: DoubleArray? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_spectrogram, container, false)

        spectrogramView = view.findViewById(R.id.spectrogramView)
        spectrogramView.let {
            it.updateInstrumentViewSettings(settings)
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
        spectrogramView.updateInstrumentViewSettings(settings)
        if (frequencyArray != null) onRangeChanged()
    }

    /**
     * this is the callback that receives a new amplitudeArray
     */
    override fun insertNewAmplitudes(spectrum: DoubleArray) {
        spectrogramView.let {
            TODO()
            it.invalidate()
        }
        this.view?.invalidate()
    }


    private fun onRangeChanged() {
        settings.let {
            TODO()
        }
    }

    override fun getCurrentSamplePosition(): Int {
        TODO()
    }

    override fun seekToSamplePosition(samplePosition: Int) {
        TODO()
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
            it.spectrogramViewState = InstrumentState.PLAYBACK
            TODO()
            it.invalidate()
        }
    }

    override fun cutToMaximumSampleNumber(samples: Int) {
        TODO()
    }

    override fun getInstrumentState(): InstrumentState =
            spectrogramView.spectrogramViewState

}
