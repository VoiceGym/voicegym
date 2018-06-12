package de.voicegym.voicegym.recordActivity.fragments

interface InstrumentFragment {
    fun updateUserSettings(userSettings: UserSettings)

    fun insertNewAmplitudes(spectrum: DoubleArray)
}
