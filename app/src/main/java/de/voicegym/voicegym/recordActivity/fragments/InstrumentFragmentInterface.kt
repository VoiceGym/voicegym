package de.voicegym.voicegym.recordActivity.fragments

import de.voicegym.voicegym.menu.settings.FourierInstrumentViewSettings

interface InstrumentFragmentInterface {
    fun updateInstrumentViewSettings(settings: FourierInstrumentViewSettings)

    fun updateFrequencyArray(frequencies: DoubleArray)

    fun insertNewAmplitudes(spectrum: DoubleArray)

    var settings: FourierInstrumentViewSettings

    fun getCurrentSamplePosition(): Int

    fun seekToSamplePosition(samplePosition: Int)

    fun resetFragment()

    fun startRecording()

    fun doneRecordingSwitchToPlayback()

    fun getInstrumentState(): InstrumentState

}


enum class InstrumentState {
    LIVE_DISPLAY,
    RECORDING_DATA,
    PLAYBACK
}
