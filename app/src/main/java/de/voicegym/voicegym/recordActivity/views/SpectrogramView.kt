package de.voicegym.voicegym.recordActivity.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.MotionEvent.ACTION_DOWN
import android.view.MotionEvent.ACTION_MOVE
import android.view.MotionEvent.ACTION_UP
import de.voicegym.voicegym.recordActivity.RecordActivity
import de.voicegym.voicegym.recordActivity.fragments.SpectrogramFragment

class SpectrogramView : InstrumentView {

    private var drawLine: Boolean = false
    private var yPosLine: Float = 0f

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)

        if (drawLine) drawFrequencyLine(canvas)
    }

    private fun drawFrequencyLine(canvas: Canvas?) {
        val width = getDrawAreaWidth()
        mPaint.color = Color.GRAY
        mPaint.strokeWidth = 2f
        mPaint.textSize = 24f
        canvas?.drawLine(left_margin, yPosLine, width + left_margin, yPosLine, mPaint)
        if (context is RecordActivity) {
            val deltaFrequency = (context as SpectrogramFragment).deltaFrequency
            val frequency = (context as SpectrogramFragment).userSpectrogramSettings.fromFrequency + (bottom_margin + getDrawAreaHeight() - yPosLine) * deltaFrequency
            canvas?.drawText("${frequency.toInt()} Hz", left_margin + 5, yPosLine - 5, mPaint)
        }
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        when (event?.action) {
            ACTION_DOWN -> {
                drawLine = !drawLine
                yPosLine = event.y
                return super.onTouchEvent(event)
            }

            ACTION_MOVE -> {
                yPosLine = event.y
                return super.onTouchEvent(event)
            }

            ACTION_UP   -> {
                yPosLine = event.y
                return super.onTouchEvent(event)
            }

        }
        return super.onTouchEvent(event)
    }

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) : super(context, attrs, defStyleAttr)

    fun insertColorLine(colorValues: IntArray) {
        val deltaX: Float = getDrawAreaWidth() / xDataPoints as Int
        val lx = deltaX * (xDataPoints as Int - 1)
        val lowerY = getDrawAreaHeight()
        moveBitmap(deltaX.toInt())
        colorValues.forEachIndexed { i, color ->
            mPaint.color = color
            mCanvas!!.drawLine(lx, lowerY - i, lx + deltaX, lowerY - i, mPaint)
        }
    }
}
