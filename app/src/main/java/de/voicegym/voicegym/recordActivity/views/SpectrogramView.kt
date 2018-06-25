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
import de.voicegym.voicegym.menu.settings.SettingsBundle
import de.voicegym.voicegym.recordActivity.fragments.InstrumentState.LIVE_DISPLAY
import de.voicegym.voicegym.recordActivity.fragments.InstrumentState.PLAYBACK
import de.voicegym.voicegym.recordActivity.fragments.InstrumentState.RECORDING_DATA
import de.voicegym.voicegym.recordActivity.fragments.InstrumentViewInterface
import de.voicegym.voicegym.recordActivity.fragments.PlaybackModeControlListener
import de.voicegym.voicegym.recordActivity.views.util.ExponentialScalingFunction
import de.voicegym.voicegym.recordActivity.views.util.HotGradientColorPicker
import de.voicegym.voicegym.recordActivity.views.util.LinearScalingFunction
import de.voicegym.voicegym.recordActivity.views.util.PixelFrequencyPair
import de.voicegym.voicegym.recordActivity.views.util.ScalingFunction
import de.voicegym.voicegym.util.audio.getDezibelFromAmplitude
import org.jetbrains.anko.backgroundColor
import kotlin.math.absoluteValue
import kotlin.math.roundToInt

class SpectrogramView : View, InstrumentViewInterface {

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

    private fun getDrawAreaWidth(): Float = (width - left_margin - right_margin)

    private fun getDrawAreaHeight(): Float = (height - top_margin - bottom_margin)


    private lateinit var settings: FourierInstrumentViewSettings

    private lateinit var scale: ScalingFunction


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

    var frequencyArray: DoubleArray? = null

    private fun indexOfFrequency(frequency: Double): Int {
        frequencyArray?.let { frequencies ->
            for (i in 0 until frequencies.size - 1) {
                if ((frequencies[i] <= frequency) && frequencies[i + 1] > frequency) return i
            }
            if (frequencies[0] < frequency) return 0 else return frequencies.size - 1 // frequency was not in range
        }
        throw Error("FrequencyArray was not set before calling index function")
    }


    private fun getPixelColorArray(amplitudes: DoubleArray): IntArray {
        val colors = IntArray(getDrawAreaHeight().toInt())
        for (i in 0 until colors.size) colors[i] = pickColor((getDrawAreaHeight() + top_margin).toInt() - i, amplitudes)
        return colors
    }

    private fun pickColor(pixelPosition: Int, amplitudes: DoubleArray): Int {
        val frequency = scale.valueFromPixel(pixelPosition)
        return HotGradientColorPicker.pickColor(getDezibelFromAmplitude(amplitudes[indexOfFrequency(frequency)]) / SettingsBundle.normalisationConstant)
    }

    private fun drawSpectrogramBar(colors: IntArray, x: Float, width: Float) {
        val lowerY = getDrawAreaHeight()
        colors.forEachIndexed { i, colorValue ->
            mPaint.color = colorValue
            mCanvas?.drawLine(x, lowerY - i, x + width, lowerY - i, mPaint)
        }
    }

    private fun pixelPerDatapoint() = (getDrawAreaWidth() / settings.displayedDatapoints).toInt()

    fun addLeft(amplitudes: DoubleArray) {
        rotateBitmap(-pixelPerDatapoint())
        val colors = getPixelColorArray(amplitudes)
        val width = getDrawAreaWidth() / settings.displayedDatapoints
        drawSpectrogramBar(colors, 0f, width)
    }

    fun addRight(amplitudes: DoubleArray) {
        rotateBitmap(pixelPerDatapoint())
        val colors = getPixelColorArray(amplitudes)
        val width = getDrawAreaWidth() / settings.displayedDatapoints
        val x = width * (settings.displayedDatapoints - 1)
        drawSpectrogramBar(colors, x, width)
    }


    private fun rotateBitmap(numberOfPixels: Int) {
        buffer?.let {
            // Rolling through pixels
            val cutSpaceLeft = if (numberOfPixels >= 0) numberOfPixels else 0
            val cutSpaceRight = if (numberOfPixels < 0) -numberOfPixels else 0
            mBitmap?.getPixels(it, 0, getDrawAreaWidth().toInt(), cutSpaceLeft, 0, getDrawAreaWidth().toInt() - numberOfPixels.absoluteValue, getDrawAreaHeight().toInt())
            mBitmap?.setPixels(it, 0, getDrawAreaWidth().toInt(), cutSpaceRight, 0, getDrawAreaWidth().toInt() - numberOfPixels.absoluteValue, getDrawAreaHeight().toInt())
        }
    }


    private fun drawFrequencyLine(canvas: Canvas?) {
        val width = getDrawAreaWidth()
        mPaint.color = Color.GRAY
        mPaint.strokeWidth = 2f
        mPaint.textSize = 24f
        canvas?.drawLine(left_margin, yPosLine, width + left_margin, yPosLine, mPaint)
        val frequency = scale.valueFromPixel(yPosLine.roundToInt())
        canvas?.drawText("${frequency.toInt()} Hz", left_margin + 5, yPosLine - 5, mPaint)
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

    var spectrogramViewState = LIVE_DISPLAY

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        when (spectrogramViewState) {
            LIVE_DISPLAY, RECORDING_DATA -> {
                when (event?.action) {
                    ACTION_DOWN -> {
                        drawLine = !drawLine
                        yPosLine = event.y

                    }

                    ACTION_MOVE -> yPosLine = event.y
                    ACTION_UP   -> yPosLine = event.y
                }
                this.invalidate()
            }

            PLAYBACK                     -> {
                if (context !is PlaybackModeControlListener) throw Error("SpectrogramView can only be used within Activities that implement PlaybackModeControlListener")
                val controller = (context as PlaybackModeControlListener)
                when (event?.action) {
                    ACTION_DOWN -> {
                        controller.playbackTouched()
                        touchedAtXpos = event.x
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


        return when (event?.action) {
            ACTION_UP, ACTION_DOWN, ACTION_MOVE -> true
            else                                -> super.onTouchEvent(event)
        }
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

    override fun updateInstrumentViewSettings(settings: FourierInstrumentViewSettings) {
        this.settings = settings
        updateScaling()
    }


    override fun onWindowFocusChanged(hasWindowFocus: Boolean) {
        super.onWindowFocusChanged(hasWindowFocus)

        updateScaling()
    }


    fun updateScaling() {
        val from = PixelFrequencyPair((getDrawAreaHeight() + top_margin).toInt(), settings.fromFrequency)
        val until = PixelFrequencyPair((top_margin).toInt(), settings.tillFrequency)
        scale = if (settings.isLogarithmic) {
            ExponentialScalingFunction(from, until)
        } else {
            LinearScalingFunction(from, until)
        }
    }


}
