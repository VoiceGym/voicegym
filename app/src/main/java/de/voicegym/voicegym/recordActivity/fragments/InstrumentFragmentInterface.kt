package de.voicegym.voicegym.recordActivity.fragments

import de.voicegym.voicegym.menu.settings.FourierInstrumentViewSettings

/**
 * this interface has to be implemented in order to be able to use a fragment as an instrument within the RecordActivity
 */
interface InstrumentFragmentInterface {

    /**
     * passes along the current settings
     */
    fun updateInstrumentViewSettings(settings: FourierInstrumentViewSettings)

    /**
     * passes the fourierhelpers frequency array
     */
    fun updateFrequencyArray(frequencies: DoubleArray)

    /**
     * passes along an array of intensities in the frequency space
     */
    fun insertNewAmplitudes(spectrum: DoubleArray)

    /**
     * settings have to publicly accessible
     */
    var settings: FourierInstrumentViewSettings

    /**
     * return the currently displayed sample number
     */
    fun getCurrentSamplePosition(): Int

    /**
     * scroll the instrument
     *
     * @param samplePosition scroll to this sample number
     */
    fun seekToSamplePosition(samplePosition: Int)

    /**
     * reset the fragment and start over in Live_display mode
     */
    fun resetFragment()

    /**
     * start into recording mode
     */
    fun startRecording()

    /**
     * switch into playback mode
     */
    fun doneRecordingSwitchToPlayback()

    fun getInstrumentState(): InstrumentState

    /**
     * Informs the Instrument of the maximum sample number the Activity is able to play.
     *
     * @param samples the maximum sampleNumber
     */
    fun cutToMaximumSampleNumber(samples: Int)

}


enum class InstrumentState {
    LIVE_DISPLAY,
    RECORDING_DATA,
    PLAYBACK
}
