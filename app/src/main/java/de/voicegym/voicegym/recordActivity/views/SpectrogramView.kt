package de.voicegym.voicegym.recordActivity.views

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.Typeface
import android.os.Process.THREAD_PRIORITY_DISPLAY
import android.os.Process.THREAD_PRIORITY_URGENT_DISPLAY
import android.os.Process.setThreadPriority
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.MotionEvent.ACTION_DOWN
import android.view.MotionEvent.ACTION_MOVE
import android.view.MotionEvent.ACTION_UP
import android.view.SurfaceView
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
import java.lang.Thread.sleep
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.concurrent.thread
import kotlin.math.absoluteValue
import kotlin.math.roundToInt

//TODO VG-67 Bitmap operationen in den Background verlagern
class SpectrogramView : SurfaceView, InstrumentViewInterface, Runnable {

    private var renderThread: Thread? = null
    private val renderQueue = ConcurrentLinkedQueue<RenderJob>()
    private var rendering: Boolean = false

    override fun run() {
        setThreadPriority(THREAD_PRIORITY_URGENT_DISPLAY)
        var needsRendering = false

        while (rendering) {
            if (renderQueue.isEmpty()) {
                if (needsRendering) {
                    drawOnBackgroundThread()
                    needsRendering = false
                }
                sleep(10)
            } else {
                val job = renderQueue.poll()
                // if the last added things didn't needed to be drawn immediately but we now hit a immediateDrawJob
                if (needsRendering && job.renderImmediately) {
                    drawOnBackgroundThread()
                    needsRendering = false
                } else {
                    needsRendering = !job.renderImmediately
                }

                when (job.position) {
                    AddPosition.LEFT  -> addLeftRenderThread(job.amplitudes, job.renderImmediately)
                    AddPosition.RIGHT -> addRightRenderThread(job.amplitudes, job.renderImmediately)
                }
            }
        }
    }

    fun startRendering() {
        Log.i("RenderThread", "startRendering() called")
        if (!rendering && renderThread == null) {
            rendering = true
            renderThread = thread(start = true) { this.run() }
            Log.i("RenderThread", "renderThread instantiated")
        }
    }

    fun stopRendering() {
        Log.i("RenderThread", "stopRendering() called")
        if (rendering && renderThread != null) {
            rendering = false
            renderThread?.join()
            renderThread = null
        }
    }

    /**
     *  The instrument is drawn below the topMargin
     */
    var topMargin: Float = 20f

    /**
     *  The instrument is drawn above the above_margin
     */
    var bottomMargin: Float = 20f

    /**
     *  The instrument is drawn right to the leftMargin
     */
    var leftMargin: Float = 50f

    /**
     * The instrument is drawn left to the rightMargin
     */
    var rightMargin: Float = 20f

    /**
     * Whether to draw a border around the instrument area
     */
    var drawBorder: Boolean = true

    /**
     * Which color to pick for the border
     */
    var borderColor = Color.GRAY

    /**
     * The thickness of the border
     */
    var borderThickness = 2f


    /**
     * Holds the ColorPicker that is used to select the appropriate color for the spectral intensity
     */
    var intensityMap: GradientPicker = HotGradientColorPicker

    private fun getDrawAreaWidth(): Float = (width - leftMargin - rightMargin)

    private fun getDrawAreaHeight(): Float = (height - topMargin - bottomMargin)


    private var settings: FourierInstrumentViewSettings = FourierInstrumentViewSettings(4096, 2, 10.0, 1000.0, 100, false, false)

