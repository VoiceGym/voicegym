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
import org.jetbrains.anko.backgroundColor
import java.util.concurrent.LinkedBlockingDeque

class SpectrogramView : View {

    /**
     * needs to be smaller or equal the number of pixels available
     */
    var xDataPoints: Int = 10

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

    val leftPixelStore = LinkedBlockingDeque<IntArray>()
    val rightPixelStore = LinkedBlockingDeque<IntArray>()

    fun moveBitmap(numberOfPixels: Int) {
        if (numberOfPixels > 0) {
            // forward
            rotateBitmapToLeft(numberOfPixels)
            paintRightStackOnBitmap(numberOfPixels)

        } else if (numberOfPixels < 0) {
            if ((-1) * numberOfPixels > leftPixelStore.size) throw Error("Not enough pixels prepared to the left")
            // backward
        } else {
            // do not move
        }
    }

    private fun paintRightStackOnBitmap(numberOfPixels: Int) {
        if (numberOfPixels > rightPixelStore.size) throw Error("Not enough pixels prepared to the right")
        for (i in 0 until numberOfPixels) {
            val pixelLine = rightPixelStore.poll()
            // store the pixels while recording for playbackmode
            if (context is RecordActivity && (context as RecordActivity).isRecording()) leftPixelStore.push(pixelLine)
            mBitmap?.setPixels(pixelLine, 0, 1, getDrawAreaWidth().toInt() - numberOfPixels + i, 0, 1, getDrawAreaHeight().toInt())
        }
    }

    private fun rotateBitmapToLeft(numberOfPixels: Int) {
        buffer?.let {
            // Rolling through pixels
            //TODO if in playback rotate pixels on right and leftDeque
            mBitmap?.getPixels(it, 0, getDrawAreaWidth().toInt(), numberOfPixels, 0, getDrawAreaWidth().toInt() - numberOfPixels, getDrawAreaHeight().toInt())
            mBitmap?.setPixels(it, 0, getDrawAreaWidth().toInt(), 0, 0, getDrawAreaWidth().toInt() - numberOfPixels, getDrawAreaHeight().toInt())
        }
    }

    fun pixelPerFFTBlock(): Int = (getDrawAreaWidth() / xDataPoints).toInt()


    fun insertColorLine(colorValues: IntArray) {
        val lx = pixelPerFFTBlock() * (xDataPoints as Int - 1)
        val lowerY = getDrawAreaHeight()

        val drawLine = Bitmap.createBitmap(1, getDrawAreaHeight().toInt(), Bitmap.Config.ARGB_8888)
        val canvas = Canvas(drawLine)

        colorValues.forEachIndexed { i, color ->
            mPaint.color = color
            canvas.drawLine(0.0f, lowerY - i, 1.0f, lowerY - i, mPaint)
        }
        val pixels = IntArray(getDrawAreaHeight().toInt())
        drawLine.getPixels(pixels, 0, 1, 0, 0, 1, getDrawAreaHeight().toInt())
        for (i in 0 until pixelPerFFTBlock()) rightPixelStore.push(pixels)
        moveBitmap(pixelPerFFTBlock())
        currentDequePosition += (context as RecordActivity).collectedSamples
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

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)

        clearBitmapAndBuffer()
        leftPixelStore.clear()
        rightPixelStore.clear()
    }

    fun clearBitmapAndBuffer() {
        mBitmap = Bitmap.createBitmap((width - left_margin - right_margin).toInt(), (height - top_margin - bottom_margin).toInt(), Bitmap.Config.ARGB_8888)
        mCanvas = Canvas(mBitmap)
        buffer = IntArray((getDrawAreaHeight() * getDrawAreaWidth()).toInt())
    }

    var currentDequePosition: Long = 0

    fun rewindDequesSkippingImage() {
        currentDequePosition = 0
        while (leftPixelStore.isNotEmpty()) rightPixelStore.push(leftPixelStore.poll())
    }

    fun windForward(samples: Long) {
        val blocks: Int = (samples / (context as RecordActivity).collectedSamples).toInt()
        if (blocks > 0) {
            val pixels = blocks * pixelPerFFTBlock()
            if (rightPixelStore.size >= pixels) moveBitmap(pixels)
        }
    }


}
