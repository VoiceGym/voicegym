package de.voicegym.voicegym.recordActivity.fragments

interface InstrumentFragmentInterface {
    fun updateUserSettings(userSettings: UserSettings)

    fun insertNewAmplitudes(spectrum: DoubleArray)

    var userSettings: UserSettings?  // TODO make not nullable

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
