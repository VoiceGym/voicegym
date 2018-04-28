package de.voicegym.voicegym.Activities.InstrumentViews

import android.animation.ArgbEvaluator
import java.util.TreeMap


class ColorGradientPicker() : GradientPicker {
    private val colorMap = TreeMap<Float, Int>()

    private val evaluator = ArgbEvaluator()

    fun addColorToPalette(atValue: Float, color: Int): ColorGradientPicker {
        colorMap.put(atValue, color)
        return this
    }

    override fun pickColor(value: Float): Int {
        if ((colorMap.firstKey() <= value) && (value <= colorMap.lastKey())) {
            if (colorMap.containsKey(value)) {
                return colorMap.get(value) as Int
            } else {
                var low: Float = colorMap.lowerKey(value)
                var high: Float = colorMap.higherKey(value)
                return (evaluator.evaluate((value - low) / (high - low), colorMap.getValue(low), colorMap.getValue(high))) as Int
            }
        } else throw RuntimeException("Color not in range of picker")
    }

    override fun pickColor(value: Double): Int = pickColor(value.toFloat())

}