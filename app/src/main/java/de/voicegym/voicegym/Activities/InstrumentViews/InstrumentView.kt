package de.voicegym.voicegym.Activities.InstrumentViews

import android.content.Context
import android.util.AttributeSet
import android.view.View
import de.voicegym.voicegym.FourierHelper.RequestedResult
import de.voicegym.voicegym.FourierHelper.RequestedResultType

abstract class InstrumentView : View {

    constructor(context: Context) : this(context, null)

    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)

    constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) : super(context, attrs, defStyleAttr) {

    }

    /**
     * The {@link RequestedResultType} RequestedResultType this implementation of the InstrumentView needs.
     *
     * This has to be overridden in concrete implementation of this class.
     */
    abstract val requiredResultType: RequestedResultType

    /**
     * This needs to be implemented to receive the result data
     */
    protected abstract fun addCheckedResult(requestedResult: RequestedResult)

    /**
     * @param requestedResult: The result to display in this InstrumentView
     */
    fun addResult(requestedResult: RequestedResult) {
        if (requestedResult.resultType == requiredResultType) addCheckedResult(requestedResult)
    }

    /**
     * This needs to be called once to tell the view the scale of the abscissa.
     *
     * @param resultDuration: the measurement time taken to capture the result (determined by binning and the blocklength of the fouriertransform)
     */
    var resultDuration: Double = 0.1

    /**
     * @param forwardSpeed: 1 / passtime, where passtime is the time in seconds a result should take to pass over the screen.
     *
     * This will set the speed of the data passing over the view.
     */
    var forwardSpeed: Double = 0.1
}