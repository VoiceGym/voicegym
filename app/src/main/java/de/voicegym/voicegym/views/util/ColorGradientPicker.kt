package de.voicegym.voicegym.views.util

import android.animation.ArgbEvaluator
import java.util.TreeMap


class ColorGradientPicker : GradientPicker {
    private val colorMap = TreeMap<Float, Int>()

    private val evaluator = ArgbEvaluator()

    fun addColorToPalette(atValue: Float, color: Int): ColorGradientPicker {
        colorMap[atValue] = color
        return this
    }

    override fun pickColor(value: Float): Int {
        return if ((colorMap.firstKey() <= value) && (value <= colorMap.lastKey())) {
            if (colorMap.containsKey(value)) {
                colorMap[value] as Int
            } else {
                val low: Float = colorMap.lowerKey(value)
                val high: Float = colorMap.higherKey(value)
                (evaluator.evaluate((value - low) / (high - low), colorMap.getValue(low), colorMap.getValue(high))) as Int
            }
        } else throw RuntimeException("Color not in range of picker")
    }

    override fun pickColor(value: Double): Int = pickColor(value.toFloat())

}
