package de.voicegym.voicegym.recordActivity.views

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.MotionEvent.ACTION_DOWN
import android.view.MotionEvent.ACTION_MOVE
import android.view.MotionEvent.ACTION_UP
import android.view.View
import de.voicegym.voicegym.recordActivity.RecordActivity
import de.voicegym.voicegym.recordActivity.fragments.SpectrogramFragment
import de.voicegym.voicegym.recordActivity.views.SpectrogramViewState.RECORDING
import org.jetbrains.anko.backgroundColor
import java.util.concurrent.LinkedBlockingDeque
import kotlin.math.roundToInt

class SpectrogramView : View {

    /**
     * The number of datapoints (blocks) to be displayed,
     * needs to be smaller or equal the number of pixels available
     */
    var xDataPoints: Int = 10

    /**
     * The number of PCM Samples per datapoint (block)
     */
    var samplesPerDataPoint: Int = 4096

    /**
     * the number of pixels per datapoint aka block
     */
    fun pixelPerFFTBlock(): Int = (getDrawAreaWidth() / xDataPoints).toInt()


    /**
     *  The instrument is drawn below the top_margin
     */
    var top_margin: Float = 20f

    /**
     *  The instrument is drawn above the above_margin
     */
    var bottom_margin: Float = 20f

    /**
     *  The instrument is drawn right to the left_margin
     */
    var left_margin: Float = 50f

    /**
     * The instrument is drawn left to the right_margin
     */
    var right_margin: Float = 20f

    fun getDrawAreaWidth(): Float = (width - left_margin - right_margin)

    fun getDrawAreaHeight(): Float = (height - top_margin - bottom_margin)

    /**
     * Whether to draw a border around the instrument area
     */
    var draw_border: Boolean = true

    /**
     * Which color to pick for the border
     */
    var border_color = Color.GRAY

    /**
     * The thickness of the border
     */
    var border_thickness = 3f

    var spectrogramViewState = RECORDING


    private var mCanvas: Canvas? = null
    private var mBitmap: Bitmap? = null
    private var mPath = Path()
    private var mPaint = Paint()
    private var buffer: IntArray? = null


    private var drawLine: Boolean = false
    private var yPosLine: Float = 0f

