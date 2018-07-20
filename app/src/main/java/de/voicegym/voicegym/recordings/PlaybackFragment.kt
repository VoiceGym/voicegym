package de.voicegym.voicegym.recordings

import android.content.Context
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import de.voicegym.voicegym.R
import de.voicegym.voicegym.menu.settings.FourierInstrumentViewSettings
import de.voicegym.voicegym.menu.settings.SettingsBundle
import de.voicegym.voicegym.recordActivity.views.SpectrogramView
import de.voicegym.voicegym.util.audio.PCMPlayer
import de.voicegym.voicegym.util.audio.PCMStorage
import de.voicegym.voicegym.util.audio.SoundFile
import de.voicegym.voicegym.util.math.FourierHelper


class PlaybackFragment : Fragment() {
    /**
     * Access to the frequency space
     */
    private lateinit var fourierHelper: FourierHelper

    private lateinit var fileName: String

    private lateinit var pcmStorage: PCMStorage

    private lateinit var instrumentView: SpectrogramView

    private lateinit var pcmPlayer: PCMPlayer

    private lateinit var internalContext: Context

    /**
     * here the FourierInstrumentViewSettings are stored that were active when the activity was started
     */
    private lateinit var settings: FourierInstrumentViewSettings

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        arguments?.let {
            fileName = it.getString(AUDIO_FILE)
        }



        internalContext = context ?: throw Error("Could not get context")
        settings = SettingsBundle.getFourierInstrumentViewSettings(internalContext)
        fourierHelper = FourierHelper(
                settings.blockSize,
                settings.binning,
                settings.samplesPerDatapoint,
                SettingsBundle.sampleRate)

        val view = inflater.inflate(R.layout.fragment_playback, container, false)
        instrumentView = view.findViewById(R.id.playback_instrumentView)
        instrumentView.frequencyArray = fourierHelper.frequencyArray()
        return view
    }

    fun processFile() {
        val soundFile = SoundFile.create(fileName, null)
        if (soundFile != null) {
            val samplesBuffer = soundFile.samples
                    ?: throw Error("Error probably while processing Audiofile. Samples null.")
            val samplesArray = ShortArray(soundFile.numSamples)
            samplesBuffer.get(samplesArray)
            pcmStorage = PCMStorage(soundFile.sampleRate)
            pcmStorage.onBufferReady(samplesArray)
            pcmStorage.stopListening()
            pcmPlayer = PCMPlayer(soundFile.sampleRate, pcmStorage.asShortBuffer(), internalContext)
            pcmPlayer.play()
        }


    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        processFile()
    }

    companion object {
        const val AUDIO_FILE = "audio-file"
    }
}
