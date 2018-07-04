package de.voicegym.voicegym.recordActivity.views

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.MotionEvent.ACTION_DOWN
import android.view.MotionEvent.ACTION_MOVE
import android.view.MotionEvent.ACTION_UP
import android.view.View
import de.voicegym.voicegym.R
import de.voicegym.voicegym.menu.settings.FourierInstrumentViewSettings
import de.voicegym.voicegym.menu.settings.SettingsBundle
import de.voicegym.voicegym.recordActivity.fragments.InstrumentState.LIVE_DISPLAY
import de.voicegym.voicegym.recordActivity.fragments.InstrumentState.PLAYBACK
import de.voicegym.voicegym.recordActivity.fragments.InstrumentState.RECORDING_DATA
import de.voicegym.voicegym.recordActivity.fragments.InstrumentViewInterface
import de.voicegym.voicegym.recordActivity.fragments.PlaybackModeControlListener
import de.voicegym.voicegym.recordActivity.views.util.*
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
    var border_thickness = 2f


    /**
     * Holds the ColorPicker that is used to select the appropriate color for the spectral intensity
     */
    var intensity_map: GradientPicker = HotGradientColorPicker

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

    private var decorationSettings: SpectrogramViewDecorationSettings

    init {
        // Set up the paint with which to draw.
        mPaint.color = Color.BLACK
        mPaint.isAntiAlias = true
        mPaint.isDither = true
        mPaint.style = Paint.Style.STROKE
        mPaint.strokeJoin = Paint.Join.ROUND
        mPaint.strokeCap = Paint.Cap.ROUND
        mPaint.strokeWidth = 1f
        decorationSettings = SpectrogramViewDecorationSettings(mPaint, SpectrogramViewPaintArea(0f, 0f, 0f, 0f))

    }

    private fun updateDecorationSettings() {
        val paint = Paint()
        paint.color = border_color
        paint.isAntiAlias = true
        paint.isDither = true
        paint.strokeJoin = Paint.Join.ROUND
        paint.strokeCap = Paint.Cap.ROUND
        paint.strokeWidth = border_thickness * resources.displayMetrics.density
        paint.style = Paint.Style.FILL
        paint.textSize = 16f * resources.displayMetrics.density
        paint.typeface = Typeface.SANS_SERIF
        decorationSettings = SpectrogramViewDecorationSettings(paint, SpectrogramViewPaintArea(left_margin, width - right_margin, height - bottom_margin, top_margin))

    }

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) : super(context, attrs, defStyleAttr) {
        attrs?.let {
            val styleableAttributes = context.theme.obtainStyledAttributes(it, R.styleable.SpectrogramView, defStyleAttr, 0)
            top_margin = styleableAttributes.getFloat(R.styleable.SpectrogramView_top_margin, 20f)
            bottom_margin = styleableAttributes.getFloat(R.styleable.SpectrogramView_bottom_margin, 20f)
            left_margin = styleableAttributes.getFloat(R.styleable.SpectrogramView_left_margin, 50f)
            right_margin = styleableAttributes.getFloat(R.styleable.SpectrogramView_right_margin, 20f)
            draw_border = styleableAttributes.getBoolean(R.styleable.SpectrogramView_draw_border, true)
            border_thickness = styleableAttributes.getFloat(R.styleable.SpectrogramView_border_thickness, 2f)
            intensity_map = when (styleableAttributes.getInteger(R.styleable.SpectrogramView_color_map, 0)) {
                0    -> HotGradientColorPicker
                else -> HotGradientColorPicker
            }


        }
    }


    var frequencyArray: DoubleArray? = null

    private fun indexOfFrequency(frequency: Double): Int {
        frequencyArray?.let { frequencies ->
            for (i in 0 until frequencies.size - 1) {
                if ((frequencies[i] <= frequency) && frequencies[i + 1] > frequency) return i
            }
            return if (frequencies[0] < frequency) 0 else frequencies.size - 1 // frequency was not in range
        }
        throw Error("FrequencyArray was not set before calling index function")
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)

        updateScaling()
        updateDecorationSettings()
    }


    private fun getPixelColorArray(amplitudes: DoubleArray): IntArray {
        val colors = IntArray(getDrawAreaHeight().toInt())
        for (i in 0 until colors.size) colors[i] = pickColor((getDrawAreaHeight() + top_margin).toInt() - i, amplitudes)
        return colors
    }

    private fun pickColor(pixelPosition: Int, amplitudes: DoubleArray): Int {
        return intensity_map.pickColor(getDezibelFromAmplitude(amplitudes[indexArray[pixelPosition - top_margin.toInt() - 1]]) / SettingsBundle.normalisationConstant)
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
        if (indexArray.isNotEmpty()) {
            rotateBitmap(-pixelPerDatapoint())
            val colors = getPixelColorArray(amplitudes)
            val width = getDrawAreaWidth() / settings.displayedDatapoints
            drawSpectrogramBar(colors, 0f, width)
        }
    }

    fun addRight(amplitudes: DoubleArray) {
        if (indexArray.isNotEmpty()) {
            rotateBitmap(pixelPerDatapoint())
            val colors = getPixelColorArray(amplitudes)
            val width = getDrawAreaWidth() / settings.displayedDatapoints
            val x = width * (settings.displayedDatapoints - 1)
            drawSpectrogramBar(colors, x, width)
        }
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
        canvas?.let {
            it.drawLine(left_margin, yPosLine, getDrawAreaWidth() + left_margin, yPosLine, decorationSettings.paint)
            val frequency = scale.valueFromPixel(yPosLine.roundToInt())
            drawText("${frequency.toInt()} Hz", left_margin, yPosLine, it)
        }
    }

    private fun drawBorder(canvas: Canvas) {
        val bottom = (height - bottom_margin + border_thickness)
        val right = (width - right_margin + border_thickness)
        canvas.drawRect(decorationSettings.drawArea.left - border_thickness,
                decorationSettings.drawArea.top - border_thickness,
                decorationSettings.drawArea.right + border_thickness,
                bottom,
                decorationSettings.paint)
    }


    private fun getTicks(): ArrayList<Tick> {
        when (scale) {
            is LinearScalingFunction      -> {
                return getLinearTicklist(scale.from.correspondingFrequency, scale.until.correspondingFrequency)
            }

            is ExponentialScalingFunction -> {
                return getExponentialTicklist(scale.from.correspondingFrequency, scale.until.correspondingFrequency)
            }

            else                          -> {
                throw Error("Unknown Scaling, extend the getHiLoTick Function")
            }
        }

    }


    private fun drawScale(canvas: Canvas) {
        canvas.drawLine(
                decorationSettings.drawArea.right + border_thickness,
                decorationSettings.drawArea.bottom,
                decorationSettings.drawArea.right + border_thickness,
                decorationSettings.drawArea.top,
                decorationSettings.paint)
        val ticklist = getTicks().sortedWith(compareBy { it.value })
        drawTick(ticklist.last().hasLabel, "${ticklist.last().value.toInt()} Hz", false, scale.valueFromFrequency(ticklist.last().value).toFloat(), canvas)
        ticklist.dropLast(1)
        ticklist.forEach { tick ->
            drawTick(tick.hasLabel, "${tick.value.toInt()} Hz", true, scale.valueFromFrequency(tick.value).toFloat(), canvas)
        }

    }


    private fun drawTick(drawText: Boolean, text: String, aboveLine: Boolean, position: Float, canvas: Canvas) {
        val tickLength = if (drawText) {
            (decorationSettings.drawArea.right - decorationSettings.drawArea.left) * 0.05f
        } else {
            (decorationSettings.drawArea.right - decorationSettings.drawArea.left) * 0.02f
        }
        canvas.drawLine(
                decorationSettings.drawArea.right - tickLength,
                position,
                decorationSettings.drawArea.right + border_thickness,
                position,
                decorationSettings.paint)
        if (drawText) drawText(text, decorationSettings.drawArea.right, position, aboveLine, false, canvas)
    }

    private fun drawText(text: String, x: Float, y: Float, canvas: Canvas) = drawText(text, x, y, true, true, canvas)

    private fun drawText(text: String, x: Float, y: Float, above: Boolean, right: Boolean, canvas: Canvas) {
        val distanceFromPosition = 5 * resources.displayMetrics.density
        val yPos = if (above) {
            y - distanceFromPosition
        } else {
            val rect = Rect()
            decorationSettings.paint.getTextBounds(text, 0, text.length - 1, rect)
            y + distanceFromPosition + rect.bottom - rect.top
        }

        val xPos = if (right) {
            x + distanceFromPosition
        } else {
            val rect = Rect()
            decorationSettings.paint.getTextBounds(text, 0, text.length - 1, rect)
            x - distanceFromPosition - rect.right + rect.left - right_margin - 2 * decorationSettings.paint.strokeWidth
        }

        canvas?.drawText(text, xPos, yPos, decorationSettings.paint)
    }

    override fun onDraw(canvas: Canvas?) {
        backgroundColor = Color.BLACK
        super.onDraw(canvas)

        canvas?.let { canvas ->
            canvas.drawBitmap(mBitmap, left_margin, top_margin, mPaint)
            canvas.drawPath(mPath, mPaint)
            // Draw Instrument Tools
            if (draw_border) drawBorder(canvas)
            if (drawLine) drawFrequencyLine(canvas)
            if (settings.drawScale) drawScale(canvas)
        }
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
        updateDecorationSettings()
    }

    override fun onWindowFocusChanged(hasWindowFocus: Boolean) {
        super.onWindowFocusChanged(hasWindowFocus)

        updateScaling()
    }

    var indexArray = IntArray(0)

    private fun updateScaling() {
        if (height > 0) {
            val from = PixelFrequencyPair((getDrawAreaHeight() + top_margin).toInt(), settings.fromFrequency)
            val until = PixelFrequencyPair((top_margin).toInt(), settings.tillFrequency)
            scale = if (settings.isLogarithmic) {
                ExponentialScalingFunction(from, until)
            } else {
                LinearScalingFunction(from, until)
            }
            indexArray = IntArray(getDrawAreaHeight().toInt())
            for (i in 0 until indexArray.size) indexArray[i] = indexOfFrequency(scale.valueFromPixel(top_margin.toInt() + i))
        }
    }
}

