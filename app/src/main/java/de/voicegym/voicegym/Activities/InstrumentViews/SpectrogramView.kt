package de.voicegym.voicegym.Activities.InstrumentViews

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet

//TODO SOLVE HOW TO UPDATE THE VIEW AT GIVEN INTERVALS
class SpectrogramView : InstrumentView {
    override fun drawInstrument(canvas: Canvas) {

        val paint = Paint()
        paint.color = Color.WHITE
        paint.textSize = 46f
        canvas.drawText("TestText", left_margin + 20f, 80f + top_margin, paint)
    }


    constructor(context: Context) : this(context, null)

    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)

    constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) : super(context, attrs, defStyleAttr) {

    }

}