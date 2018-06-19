package de.voicegym.voicegym.menu.settings

import android.content.Context
import android.media.AudioFormat
import android.preference.PreferenceManager

/**
 * This object is intended as a single access point for all constants and user-defined variables (settings)
 */
object SettingsBundle {

    // constants
    const val sampleRate = 44100
    const val channelConfig = AudioFormat.CHANNEL_IN_MONO
    const val audioFormat = AudioFormat.ENCODING_PCM_16BIT

    /**
     * This function returns an object that contains all settings related to the FourierInstrumentViewSettings
     * @return FourierInstrumentViewSettings that are currently active
     */
    fun getFourierInstrumentViewSettings(context: Context): FourierInstrumentViewSettings {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        // collect the values
        // TODO implement NumberPickerPreference
        val blockSize = sharedPreferences.getString("fft_blocksize", "4096").toInt()
        val binning = sharedPreferences.getString("fft_binning", "2").toInt()
        val fromFrequency = sharedPreferences.getString("from_frequency", "10").toDouble()
        val tillFrequency = sharedPreferences.getString("till_frequency", "1000").toDouble()
        val isLogarithmic = sharedPreferences.getBoolean("display_logarithmic", false)
        val displayedDatapoints = sharedPreferences.getString("display_sample_numbers", "100").toInt()
        return FourierInstrumentViewSettings(blockSize, binning, fromFrequency, tillFrequency, displayedDatapoints, isLogarithmic)
    }
}

/**
 * Data Class to bundle all settings related to InstrumentIn and the Fourier Transformation
 */
data class FourierInstrumentViewSettings(val blockSize: Int,
                                         val binning: Int,
                                         val fromFrequency: Double,
                                         val tillFrequency: Double,
                                         val displayedDatapoints: Int,
                                         val isLogarithmic: Boolean) {

    /**
     * not an independent setting, i.e. must be calculated by blockSize and binning
     */
    val samplesPerDatapoint = blockSize * binning
}
