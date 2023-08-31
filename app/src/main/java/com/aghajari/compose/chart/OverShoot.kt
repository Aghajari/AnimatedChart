package com.aghajari.compose.chart

import androidx.compose.animation.core.Easing

val OverShootEasing = Easing {
    val t = it - 1.0f
    val tension = 4
    t * t * ((tension + 1) * t + tension) + 1.0f
}