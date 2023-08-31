package com.aghajari.compose.chart

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import kotlin.math.max
import kotlin.math.min

internal class AnimationCU(
    data: Array<out ChartData>,
    private val animationSpec: AnimationSpec<Float>
) {

    private val animator = Animatable(0f)
    private val ranges = mutableMapOf<Any, AnimationRange>()

    init {
        var start = 0f
        var cost = FRACTION_DELAY
        data.forEach {
            val endPathFraction = 1f + (FRACTION_DELAY - cost) * 2

            ranges[it] = AnimationRange(
                drawFraction = FractionRange(
                    start = start,
                    end = start + 0.35f
                ),
                pathFraction = FractionRange(
                    start = start + 0.35f,
                    end = endPathFraction
                ),
                heightFraction = FractionRange(
                    start = start + 0.5f,
                    end = endPathFraction
                ),
                contentFraction = FractionRange(
                    start = endPathFraction,
                    end = min(MAX_FRACTION, endPathFraction + 0.3f)
                )
            )
            start += cost
            cost = max(0f, cost - 0.05f)
        }

        ranges[KEY_CENTER_CIRCLE] = AnimationRange(
            drawFraction = FractionRange(
                start = 0.3f,
                end = start - cost + 0.35f
            )
        )

        ranges[KEY_BAR] = AnimationRange(
            drawFraction = FractionRange(
                start = 0.2f,
                end = 0.6f
            )
        )
    }

    private val totalFraction: Float
        get() = animator.value

    val isRunning: Boolean
        get() = animator.isRunning || animator.value != 0f

    val isReverseRunning: Boolean
        get() = animator.isRunning && animator.targetValue == 0.0f

    val canPlayReverse: Boolean
        get() = animator.isRunning.not() &&
                animator.value == MAX_FRACTION

    fun setPath(key: Any, path: Path, size: Float) {
        ranges[key]?.apply {
            pathMeasure = PathMeasure().also {
                it.setPath(path, false)
            }
            finalSize = size
        }
    }

    fun fractionOf(key: Any, fraction: Float = totalFraction): Float {
        return getFractionInRange(ranges[key]!!.drawFraction, fraction)
    }

    fun contentFractionOf(key: Any): Float {
        return LinearOutSlowInEasing.transform(
            getFractionInRange(ranges[key]!!.contentFraction)
        )
    }

    private fun getFractionInRange(
        fractionRange: FractionRange,
        fraction: Float = totalFraction
    ): Float {
        return if (fractionRange.start >= fraction) {
            0f
        } else if (fractionRange.end <= fraction) {
            1f
        } else {
            (fraction - fractionRange.start) / (fractionRange.end - fractionRange.start)
        }
    }

    fun positionOf(key: Any): Offset? {
        return ranges[key]?.run {
            val fraction = getFractionInRange(pathFraction)
            if (fraction == 0f) {
                return null
            }
            println(fraction)
            pathMeasure?.getPosition(pathMeasure!!.length * fraction)
        }
    }

    fun heightOf(key: Any, sizeCalculator: (Float) -> Size): Size {
        return ranges[key]?.run {
            val fraction = getFractionInRange(heightFraction)
            val withStroke = finalSize / 1.5f
            if (fraction >= 1f) {
                Size(withStroke, withStroke)
            } else if (fraction == 0f) {
                sizeCalculator(fractionOf(key))
            } else {
                val startSize = sizeCalculator(fractionOf(key, heightFraction.start)).width
                val size = (startSize + fraction * (withStroke - startSize))
                Size(size, size)
            }
        } ?: sizeCalculator(fractionOf(key))
    }

    suspend fun start() {
        animator.animateTo(MAX_FRACTION, animationSpec)
    }

    suspend fun reverse() {
        animator.animateTo(0f, animationSpec)
    }
}

private data class FractionRange(
    val start: Float,
    val end: Float
)

private data class AnimationRange(
    val drawFraction: FractionRange = FractionRange(0f, 0f),
    val pathFraction: FractionRange = FractionRange(0f, 0f),
    val heightFraction: FractionRange = FractionRange(0f, 0f),
    val contentFraction: FractionRange = FractionRange(0f, 0f),
    var pathMeasure: PathMeasure? = null,
    var finalSize: Float = 0f
)

private const val MAX_FRACTION = 1.6f
private const val FRACTION_DELAY = 0.2f

internal const val KEY_CENTER_CIRCLE = 0
internal const val KEY_BAR = 1