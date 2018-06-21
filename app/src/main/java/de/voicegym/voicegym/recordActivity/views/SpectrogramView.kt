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
import de.voicegym.voicegym.menu.settings.FourierInstrumentViewSettings
import de.voicegym.voicegym.recordActivity.fragments.InstrumentState.LIVE_DISPLAY
import de.voicegym.voicegym.recordActivity.fragments.InstrumentState.PLAYBACK
import de.voicegym.voicegym.recordActivity.fragments.InstrumentState.RECORDING_DATA
import de.voicegym.voicegym.recordActivity.fragments.InstrumentViewInterface
import de.voicegym.voicegym.recordActivity.fragments.PlaybackModeControlListener
import org.jetbrains.anko.backgroundColor
import kotlin.math.absoluteValue

class SpectrogramView : View, InstrumentViewInterface {

    override fun updateInstrumentViewSettings(settings: FourierInstrumentViewSettings) {
        this.settings = settings
        //TODO check if settings actually changed
        //TODO if settings changed
    }

    private lateinit var settings: FourierInstrumentViewSettings


    /**
     * the number of pixels per datapoint aka block
     */
    fun pixelPerFFTBlock(): Int = (getDrawAreaWidth() / settings.samplesPerDatapoint).toInt()


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

    var spectrogramViewState = LIVE_DISPLAY


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


    private fun rotateBitmap(numberOfPixels: Int) {
        buffer?.let {
            // Rolling through pixels
            val cutSpaceLeft = if (numberOfPixels >= 0) numberOfPixels else 0
            val cutSpaceRight = if (numberOfPixels < 0) -numberOfPixels else 0
            mBitmap?.getPixels(it, 0, getDrawAreaWidth().toInt(), cutSpaceLeft, 0, getDrawAreaWidth().toInt() - numberOfPixels.absoluteValue, getDrawAreaHeight().toInt())
            mBitmap?.setPixels(it, 0, getDrawAreaWidth().toInt(), cutSpaceRight, 0, getDrawAreaWidth().toInt() - numberOfPixels.absoluteValue, getDrawAreaHeight().toInt())
        }
    }


    /**
     * Inserts one datapoint represented by its colorvalues when the view is in LIVE_DISPLAY MODE
     */
    fun insertNewDataPoint(colorValues: IntArray) {
        if (spectrogramViewState != PLAYBACK) {
            TODO()
        } else throw Error("View already in Playback mode")
    }

    private fun drawFrequencyLine(canvas: Canvas?) {
        val width = getDrawAreaWidth()
        mPaint.color = Color.GRAY
        mPaint.strokeWidth = 2f
        mPaint.textSize = 24f
        canvas?.drawLine(left_margin, yPosLine, width + left_margin, yPosLine, mPaint)
        TODO()
        //val deltaFrequency = (fragment as SpectrogramFragment).deltaFrequency
        //val frequency = (fragment as SpectrogramFragment).settings.fromFrequency + (bottom_margin + getDrawAreaHeight() - yPosLine) * deltaFrequency
        //canvas?.drawText("${frequency.toInt()} Hz", left_margin + 5, yPosLine - 5, mPaint)

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

    var touchedAtXpos: Float = 0f
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        when (spectrogramViewState) {
            LIVE_DISPLAY, RECORDING_DATA -> when (event?.action) {
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

            PLAYBACK                     -> {
                if (context !is PlaybackModeControlListener) throw Error("SpectrogramView can only be used within Activities that implement PlaybackModeControlListener")
                val controller = (context as PlaybackModeControlListener)
                when (event?.action) {
                    ACTION_DOWN -> {
                        controller.playbackTouched()
                        touchedAtXpos = event.x
                        return true
                    }

                    ACTION_MOVE -> {
                        controller.playbackSeekTo((event.x - touchedAtXpos) / getDrawAreaWidth())

                    }

                    ACTION_UP   -> {
                        controller.playbackReleased()
                    }

                }
            }
        }
        return super.onTouchEvent(event)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)

        clearBitmapAndBuffer()
        //TODO repaint bitmap
    }

    fun clearBitmapAndBuffer() {
        mBitmap = Bitmap.createBitmap((width - left_margin - right_margin).toInt(), (height - top_margin - bottom_margin).toInt(), Bitmap.Config.ARGB_8888)
        mCanvas = Canvas(mBitmap)
        buffer = IntArray((getDrawAreaHeight() * getDrawAreaWidth()).toInt())
    }


}
