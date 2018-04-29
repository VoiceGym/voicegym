package de.voicegym.voicegym.views

import android.content.Context
import android.util.AttributeSet

//TODO SOLVE HOW TO UPDATE THE VIEW AT GIVEN INTERVALS
class SpectrogramView : InstrumentView {

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) : super(context, attrs, defStyleAttr)

    fun insertColorLine(colorValues: IntArray) {
        val deltaX : Float = getDrawAreaWidth() / xDataPoints as Int
        val lx = deltaX * (xDataPoints as Int - 1)
        val lowerY = getDrawAreaHeight()
        moveBitmap(deltaX.toInt())
        colorValues.forEachIndexed { i, color ->
                mPaint.color = color
                mCanvas!!.drawLine(lx, lowerY - i, lx + deltaX, lowerY - i, mPaint)
        }
    }
}
