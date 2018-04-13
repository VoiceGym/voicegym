package de.voicegym.voicegym.FourierHelper

import de.voicegym.voicegym.FourierHelper.FFTJobType.*

class FFTJob(val jobType: FFTJobType,
             val pcmData: ShortArray) {

    var amplitude: DoubleArray? = null
    var phase: DoubleArray? = null
    var complexResult: DoubleArray? = null
    var processed: Boolean = false

    init {
        when (jobType) {
            GET_AMPLITUDE -> {
                amplitude = DoubleArray(pcmData.size)
            }
            GET_PHASE -> {
                phase = DoubleArray(pcmData.size)
            }
            GET_AMPLITUDE_AND_PHASE -> {
                amplitude = DoubleArray(pcmData.size)
                phase = DoubleArray(pcmData.size)
            }
            GET_COMPLEX_RESULT -> {
                complexResult = DoubleArray(2 * pcmData.size)
            }
        }
    }

}

enum class FFTJobType {
    GET_AMPLITUDE,
    GET_PHASE,
    GET_AMPLITUDE_AND_PHASE,
    GET_COMPLEX_RESULT
}
