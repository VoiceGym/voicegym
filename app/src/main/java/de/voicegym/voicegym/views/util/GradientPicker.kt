package de.voicegym.voicegym.views.util

interface GradientPicker {
    fun pickColor(value: Double): Int
    fun pickColor(value: Float): Int
}