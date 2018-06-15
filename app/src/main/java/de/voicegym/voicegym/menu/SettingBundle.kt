package de.voicegym.voicegym.menu

import android.content.Context
import android.preference.PreferenceManager

object SettingBundle {
    fun getFourierInstrumentViewSettings(context: Context): FourierInstrumentViewSettings {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        val blocksize = sharedPreferences.getString("fft_blocksize", "4096").toInt()
        val binning = sharedPreferences.getString("fft_binning", "2").toInt()
        val fromFrequency = sharedPreferences.getString("from_frequency", "10").toDouble()
        val tillFrequency = sharedPreferences.getString("till_frequency", "1000").toDouble()
        //TODO obtain logarithmic option
        return FourierInstrumentViewSettings(blocksize, binning, fromFrequency, tillFrequency, false)
    }
}

data class FourierInstrumentViewSettings(val blockSize: Int,
                                         val binning: Int,
                                         val fromFrequency: Double,
                                         val tillFrequency: Double,
                                         val isLogarithmic: Boolean) {

    val samplesPerDatapoint = blockSize * binning
}
