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
        return view
    }


    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        if (frequencyArray == null) throw Error("Didn't pass required arguments to fragments, frequencyArray missing")
    }

    override fun updateFrequencyArray(frequencies: DoubleArray) {
        frequencyArray = frequencies
        spectrogramView.frequencyArray = frequencies
    }

    override fun updateInstrumentViewSettings(settings: FourierInstrumentViewSettings) {
        this.settings = settings
        spectrogramView.updateInstrumentViewSettings(settings)
    }

    /**
     * this is the callback that receives a new amplitudeArray
     */
    override fun insertNewAmplitudes(spectrum: DoubleArray) {
        spectrogramView.addRight(spectrum)
    }


    override fun getCurrentSamplePosition(): Int = 0

    override fun seekToSamplePosition(samplePosition: Int) {
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
        }
    }

    override fun cutToMaximumSampleNumber(samples: Int) {

    }

    override fun getInstrumentState(): InstrumentState =
            spectrogramView.spectrogramViewState

}
