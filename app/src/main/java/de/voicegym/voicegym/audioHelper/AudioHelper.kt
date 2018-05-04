package de.voicegym.voicegym.audioHelper

object AudioHelper {

    fun getDezibelFromAmplitude(amplitude: Double): Double {
        return (20 * Math.log10(amplitude))
    }
}