package de.voicegym.voicegym.menu.settings

import android.content.Context
import android.media.AudioFormat
import android.preference.PreferenceManager

object SettingBundle {
    // constants
    const val sampleRate = 44100
    const val channelConfig = AudioFormat.CHANNEL_IN_MONO
    const val audioFormat = AudioFormat.ENCODING_PCM_16BIT

    // user settings
    fun getFourierInstrumentViewSettings(context: Context): FourierInstrumentViewSettings {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        // collect the values
        val blockSize = sharedPreferences.getString("fft_blocksize", "4096").toInt()
        val binning = sharedPreferences.getString("fft_binning", "2").toInt()
        val fromFrequency = sharedPreferences.getString("from_frequency", "10").toDouble()
        val tillFrequency = sharedPreferences.getString("till_frequency", "1000").toDouble()
        val isLogarithmic = sharedPreferences.getBoolean("display_logarithmic", false)
        val displayedDatapoints = sharedPreferences.getString("display_sample_numbers", "100").toInt()
        return FourierInstrumentViewSettings(blockSize, binning, fromFrequency, tillFrequency, displayedDatapoints, isLogarithmic)
    }
}

data class FourierInstrumentViewSettings(val blockSize: Int,
                                         val binning: Int,
                                         val fromFrequency: Double,
                                         val tillFrequency: Double,
                                         val displayedDatapoints: Int,
                                         val isLogarithmic: Boolean) {

    val samplesPerDatapoint = blockSize * binning
}
