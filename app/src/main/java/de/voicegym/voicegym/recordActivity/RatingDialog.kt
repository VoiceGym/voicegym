package de.voicegym.voicegym.recordActivity

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.Window
import android.widget.ImageView
import de.voicegym.voicegym.R

class RatingDialog : Dialog {

    private lateinit var star4: ImageView
    private lateinit var star3: ImageView
    private lateinit var star2: ImageView
    private lateinit var star1: ImageView

    constructor(context: Context) : this(context, 0)
    constructor(context: Context, themeResId: Int) : super(context, themeResId)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.rating_dialog);
        star4 = findViewById(R.id.rate4)
        star3 = findViewById(R.id.rate3)
        star2 = findViewById(R.id.rate2)
        star1 = findViewById(R.id.rate1)
    }
}
