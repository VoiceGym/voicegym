package de.voicegym.voicegym.recordActivity

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.view.KeyEvent
import android.view.Window
import android.widget.ImageView
import de.voicegym.voicegym.R
import de.voicegym.voicegym.recordActivity.fragments.PlaybackModeControlListener

class RatingDialog : Dialog {

    /**
     * stars keeps the views that display the rating
     */
    private lateinit var stars: List<ImageView>

    var playbackModeControlListener: PlaybackModeControlListener? = null

    constructor(context: Context) : this(context, 0)
    constructor(context: Context, themeResId: Int) : super(context, themeResId)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.rating_dialog)
        // get views from layout
        val rate0 = findViewById<ImageView>(R.id.rate0)
        rate0.setOnClickListener {
            rate0.setImageResource(R.drawable.ic_cancel_on)
            sendRating(0, 200)
        }

        val star1 = findViewById<ImageView>(R.id.rate1)
        val star2 = findViewById<ImageView>(R.id.rate2)
        val star3 = findViewById<ImageView>(R.id.rate3)
        stars = listOf(star1, star2, star3)
        // set all the OnClickListeners of the Views
        stars.forEachIndexed { idx, view ->
            view.setOnClickListener {
                selectRating(idx)
            }
        }
    }

    /**
     * This method selects a specific rating, and displays it on the screen.
     */
    private fun selectRating(idx: Int) {
        for (i in 0 until stars.size) {
            if (i > idx) {
                setStar(i, false)
            } else {
                setStar(i, true)
            }
        }
        sendRating(idx + 1, 200)
    }

    private fun sendRating(rating: Int, delayMillis: Long) {
        Handler().postDelayed({
            hide()
            playbackModeControlListener?.receiveRating(rating)
        }, delayMillis)
    }

    /**
     * this private method changes the appearance of the star referenced to by index
     */
    private fun setStar(idx: Int, on: Boolean) {
        val star = stars[idx]
        if (on) {
            star.setImageResource(R.drawable.ic_star_on)
        } else {
            star.setImageResource(R.drawable.ic_star_off)
        }
    }

    /**
     * overriden so KeyEvent.KEYCODE_BACK can dismiss the dialog
     */
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        val ret = super.onKeyDown(keyCode, event)

        if (keyCode == KeyEvent.KEYCODE_BACK) {
            dismiss()
        }
        return ret
    }
}
