package de.voicegym.voicegym.recordActivity.fragments

interface InstrumentFragmentInterface {
    fun updateUserSettings(userSettings: UserSettings)

    fun updateFrequencyArray(frequencies: DoubleArray)

    fun insertNewAmplitudes(spectrum: DoubleArray)

    var userSettings: UserSettings

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
