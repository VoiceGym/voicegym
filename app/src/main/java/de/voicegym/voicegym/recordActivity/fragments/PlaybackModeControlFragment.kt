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
    private var rateButton: FloatingActionButton? = null
    private var saveButton: FloatingActionButton? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_playback_mode_control, container, false)
        playPauseButton = view.findViewById(R.id.playPauseControlButton)
        rateButton = view.findViewById(R.id.rateControlButton)
        saveButton = view.findViewById(R.id.saveControlButton)

        playPauseButton?.setOnClickListener { playbackModeControlListener?.playPause() }
        rateButton?.setOnClickListener { playbackModeControlListener?.openRatingDialog() }
        saveButton?.setOnClickListener { playbackModeControlListener?.saveToSdCard() }
        return view
    }

    fun hideSaveButton(){
        saveButton?.hide()
    }

    fun showSaveButton(){
        saveButton?.show()
    }
}

interface PlaybackModeControlListener {

    /**
     * rate button was pressed
     */
    fun openRatingDialog()

    /**
     * rating dialog has a value for us
     */
    fun receiveRating(rating: Int)

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

