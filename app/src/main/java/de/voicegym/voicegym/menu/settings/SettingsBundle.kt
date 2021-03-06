package de.voicegym.voicegym.menu.settings

import android.content.Context
import android.media.AudioFormat
import android.preference.PreferenceManager

/**
 * Single access point for all constants and user-defined variables (settings)
 */
object SettingsBundle {

    // constants
    const val sampleRate = 44100
    const val channelConfig = AudioFormat.CHANNEL_IN_MONO
    const val audioFormat = AudioFormat.ENCODING_PCM_16BIT
    const val normalisationConstant = 55.0


    /**
     * Returns an object containing all settings related to the FourierInstrumentViewSettings
     * @return FourierInstrumentViewSettings that are currently active
     */
    fun getFourierInstrumentViewSettings(context: Context): FourierInstrumentViewSettings {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        // collect the values
        // TODO implement NumberPickerPreference
        val blockSize = sharedPreferences.getString("fft_blocksize", "4096").toInt()
        val binning = sharedPreferences.getString("fft_binning", "2").toInt()
        val fromFrequency = sharedPreferences.getString("from_frequency", "50").toDouble()
        val tillFrequency = sharedPreferences.getString("till_frequency", "8000").toDouble()
        val isLogarithmic = sharedPreferences.getBoolean("display_logarithmic", true)
        val drawScale = sharedPreferences.getBoolean("display_scale", true)
        val displayedDatapoints = sharedPreferences.getString("display_sample_numbers", "100").toInt()

        return FourierInstrumentViewSettings(blockSize, binning, fromFrequency, tillFrequency, displayedDatapoints, isLogarithmic, drawScale)
    }
}

/**
 * Data Class to bundle all settings related to InstrumentIn and the Fourier Transformation.
 */
data class FourierInstrumentViewSettings(
        val blockSize: Int,
        val binning: Int,
        val fromFrequency: Double,
        val tillFrequency: Double,
        val displayedDatapoints: Int,
        val isLogarithmic: Boolean,
        val drawScale: Boolean) {

    val samplesPerDatapoint by lazy { blockSize * binning }
}
