package de.voicegym.voicegym.views

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View
import org.jetbrains.anko.backgroundColor

abstract class InstrumentView : View {

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) : super(context, attrs, defStyleAttr)

    var mCanvas: Canvas? = null
    var mBitmap: Bitmap? = null
    var mPath = Path()
    var mPaint = Paint()
    var buffer: IntArray? = null

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

    var fromY: Double? = null

    var untilY: Double? = null

    /**
     * needs to be smaller or equal the number of pixels available
     */
    var xDataPoints: Int? = null

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
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)

        mBitmap = Bitmap.createBitmap((width - left_margin - right_margin).toInt(), (height - top_margin - bottom_margin).toInt(), Bitmap.Config.ARGB_8888)
        mCanvas = Canvas(mBitmap)
        buffer = IntArray((getDrawAreaHeight() * getDrawAreaWidth()).toInt())
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

    fun moveBitmap(numberOfPixels: Int) {
        if (buffer != null) {
            mBitmap?.getPixels(buffer, 0, getDrawAreaWidth().toInt(), numberOfPixels, 0, getDrawAreaWidth().toInt() - numberOfPixels, getDrawAreaHeight().toInt())
            mBitmap?.setPixels(buffer, 0, getDrawAreaWidth().toInt(), 0, 0, getDrawAreaWidth().toInt() - numberOfPixels, getDrawAreaHeight().toInt())
        }
    }
}
