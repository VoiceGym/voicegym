package de.voicegym.voicegym.recordActivity.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import de.voicegym.voicegym.R
import de.voicegym.voicegym.menu.settings.FourierInstrumentViewSettings
import de.voicegym.voicegym.recordActivity.fragments.InstrumentState.LIVE_DISPLAY
import de.voicegym.voicegym.recordActivity.fragments.InstrumentState.PLAYBACK
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
    private var zeroAmplitudes: DoubleArray? = null

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
        zeroAmplitudes = DoubleArray(frequencies.size)
    }

    override fun updateInstrumentViewSettings(settings: FourierInstrumentViewSettings) {
        this.settings = settings
        spectrogramView.updateInstrumentViewSettings(settings)
    }

    private val savedSpectra = ArrayList<DoubleArray>()

    private var currentPosition = 0

    private fun leftBorderPosition() = currentPosition - settings.displayedDatapoints

    /**
     * this is the callback that receives a new amplitudeArray
     */
    override fun insertNewAmplitudes(spectrum: DoubleArray) {
        spectrogramView.addRight(spectrum)
        spectrogramView.invalidate()
        if (spectrogramView.spectrogramViewState == RECORDING_DATA) {
            savedSpectra.add(spectrum)
            currentPosition++
        }
    }

    private fun scrollLeft() {
        zeroAmplitudes?.let {
            if (spectrogramView.spectrogramViewState == PLAYBACK) {
                currentPosition--
                spectrogramView.addLeft(
                        if (leftBorderPosition() < 0) {
                            it
                        } else {
                            savedSpectra[leftBorderPosition()]
                        })
            }
        }
        spectrogramView.invalidate()
    }

    private fun scrollRight() {
        if (spectrogramView.spectrogramViewState == PLAYBACK) {
            if (currentPosition < savedSpectra.size - 1) {
                spectrogramView.addRight(savedSpectra[++currentPosition])
            }
        }
        spectrogramView.invalidate()
    }

    override fun getCurrentSamplePosition(): Int = currentPosition * settings.samplesPerDatapoint

    override fun seekToSamplePosition(samplePosition: Int) {
        val position = samplePosition / settings.samplesPerDatapoint

        val scrollTo = when {
            position < savedSpectra.size && position >= 0 -> position
            position < 0                                  -> 0
            else                                          -> savedSpectra.size - 1
        }

        while (scrollTo != currentPosition) {
            if (scrollTo < currentPosition) scrollLeft()
            else if (scrollTo > currentPosition) scrollRight()
        }
    }

    override fun resetFragment() {
        spectrogramView.let {
            it.clearBitmapAndBuffer()
            it.spectrogramViewState = LIVE_DISPLAY
            it.invalidate()
        }
        savedSpectra.clear()
    }

    override fun startRecording() {
        spectrogramView.spectrogramViewState = RECORDING_DATA
    }

    override fun doneRecordingSwitchToPlayback() {
        spectrogramView.spectrogramViewState = InstrumentState.PLAYBACK
        spectrogramView.clearBitmapAndBuffer()
        val left = if (leftBorderPosition() >= 0) currentPosition - settings.displayedDatapoints else 0
        for (i in left until savedSpectra.size - 1) spectrogramView.addRight(savedSpectra[i])
        spectrogramView.invalidate()
    }

    override fun cutToMaximumSampleNumber(samples: Int) {
        if (samples < settings.samplesPerDatapoint * savedSpectra.size) {
            val remove = savedSpectra.subList(samples / settings.samplesPerDatapoint, savedSpectra.size - 1)
            savedSpectra.removeAll(remove)
        }
    }

    override fun getInstrumentState(): InstrumentState =
            spectrogramView.spectrogramViewState

}
