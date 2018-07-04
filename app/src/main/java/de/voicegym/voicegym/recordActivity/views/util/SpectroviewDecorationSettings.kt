package de.voicegym.voicegym.recordActivity.views.util

import android.graphics.Paint

data class SpectrogramViewDecorationSettings(val paint: Paint,
                                             val drawArea: SpectrogramViewPaintArea)


data class SpectrogramViewPaintArea(val left: Float,
                                    val right: Float,
                                    val bottom: Float,
                                    val top: Float)
