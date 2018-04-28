package de.voicegym.voicegym.Activities.InstrumentViews

interface GradientPicker {
    fun pickColor(value: Double): Int
    fun pickColor(value: Float): Int
}