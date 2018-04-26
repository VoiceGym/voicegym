package de.voicegym.voicegym.Activities.InstrumentViews

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet

//TODO SOLVE HOW TO UPDATE THE VIEW AT GIVEN INTERVALS
class SpectrogramView : InstrumentView {


    fun getDrawAreaWidth(): Float = (width - left_margin - right_margin)

    fun getDrawAreaHeight(): Float = (height - top_margin - bottom_margin)

    var colorQueue = ArrayList<IntArray>()

    fun refreshColorArray() {
        if (xDataPoints != null) {
            shrinkColorValueList()

            var toBeRemoved = ArrayList<IntArray>()
            colorQueue.forEach {
                if (it.size != getDrawAreaHeight().toInt()) {
                    toBeRemoved.add(it)
                }
            }

            toBeRemoved.forEach {
                colorQueue.remove(it)
            }
        }
    }

    fun insertColorLine(colorValues: IntArray) {
        if (xDataPoints != null && colorQueue != null && colorValues.size == getDrawAreaHeight().toInt()) {
            colorQueue.add(colorValues)
            shrinkColorValueList()
        }
    }

    private fun shrinkColorValueList() {
        while (colorQueue.size > xDataPoints as Int) {
            colorQueue.remove(colorQueue.get(0))
        }
    }

    override fun drawInstrument(canvas: Canvas) {
        if (xDataPoints != null && colorQueue != null) {
            val paint = Paint()
            paint.strokeWidth = 1f
            paint.style = Paint.Style.STROKE

            val deltaX = getDrawAreaWidth() / xDataPoints as Int
            val offset = xDataPoints as Int - colorQueue.size
            val lowerY = top_margin + getDrawAreaHeight()
            var lx = deltaX * (offset - 1) + left_margin

            for (i in 0 until colorQueue.size) {
                lx = lx + deltaX
                val colorValues = colorQueue.get(i)
                colorValues.forEachIndexed { i, color ->
                    run {
                        paint.color = color
                        canvas.drawLine(lx, lowerY - i, lx + deltaX, lowerY - i, paint)
                    }
                }
            }
        }

    }


    constructor(context: Context) : this(context, null)

    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)

    constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) : super(context, attrs, defStyleAttr) {

    }

}