    private var scale: ScalingFunction = LinearScalingFunction(PixelFrequencyPair(10, 10.0), PixelFrequencyPair(200, 1000.0))


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
        paint.color = borderColor
        paint.isAntiAlias = true
        paint.isDither = true
        paint.strokeJoin = Paint.Join.ROUND
        paint.strokeCap = Paint.Cap.ROUND
        paint.strokeWidth = borderThickness * resources.displayMetrics.density
        paint.style = Paint.Style.FILL
        paint.textSize = 16f * resources.displayMetrics.density
        paint.typeface = Typeface.SANS_SERIF
        decorationSettings = SpectrogramViewDecorationSettings(paint, SpectrogramViewPaintArea(leftMargin, width - rightMargin, height - bottomMargin, topMargin))

    }

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) : super(context, attrs, defStyleAttr) {
        attrs?.let {
            val styleableAttributes = context.theme.obtainStyledAttributes(it, R.styleable.SpectrogramView, defStyleAttr, 0)
            topMargin = styleableAttributes.getFloat(R.styleable.SpectrogramView_top_margin, 20f)
            bottomMargin = styleableAttributes.getFloat(R.styleable.SpectrogramView_bottom_margin, 20f)
            leftMargin = styleableAttributes.getFloat(R.styleable.SpectrogramView_left_margin, 50f)
            rightMargin = styleableAttributes.getFloat(R.styleable.SpectrogramView_right_margin, 20f)
            drawBorder = styleableAttributes.getBoolean(R.styleable.SpectrogramView_draw_border, true)
            borderThickness = styleableAttributes.getFloat(R.styleable.SpectrogramView_border_thickness, 2f)
            intensityMap = when (styleableAttributes.getInteger(R.styleable.SpectrogramView_color_map, 0)) {
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
        for (i in 0 until colors.size) colors[i] = pickColor((getDrawAreaHeight() + topMargin).toInt() - i, amplitudes)
        return colors
    }

    private fun pickColor(pixelPosition: Int, amplitudes: DoubleArray): Int {
        return intensityMap.pickColor(getDezibelFromAmplitude(amplitudes[indexArray[pixelPosition - topMargin.toInt() - 1]]) / SettingsBundle.normalisationConstant)
    }

    private fun drawSpectrogramBar(colors: IntArray, x: Float, width: Float) {
        val lowerY = getDrawAreaHeight()
        colors.forEachIndexed { i, colorValue ->
            mPaint.color = colorValue
            mCanvas?.drawLine(x, lowerY - i, x + width, lowerY - i, mPaint)
        }
    }

    private fun pixelPerDatapoint() = (getDrawAreaWidth() / settings.displayedDatapoints).toInt()

    fun addLeft(amplitudes: DoubleArray, immediateDraw: Boolean) {
        renderQueue.add(RenderJob(amplitudes, immediateDraw, AddPosition.LEFT))
    }

    fun addRight(amplitudes: DoubleArray, immediateDraw: Boolean) {
        renderQueue.add(RenderJob(amplitudes, immediateDraw, AddPosition.RIGHT))
    }

    private fun addLeftRenderThread(amplitudes: DoubleArray, immediateDraw: Boolean) {
        if (indexArray.isNotEmpty()) {
            rotateBitmap(-pixelPerDatapoint())
            val colors = getPixelColorArray(amplitudes)
            val width = getDrawAreaWidth() / settings.displayedDatapoints
            drawSpectrogramBar(colors, 0f, width)
        }
        if (immediateDraw) drawOnBackgroundThread()
    }

    private fun addRightRenderThread(amplitudes: DoubleArray, immediateDraw: Boolean) {
        if (indexArray.isNotEmpty()) {
            rotateBitmap(pixelPerDatapoint())
            val colors = getPixelColorArray(amplitudes)
            val width = getDrawAreaWidth() / settings.displayedDatapoints
            val x = width * (settings.displayedDatapoints - 1)
            drawSpectrogramBar(colors, x, width)
        }
        if (immediateDraw) drawOnBackgroundThread()
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
            it.drawLine(leftMargin, yPosLine, getDrawAreaWidth() + leftMargin, yPosLine, decorationSettings.paint)
            val frequency = scale.valueFromPixel(yPosLine.roundToInt())
            drawText("${frequency.toInt()} Hz", leftMargin, yPosLine, it)
        }
    }

    private fun drawBorder(canvas: Canvas) {
        val bottom = (height - bottomMargin + borderThickness)
        val right = (width - rightMargin + borderThickness)
        canvas.drawRect(decorationSettings.drawArea.left - borderThickness,
                decorationSettings.drawArea.top - borderThickness,
                decorationSettings.drawArea.right + borderThickness,
                bottom,
                decorationSettings.paint)
    }


    private fun getTicks(): List<Tick> {
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
                decorationSettings.drawArea.right + borderThickness,
                decorationSettings.drawArea.bottom,
                decorationSettings.drawArea.right + borderThickness,
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
                decorationSettings.drawArea.right + borderThickness,
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
            x - distanceFromPosition - rect.right + rect.left - rightMargin - 2 * decorationSettings.paint.strokeWidth
        }

        canvas?.drawText(text, xPos, yPos, decorationSettings.paint)
    }

    // TODO REMOVE??
    private var mayDraw = false

    private fun drawOnBackgroundThread() {
        if (mayDraw && holder.surface.isValid && mBitmap != null) {
            val canvas = holder.surface.lockCanvas(null)
            synchronized(mBitmap!!) {
                canvas.let { canvas ->
                    canvas.drawBitmap(mBitmap, leftMargin, topMargin, mPaint)
                    canvas.drawPath(mPath, mPaint)
                    // Draw Instrument Tools
                    if (drawBorder) drawBorder(canvas)
                    if (drawLine) drawFrequencyLine(canvas)
                    if (settings.drawScale) drawScale(canvas)
                }
            }
            holder.surface.unlockCanvasAndPost(canvas)
        }
    }

    //TODO remove?
    override fun onVisibilityChanged(changedView: View?, visibility: Int) {
        super.onVisibilityChanged(changedView, visibility)
        mayDraw = when (visibility) {
            View.VISIBLE -> true
            else         -> false

        }
    }

    override fun onDraw(canvas: Canvas?) {
        backgroundColor = Color.BLACK
        super.onDraw(canvas)
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
        mBitmap = Bitmap.createBitmap((width - leftMargin - rightMargin).toInt(), (height - topMargin - bottomMargin).toInt(), Bitmap.Config.ARGB_8888)
        mBitmap?.setHasAlpha(false)
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
            val from = PixelFrequencyPair((getDrawAreaHeight() + topMargin).toInt(), settings.fromFrequency)
            val until = PixelFrequencyPair((topMargin).toInt(), settings.tillFrequency)
            scale = if (settings.isLogarithmic) {
                ExponentialScalingFunction(from, until)
            } else {
                LinearScalingFunction(from, until)
            }
            indexArray = IntArray(getDrawAreaHeight().toInt())
            for (i in 0 until indexArray.size) indexArray[i] = indexOfFrequency(scale.valueFromPixel(topMargin.toInt() + i))
        }
    }
}


data class RenderJob(val amplitudes: DoubleArray, val renderImmediately: Boolean, val position: AddPosition)

enum class AddPosition { LEFT,
    RIGHT
}
