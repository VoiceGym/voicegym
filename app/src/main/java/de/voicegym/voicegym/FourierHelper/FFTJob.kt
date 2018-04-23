package de.voicegym.voicegym.FourierHelper

sealed class FFTJob(
        open val  pcmData: ShortArray)

data class AmplitudeJob(
        override val pcmData: ShortArray,
        val amplitude: DoubleArray,
        val processed: Boolean = false
) : FFTJob(pcmData)

data class PhaseJob(
        override val pcmData: ShortArray,
        val phase: DoubleArray,
        val processed: Boolean = false
) : FFTJob(pcmData)

data class AmplitudeAndPhaseJob(
        override val pcmData: ShortArray,
        val amplitude: DoubleArray,
        val phase: DoubleArray,
        val processed: Boolean = false
) : FFTJob(pcmData)

data class ComplexJob(
        override val pcmData: ShortArray,
        val complexResult: DoubleArray,
        val processed: Boolean = false
) : FFTJob(pcmData)


//
//class FFTJobA(val jobType: FFTJobType,
//              val pcmData: ShortArray) {
//
//    var amplitude: DoubleArray? = null
//    var phase: DoubleArray? = null
//    var complexResult: DoubleArray? = null
//    var processed: Boolean = false
//
//    init {
//        when (jobType) {
//            GET_AMPLITUDE -> {
//                amplitude = DoubleArray(pcmData.size)
//            }
//            GET_PHASE -> {
//                phase = DoubleArray(pcmData.size)
//            }
//            GET_AMPLITUDE_AND_PHASE -> {
//                amplitude = DoubleArray(pcmData.size)
//                phase = DoubleArray(pcmData.size)
//            }
//            GET_COMPLEX_RESULT -> {
//                complexResult = DoubleArray(2 * pcmData.size)
//            }
//        }
//    }
//
//}
//
//enum class FFTJobType {
//    GET_AMPLITUDE,
//    GET_PHASE,
//    GET_AMPLITUDE_AND_PHASE,
//    GET_COMPLEX_RESULT
//}
