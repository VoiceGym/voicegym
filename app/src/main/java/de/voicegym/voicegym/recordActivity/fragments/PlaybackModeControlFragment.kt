package de.voicegym.voicegym.recordActivity.fragments

import android.content.Context
import android.os.Bundle
import android.support.design.widget.FloatingActionButton
import android.support.v4.app.Fragment
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import de.voicegym.voicegym.R


class PlaybackModeControlFragment : Fragment() {

    private var playbackModeControlListener: PlaybackModeControlListener? = null

    override fun onAttach(context: Context?) {
        super.onAttach(context)

        playbackModeControlListener = if (context is PlaybackModeControlListener) {
            context
        } else {
            throw Error("Needs to be called from a context that implements PlaybackModeControlListener")
        }
    }

    private var playPauseButton: FloatingActionButton? = null
    private var saveButton: FloatingActionButton? = null
    private var backButton: FloatingActionButton? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_playback_mode_control, container, false)

        // PlayPauseButton
        playPauseButton = view.findViewById(R.id.playPauseControlButton)
        playPauseButton?.setOnClickListener {
            playbackModeControlListener?.playPause()
        }

        // SaveButton
        saveButton = view.findViewById(R.id.saveControlButton)
        saveButton?.setOnClickListener {
            playbackModeControlListener?.saveToSdCard()
            hideSaveButton()
        }

        //BackButton
        backButton = view.findViewById(R.id.playbackModeControl_backButton)
        backButton?.setImageResource(R.drawable.arrow_left_white)
        backButton?.setOnClickListener {
            activity?.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BACK))
            activity?.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_BACK))
        }



        return view
    }

    fun hideBackButton() {
        backButton?.hide()
    }

    fun showBackButton() {
        backButton?.show()
    }

    fun hideSaveButton() {
        saveButton?.hide()
    }

    fun showSaveButton() {
        saveButton?.show()
    }
}

interface PlaybackModeControlListener {

    /**
     * play or pause button was pressed
     */
    fun playPause()

    /**
     * save button was pressed
     */
    fun saveToSdCard()

    /**
     * called once the screen is touched during PlaybackMode and a series of TouchEvents is started
     */
    fun playbackTouched()

    /**
     * called once the series of TouchEvents is completed during PlaybackMode
     */
    fun playbackReleased()

    /**
     * called during the series of TouchEvents
     * @param relativeMovement is the relative movement (to the screen width), in between -1 to 1
     *      * -1: scroll one screen back
     *      * +1: scroll one screen forward
     */
    fun playbackSeekTo(relativeMovement: Float)
}

