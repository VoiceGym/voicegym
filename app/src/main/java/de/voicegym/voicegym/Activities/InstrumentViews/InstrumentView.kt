package de.voicegym.voicegym.Activities.InstrumentViews

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View

abstract class InstrumentView : View {

    constructor(context: Context) : this(context, null)

    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)

    constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) : super(context, attrs, defStyleAttr) {

    }


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

    /**
     * This function needs to be overridden by the concrete implementation of the InstrumentView
     */
    protected abstract fun drawInstrument(canvas: Canvas)


    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)

        if (canvas != null) {
            drawInstrument(canvas)

            drawBorder(canvas)
        }

    }

    private fun drawBorder(canvas: Canvas) {
        val paint = Paint()
        paint.color = border_color
        paint.strokeWidth = border_thickness
        // top line
        canvas.drawLine(left_margin - border_thickness, top_margin - border_thickness, width - right_margin + border_thickness, top_margin - border_thickness, paint)
        // bottom line
        canvas.drawLine(left_margin - border_thickness, height - bottom_margin + border_thickness, width - right_margin + border_thickness, height - bottom_margin + border_thickness, paint)
        // left line
        canvas.drawLine(left_margin - border_thickness, top_margin - border_thickness, left_margin - border_thickness, height - bottom_margin + border_thickness, paint)
        // right line
        canvas.drawLine(width - right_margin + border_thickness, top_margin - border_thickness, width - right_margin + border_thickness, height - bottom_margin + border_thickness, paint)
    }
}