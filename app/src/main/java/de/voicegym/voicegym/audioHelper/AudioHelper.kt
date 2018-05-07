package de.voicegym.voicegym.audioHelper

object AudioHelper {

    fun getDezibelFromAmplitude(amplitude: Double): Double = 20 * Math.log10(amplitude)

}
