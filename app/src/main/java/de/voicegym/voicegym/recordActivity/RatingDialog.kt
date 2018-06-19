package de.voicegym.voicegym.recordActivity

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.Window
import android.widget.ImageView
import de.voicegym.voicegym.R

class RatingDialog : Dialog {

    private lateinit var stars: Array<ImageView>

    constructor(context: Context) : this(context, 0)
    constructor(context: Context, themeResId: Int) : super(context, themeResId)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.rating_dialog)
        val star4 = findViewById<ImageView>(R.id.rate4)
        val star3 = findViewById<ImageView>(R.id.rate3)
        val star2 = findViewById<ImageView>(R.id.rate2)
        val star1 = findViewById<ImageView>(R.id.rate1)
        stars = arrayOf(star4, star3, star2, star1)
        stars.forEachIndexed { idx, view ->
            view.setOnClickListener {
                selectStar(idx)
            }
        }
    }


    private fun selectStar(idx: Int) {
        for (i in 0 until stars.size) {
            if (i < idx) {
                setStar(i, false)
            } else {
                setStar(i, true)
            }
        }
    }

    private fun setStar(idx: Int, on: Boolean) {
        val star = stars[idx]
        if (on) {
            star.setImageResource(R.drawable.star_big_on)
        } else {
            star.setImageResource(R.drawable.star_big_off)
        }
    }
}
