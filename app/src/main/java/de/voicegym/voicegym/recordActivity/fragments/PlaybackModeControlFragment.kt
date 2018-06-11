package de.voicegym.voicegym.recordActivity.fragments

import android.content.Context
import android.os.Bundle
import android.support.design.widget.FloatingActionButton
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
            throw Error("Needs to be called from a context that implements PlaybackModeControlListener")
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_playback_mode_control, container, false)
        val playPauseButton = view.findViewById<FloatingActionButton>(R.id.playPauseControlButton)
        val rateButton = view.findViewById<FloatingActionButton>(R.id.rateControlButton)
        val saveButton = view.findViewById<FloatingActionButton>(R.id.saveControlButton)

        playPauseButton.setOnClickListener { playbackModeControlListener?.playPause() }
        rateButton.setOnClickListener { playbackModeControlListener?.rate() }
        saveButton.setOnClickListener { playbackModeControlListener?.saveToSdCard() }
        return view
    }

}

interface PlaybackModeControlListener {
    fun rate()
    fun playPause()
    fun saveToSdCard()

    fun playbackTouched()
    fun playbackReleased()
    fun playbackSeekTo(relativeMovement: Float)
}
