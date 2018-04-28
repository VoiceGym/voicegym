package de.voicegym.voicegym.Activities.InstrumentViews

import android.graphics.Color
import android.graphics.Color.rgb

class HotGradientColorPicker {
    companion object {
        val rThreshold = 0.4f
        val gThreshold = 0.8f
        val bThreshold = 1f
        val delGR = 0xFF / (gThreshold - rThreshold);
        val delBG = 0xFF / (bThreshold - gThreshold)
        /**
         * value must be between 0 and 1, otherwise
         */
        fun pickColor(value: Double): Int {
            var r: Int = 0;
            var g: Int = 0;
            var b: Int = 0;
            if (value <= 0) {
                return Color.BLACK
            } else if (value < rThreshold) {
                r = ((value / rThreshold) * 0xFF).toInt()
            } else if (value < gThreshold) {
                r = 0xFF;
                g = ((value - rThreshold) * delGR).toInt()
            } else if (value < bThreshold) {
                r = 0xFF;
                g = 0xFF;
                b = ((value - gThreshold) * delBG).toInt()
            } else {
                return Color.WHITE
            }
            return rgb(r, g, b)
        }

        fun pickColor(value: Float): Int {
            return pickColor(value.toDouble())
        }
    }
}