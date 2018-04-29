package de.voicegym.voicegym.views.util

import android.graphics.Color
import android.graphics.Color.rgb

object HotGradientColorPicker : GradientPicker {

    const val rThreshold = 0.4
    const val gThreshold = 0.8
    const val bThreshold = 1.0
    const val delGR = 255 / (gThreshold - rThreshold)
    const val delBG = 255 / (bThreshold - gThreshold)

    /**
     * value must be between 0 and 1, otherwise
     */
    override fun pickColor(value: Double) = when {
        value <= 0 ->
            Color.BLACK
        value < rThreshold -> {
            val r = ((value / rThreshold) * 255).toInt()
            rgb(r, 0, 0)
        }
        value < gThreshold -> {
            val g = ((value - rThreshold) * delGR).toInt()
            rgb(255, g, 0)
        }
        value < bThreshold -> {
            val b = ((value - gThreshold) * delBG).toInt()
            rgb(255, 255, b)
        }
        else ->
            Color.WHITE
    }

    override fun pickColor(value: Float): Int = pickColor(value.toDouble())

}
