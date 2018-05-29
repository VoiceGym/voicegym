package de.voicegym.voicegym.util.math

import de.voicegym.voicegym.util.math.RequestedResultType.FFT_AMPLITUDE
import de.voicegym.voicegym.util.math.RequestedResultType.FFT_AMPLITUDE_AND_PHASE
import de.voicegym.voicegym.util.math.RequestedResultType.FFT_COMPLEX_RESULT
import de.voicegym.voicegym.util.math.RequestedResultType.FFT_PHASE

data class ResultTask(val requestedResult: RequestedResultType, val pcmData: ShortArray)

abstract class RequestedResult {
    abstract val resultType: RequestedResultType
}

data class AmplitudeResult(val amplitude: DoubleArray) : RequestedResult() {
    override val resultType = FFT_AMPLITUDE
}

data class PhaseResult(val phase: DoubleArray) : RequestedResult() {
    override val resultType = FFT_PHASE
}

data class AmplitudeAndPhaseResult(val amplitude: DoubleArray, val phase: DoubleArray) : RequestedResult() {
    override val resultType = FFT_AMPLITUDE_AND_PHASE
}

data class ComplexResult(val complexResult: DoubleArray) : RequestedResult() {
    override val resultType = FFT_COMPLEX_RESULT
}