    init {
        // Set up the paint with which to draw.
        mPaint.color = Color.BLACK
        mPaint.isAntiAlias = true
        mPaint.isDither = true
        mPaint.style = Paint.Style.STROKE
        mPaint.strokeJoin = Paint.Join.ROUND
        mPaint.strokeCap = Paint.Cap.ROUND
        mPaint.strokeWidth = 1f
    }

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) : super(context, attrs, defStyleAttr)

    val leftDeque = LinkedBlockingDeque<IntArray>()
    val rightDeque = LinkedBlockingDeque<IntArray>()
    val currentDeque = LinkedBlockingDeque<IntArray>()


    private fun rotateBitmap(numberOfPixels: Int) {
        buffer?.let {
            // Rolling through pixels
            val cutSpaceLeft = if (numberOfPixels >= 0) numberOfPixels else 0
            val cutSpaceRight = if (numberOfPixels < 0) -numberOfPixels else 0
            mBitmap?.getPixels(it, 0, getDrawAreaWidth().toInt(), cutSpaceLeft, 0, getDrawAreaWidth().toInt() - numberOfPixels, getDrawAreaHeight().toInt())
            mBitmap?.setPixels(it, 0, getDrawAreaWidth().toInt(), cutSpaceRight, 0, getDrawAreaWidth().toInt() - numberOfPixels, getDrawAreaHeight().toInt())
        }
    }


    /**
     * Inserts one datapoint represented by its colorvalues when the view is in RECORDING MODE
     */
    fun insertNewDataPoint(colorValues: IntArray) {
        if (spectrogramViewState == RECORDING) {
            rightDeque.addLast(colorValues)
            left()
        } else throw Error("Cannot insert colorlines to SpectrogramView not in recording mode")
    }

    private fun drawFrequencyLine(canvas: Canvas?) {
        val width = getDrawAreaWidth()
        mPaint.color = Color.GRAY
        mPaint.strokeWidth = 2f
        mPaint.textSize = 24f
        canvas?.drawLine(left_margin, yPosLine, width + left_margin, yPosLine, mPaint)
        if (context is RecordActivity) {
            val fragment = (context as RecordActivity).getInstrumentFragment()
            if (fragment is SpectrogramFragment) {
                val deltaFrequency = (fragment as SpectrogramFragment).deltaFrequency
                val frequency = (fragment as SpectrogramFragment).userSpectrogramSettings.fromFrequency + (bottom_margin + getDrawAreaHeight() - yPosLine) * deltaFrequency
                canvas?.drawText("${frequency.toInt()} Hz", left_margin + 5, yPosLine - 5, mPaint)
            } else {
                throw Error("InstrumentFragment was not a SpectrogramFragment, please expand SpectrogramView")
            }

        }
    }

    private fun drawBorder(canvas: Canvas) {
        val paint = Paint()
        paint.color = border_color
        paint.strokeWidth = border_thickness
        paint.style = Paint.Style.STROKE
        val bottom = (height - bottom_margin + border_thickness)
        val top = (top_margin - border_thickness)
        val left = (left_margin - border_thickness)
        val right = (width - right_margin + border_thickness)
        canvas.drawRect(left, top, right, bottom, paint)
    }

    override fun onDraw(canvas: Canvas?) {
        backgroundColor = Color.BLACK
        super.onDraw(canvas)

        if (canvas != null) {
            // Draw internal bitmap and path
            canvas.drawBitmap(mBitmap, left_margin, top_margin, mPaint)
            canvas.drawPath(mPath, mPaint)
            // Draw Instrument Tools
            if (draw_border) drawBorder(canvas)
        }

        if (drawLine) drawFrequencyLine(canvas)
    }


    override fun onTouchEvent(event: MotionEvent?): Boolean {
        if (spectrogramViewState == RECORDING) {
            when (event?.action) {
                ACTION_DOWN -> {
                    drawLine = !drawLine
                    yPosLine = event.y
                    return true
                }

                ACTION_MOVE -> {
                    yPosLine = event.y
                    return true
                }

                ACTION_UP   -> {
                    yPosLine = event.y
                    return true
                }
            }
        }
        return super.onTouchEvent(event)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)

        clearBitmapAndBuffer()
        //TODO recalulate the content in the deques
        leftDeque.clear()
        rightDeque.clear()
        currentDeque.clear()
    }

    fun clearBitmapAndBuffer() {
        mBitmap = Bitmap.createBitmap((width - left_margin - right_margin).toInt(), (height - top_margin - bottom_margin).toInt(), Bitmap.Config.ARGB_8888)
        mCanvas = Canvas(mBitmap)
        buffer = IntArray((getDrawAreaHeight() * getDrawAreaWidth()).toInt())
    }


    fun totalSamples() = currentDeque.size + leftDeque.size + rightDeque.size
    var currentDequePosition: Int = 0

    fun rewindDeques() {
        currentDequePosition = 0
        while (currentDeque.isNotEmpty()) rightDeque.push(currentDeque.poll())
        while (leftDeque.isNotEmpty()) rightDeque.push(leftDeque.poll())
    }

    private fun left() {
        rotateBitmap((getDrawAreaWidth() / xDataPoints).roundToInt())
        if (rightDeque.size > 0) {
            val newDataPoint = rightDeque.poll()
            currentDeque.push(newDataPoint)
            if (currentDeque.size > xDataPoints) leftDeque.push(currentDeque.removeLast())
            val deltaX: Float = getDrawAreaWidth() / xDataPoints as Int
            val lx = deltaX * (xDataPoints as Int - 1)
            val lowerY = getDrawAreaHeight()

            newDataPoint.forEachIndexed { i, colorValue ->
                mPaint.color = colorValue
                mCanvas?.drawLine(lx, lowerY - i, lx + deltaX, lowerY - i, mPaint)
            }

        } else throw Error("Stack has reached the end to the right")
        currentDequePosition += 1
        this.invalidate()
    }

    private fun right() {
        if (currentDeque.size + leftDeque.size > 0) {
            val newDataPoint = if (leftDeque.size > 0) leftDeque.poll() else null
            rightDeque.push(currentDeque.poll())
            rotateBitmap(-(getDrawAreaWidth() / xDataPoints).roundToInt())
            newDataPoint?.let {
                currentDeque.addLast(it)
                val deltaX: Float = getDrawAreaWidth() / xDataPoints as Int
                val lowerY = getDrawAreaHeight()

                newDataPoint.forEachIndexed { i, colorValue ->
                    mPaint.color = colorValue
                    mCanvas?.drawLine(0f, lowerY - i, deltaX, lowerY - i, mPaint)
                }
            }

        } else throw Error("Stack has reached its end to the left")
        currentDequePosition -= 1
        this.invalidate()
    }

    fun seekTo(sampleNumber: Int) {
        val position = sampleNumber / samplesPerDataPoint
        windToPosition(position)
    }

    private fun windToPosition(position: Int) {
        if (position < 0) throw IllegalArgumentException("SpectrogramView.kt: sampleNumber has to be positive or zero")

        if (position < currentDequePosition) {
            while (currentDequePosition > position) right()
        } else if (position > currentDequePosition) {
            if (position > totalSamples()) throw IllegalArgumentException("SpectrogramView.kt: sampleNumber must be smaller than total collected samples")
            while (currentDequePosition < position) left()
        }
    }


}

enum class SpectrogramViewState {
    PLAYBACK,
    RECORDING
}
