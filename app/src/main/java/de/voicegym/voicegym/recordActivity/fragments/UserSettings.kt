package de.voicegym.voicegym.recordActivity.fragments

data class UserSettings(
        val fromFrequency: Double,
        val tillFrequency: Double,
        val numberDataPoints: Int,
        val samplesPerDatapoint: Int
) {}
