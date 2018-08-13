package de.voicegym.voicegym.recordActivity.views

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.Typeface
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
import java.lang.Thread.sleep
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.concurrent.thread
import kotlin.math.absoluteValue
import kotlin.math.roundToInt

class SpectrogramView : SurfaceView, InstrumentViewInterface, Runnable {

    /**
     * Which color to pick for the decorations
     */
    private var decorationColor = Color.GRAY

    /**
     * The thickness of drawn lines
     */
    private var drawLineThickness = 2f


    /**
     * Holds the ColorPicker that is used to select the appropriate color for the spectral intensity
     */
    var intensityMap: GradientPicker = HotGradientColorPicker


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
        paint.color = decorationColor
        paint.isAntiAlias = true
        paint.isDither = true
        paint.strokeJoin = Paint.Join.ROUND
        paint.strokeCap = Paint.Cap.ROUND
        paint.strokeWidth = drawLineThickness * resources.displayMetrics.density
        paint.style = Paint.Style.FILL
        paint.textSize = 16f * resources.displayMetrics.density
        paint.typeface = Typeface.SANS_SERIF
        decorationSettings = SpectrogramViewDecorationSettings(paint, SpectrogramViewPaintArea(0f, width.toFloat(), height.toFloat(), 0f))

    }

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) : super(context, attrs, defStyleAttr) {
        attrs?.let {
            val styleableAttributes = context.theme.obtainStyledAttributes(it, R.styleable.SpectrogramView, defStyleAttr, 0)
            drawLineThickness = styleableAttributes.getFloat(R.styleable.SpectrogramView_border_thickness, 2f)
            intensityMap = when (styleableAttributes.getInteger(R.styleable.SpectrogramView_color_map, 0)) {
                0    -> HotGradientColorPicker
                else -> HotGradientColorPicker
            }


        }
    }

    override fun onWindowFocusChanged(hasWindowFocus: Boolean) {
        interruptRendering {
            super.onWindowFocusChanged(hasWindowFocus)
            updateScaling()
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        interruptRendering {
            super.onSizeChanged(w, h, oldw, oldh)

            clearBitmapAndBuffer()
            updateScaling()
            updateDecorationSettings()
            //TODO VG-61 repaint bitmap
        }
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        interruptRendering {
            super.onLayout(changed, left, top, right, bottom)
            updateScaling()
            updateDecorationSettings()
        }
    }

    override fun onVisibilityChanged(changedView: View?, visibility: Int) {
        interruptRendering {
            super.onVisibilityChanged(changedView, visibility)
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
                        controller.playbackSeekTo((event.x - touchedAtXpos) / width)
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

    private fun getPixelColorArray(amplitudes: DoubleArray): IntArray {
        val colors = IntArray(height)
        for (i in 0 until colors.size) colors[i] = pickColor(height - i, amplitudes)
        return colors
    }

    private fun pickColor(pixelPosition: Int, amplitudes: DoubleArray): Int {
        return intensityMap.pickColor(getDezibelFromAmplitude(amplitudes[indexArray[pixelPosition - 1]]) / SettingsBundle.normalisationConstant)
    }

    private fun drawSpectrogramBar(colors: IntArray, x: Float, width: Float) {
        colors.forEachIndexed { i, colorValue ->
            mPaint.color = colorValue
            mCanvas?.drawLine(x, (height - i).toFloat(), x + width, (height - i).toFloat(), mPaint)
        }
    }

    private fun pixelPerDatapoint() = width / settings.displayedDatapoints

    fun addLeft(amplitudes: DoubleArray, immediateDraw: Boolean) {
        renderQueue.add(RenderJob(amplitudes, immediateDraw, AddPosition.LEFT))
    }

    fun addRight(amplitudes: DoubleArray, immediateDraw: Boolean) {
        renderQueue.add(RenderJob(amplitudes, immediateDraw, AddPosition.RIGHT))
    }


    private fun drawFrequencyLine(canvas: Canvas?) {
        canvas?.let {
            it.drawLine(0f, yPosLine, width.toFloat(), yPosLine, decorationSettings.paint)
            val frequency = scale.valueFromPixel(yPosLine.roundToInt())
            drawText("${frequency.toInt()} Hz", 0f, yPosLine, it)
        }
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
                decorationSettings.drawArea.right + drawLineThickness,
                decorationSettings.drawArea.bottom,
                decorationSettings.drawArea.right + drawLineThickness,
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
                decorationSettings.drawArea.right + drawLineThickness,
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
            x - distanceFromPosition - rect.right + rect.left - 2 * decorationSettings.paint.strokeWidth
        }

        canvas.drawText(text, xPos, yPos, decorationSettings.paint)
    }


    private fun addLeftForBackgroundThread(amplitudes: DoubleArray, immediateDraw: Boolean) {
        if (indexArray.isNotEmpty()) {
            rotateBitmapForBackgroundThread(-pixelPerDatapoint())
            val colors = getPixelColorArray(amplitudes)
            val xEnd = width.toFloat() / settings.displayedDatapoints
            drawSpectrogramBar(colors, 0f, xEnd)
        }
        if (immediateDraw) drawForBackgroundThread()
    }

    private fun addRightForBackgroundThread(amplitudes: DoubleArray, immediateDraw: Boolean) {
        if (indexArray.isNotEmpty()) {
            rotateBitmapForBackgroundThread(pixelPerDatapoint())
            val colors = getPixelColorArray(amplitudes)
            val xEnd = width.toFloat() / settings.displayedDatapoints
            val xStart = xEnd * (settings.displayedDatapoints - 1)
            drawSpectrogramBar(colors, xStart, xEnd)
        }
        if (immediateDraw) drawForBackgroundThread()
    }

    private fun rotateBitmapForBackgroundThread(numberOfPixels: Int) {
        buffer?.let {
            // Rolling through pixels
            val cutSpaceLeft = if (numberOfPixels >= 0) numberOfPixels else 0
            val cutSpaceRight = if (numberOfPixels < 0) -numberOfPixels else 0
            mBitmap?.getPixels(it, 0, width, cutSpaceLeft, 0, width - numberOfPixels.absoluteValue, height)
            mBitmap?.setPixels(it, 0, width, cutSpaceRight, 0, width - numberOfPixels.absoluteValue, height)
        }
    }

    private fun drawForBackgroundThread() {
        if (holder.surface.isValid && mBitmap != null) {
            val canvas = holder.surface.lockCanvas(null)
            synchronized(mBitmap!!) {
                canvas.let { canvas ->
                    canvas.drawBitmap(mBitmap, 0f, 0f, mPaint)
                    canvas.drawPath(mPath, mPaint)
                    // Draw Instrument Tools
                    if (drawLine) drawFrequencyLine(canvas)
                    if (settings.drawScale) drawScale(canvas)
                }
            }
            holder.surface.unlockCanvasAndPost(canvas)
        }
    }


    fun clearBitmapAndBuffer() {
        mBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        mBitmap?.setHasAlpha(false)
        mCanvas = Canvas(mBitmap)
        buffer = IntArray(height * width)
    }

    override fun updateInstrumentViewSettings(settings: FourierInstrumentViewSettings) {
        interruptRendering {
            this.settings = settings
            updateScaling()
            updateDecorationSettings()
        }
    }


    /**
     * This array contains the frequencies corresponding to the y-position of the pixel on the drawn bitmap
     */
    private var indexArray = IntArray(0)

    /**
     * This function updates the indexArray when any settings or the screen layout have changed
     */
    private fun updateScaling() {
        if (height > 0) {
            val from = PixelFrequencyPair(height, settings.fromFrequency)
            val until = PixelFrequencyPair(0, settings.tillFrequency)
            scale = if (settings.isLogarithmic) {
                ExponentialScalingFunction(from, until)
            } else {
                LinearScalingFunction(from, until)
            }
            indexArray = IntArray(height)
            for (i in 0 until indexArray.size) indexArray[i] = indexOfFrequency(scale.valueFromPixel(i))
        }
    }

    /**
     * Render Thread that is running in the background
     */
    private var renderThread: Thread? = null

    /**
     * The Queue that holds the RenderJobs that have been given us by another thread
     */
    private val renderQueue = ConcurrentLinkedQueue<RenderJob>()

    /**
     * control variable for the renderThread
     */
    private var rendering: Boolean = false

    fun isReady(): Boolean {
        return !renderThreadBusy
    }

    private var renderThreadBusy = false
    /**
     * Our Render Loop
     */
    override fun run() {
        setThreadPriority(THREAD_PRIORITY_URGENT_DISPLAY)
        var needsRendering = false

        while (rendering) {
            when {
                visibility == View.INVISIBLE -> sleep(20)

                renderQueue.isEmpty()        -> {
                    if (needsRendering) {
                        drawForBackgroundThread()
                        needsRendering = false
                    }
                    renderThreadBusy = false
                    sleep(10)
                }

                else                         -> {
                    renderThreadBusy = true
                    val job = renderQueue.poll()
                    // if the last added things didn't needed to be drawn immediately but we now hit a immediateDrawJob
                    if (needsRendering && job.renderImmediately) {
                        drawForBackgroundThread()
                        needsRendering = false
                    } else {
                        needsRendering = !job.renderImmediately
                    }

                    when (job.position) {
                        AddPosition.LEFT  -> addLeftForBackgroundThread(job.amplitudes, job.renderImmediately)
                        AddPosition.RIGHT -> addRightForBackgroundThread(job.amplitudes, job.renderImmediately)
                    }
                }
            }
        }
    }

    /**
     * If the rendering Thread is not yet running this will start the thread
     */
    fun startRendering() {
        Log.i("RenderThread", "startRendering() called")
        if (!rendering && renderThread == null) {
            rendering = true
            renderThread = thread(start = true) { this.run() }
        }
    }

    /**
     * This function stops the rendering Thread if it is running and ensures that block
     * is invoked without an active renderThread
     *
     * @param block: the code to be executed with no active rendering
     */
    private fun interruptRendering(block: () -> Unit) {
        val rendering = renderThread?.isAlive ?: false
        if (rendering) stopRendering()
        block.invoke()
        if (rendering) startRendering()
    }

    /**
     * This function stops the renderThread and locks the calling thread until renderThread has finished.
     */
    fun stopRendering() {
        Log.i("RenderThread", "stopRendering() called")
        if (rendering && renderThread != null) {
            rendering = false
            renderThread?.join()
            renderThread = null
        }
    }
}

data class RenderJob(val amplitudes: DoubleArray, val renderImmediately: Boolean, val position: AddPosition)

enum class AddPosition { LEFT,
    RIGHT
}
