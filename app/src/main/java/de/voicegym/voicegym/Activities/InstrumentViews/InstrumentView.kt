package de.voicegym.voicegym.Activities.InstrumentViews

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.util.AttributeSet
import android.view.View

class InstrumentView : View {

    constructor(context: Context) : this(context, null)

    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)

    constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) : super(context, attrs, defStyleAttr) {

    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)

        if (canvas != null) {
            val paint = Paint()
            paint.color = Color.WHITE
            paint.textSize = 46.0f
            canvas.drawText("TestText", 20.0f, 80.0f, paint)

        }
    }
}