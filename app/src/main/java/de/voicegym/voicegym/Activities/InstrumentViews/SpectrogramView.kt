package de.voicegym.voicegym.Activities.InstrumentViews

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import de.voicegym.voicegym.Activities.RecordActivity
import de.voicegym.voicegym.FourierHelper.AmplitudeResult
import de.voicegym.voicegym.FourierHelper.FourierHelper
import de.voicegym.voicegym.FourierHelper.RequestedResult
import de.voicegym.voicegym.FourierHelper.RequestedResultType.FFT_AMPLITUDE
import java.util.concurrent.ConcurrentLinkedQueue

//TODO SOLVE HOW TO UPDATE THE VIEW AT GIVEN INTERVALS
class SpectrogramView : InstrumentView {

    override val requiredResultType = FFT_AMPLITUDE

    val inputBuffer = ConcurrentLinkedQueue<AmplitudeResult>()

    private var viewBuffer: Array<DoubleArray>? = null

    private var fourierHelper: FourierHelper

    constructor(context: Context) : this(context, null)

    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)

    constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) : super(context, attrs, defStyleAttr) {

    }

    init {
        updateNumberOfDisplayedResults()
        //TODO refactor to be more general -> that the Activity has a fourierHelper
        if (context is RecordActivity) {
            fourierHelper = (context as RecordActivity).fourierHelper
        } else {
            throw RuntimeException("SpectrogramView requires RecordActivity as context")
        }
    }

    private fun updateNumberOfDisplayedResults() {
        var numberOfDisplayedResults = (1 / (forwardSpeed * resultDuration)).toInt()

        //TODO transfer old buffer to to new buffer
        viewBuffer = Array(numberOfDisplayedResults, { DoubleArray(height) })
    }

    fun changeSpeedAndDuration(forwardSpeed: Double, resultDuration: Double) {
        this.forwardSpeed = forwardSpeed
        this.resultDuration = resultDuration
        updateNumberOfDisplayedResults()
    }

    override fun addCheckedResult(requestedResult: RequestedResult) {
        inputBuffer.add(requestedResult as AmplitudeResult)
    }

    private fun moveResultIntoViewBuffer() {
        if (!inputBuffer.isEmpty()) {
            val amplitude = inputBuffer.poll().amplitude
            // TODO DECIDE HOW TO INTERPOLATE AND HOW TO HANDLE LINEAR VS. LOG(f) SCALING
        }
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)

        if (canvas != null) {
            val paint = Paint()
            paint.color = Color.WHITE
            paint.textSize = 46f
            canvas.drawText("TestText", 20f, 80f, paint)
        }
    }
}