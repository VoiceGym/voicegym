package de.voicegym.voicegym.recordActivity.fragments

import android.content.Context
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import de.voicegym.voicegym.R


class PlaybackModeControlFragment : Fragment() {

    var playbackModeControlListener: PlaybackModeControlListener? = null

    override fun onAttach(context: Context?) {
        super.onAttach(context)

        playbackModeControlListener = if (context is PlaybackModeControlListener) {
            context
        } else {
            throw Error("Needs to be called from a context that implements RecordModeControlListener")
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_playback_mode_control, container, false)
    }

}

interface PlaybackModeControlListener {
    fun rate()
    fun play()
    fun pause()
    fun saveToSdCard()
    fun restart()
}
