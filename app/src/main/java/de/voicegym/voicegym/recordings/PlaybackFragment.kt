package de.voicegym.voicegym.recordings

import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import de.voicegym.voicegym.R
import de.voicegym.voicegym.menu.settings.FourierInstrumentViewSettings
import de.voicegym.voicegym.menu.settings.SettingsBundle
import de.voicegym.voicegym.recordActivity.views.SpectrogramView
import de.voicegym.voicegym.util.math.FourierHelper


class PlaybackFragment : Fragment() {
    /**
     * Access to the frequency space
     */
    private lateinit var fourierHelper: FourierHelper

    private lateinit var fileName: String

    /**
     * here the FourierInstrumentViewSettings are stored that were active when the activity was started
     */
    private lateinit var settings: FourierInstrumentViewSettings

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment

        val context = context ?: throw Error("Could not get context")
        settings = SettingsBundle.getFourierInstrumentViewSettings(context)
        fourierHelper = FourierHelper(
                settings.blockSize,
                settings.binning,
                settings.samplesPerDatapoint,
                SettingsBundle.sampleRate)

        val view = inflater.inflate(R.layout.fragment_playback, container, false)
        val instrument = view.findViewById<SpectrogramView>(R.id.playback_instrumentView)
        instrument.frequencyArray = fourierHelper.frequencyArray()
        return view
    }

    fun loadFile(fileName: String) {
        this.fileName = fileName
    }


}
