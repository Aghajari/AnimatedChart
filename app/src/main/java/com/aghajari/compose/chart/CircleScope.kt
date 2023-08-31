package com.aghajari.compose.chart

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerInputScope
import kotlin.math.acos
import kotlin.math.max
import kotlin.math.sqrt

interface CircleScope {

    var selectionFraction: Float
    val circleSize: Float
    val circleOffset: Float
    val maxDataValue: Float
    val strokeWidth: Float

    fun calculateSweep(value: Float): Float

    fun drawItem(
        brush: Brush,
        item: ChartData,
        start: Float,
        useCenter: Boolean = false,
        size: Size = Size(circleSize, circleSize),
        alpha: Float = 1.0f,
        colorFilter: ColorFilter? = null,
        blendMode: BlendMode = DrawScope.DefaultBlendMode
    )

    fun drawCenterCircle(
        color: Color,
        radius: Float
    )
}

private class CircleScopeImpl(
    private val originalSize: Float,
    private val padding: Float,
    private val originalStrokeWidth: Float,
    private val selectionSize: Float,
    private val data: Array<out ChartData>,
    private val drawScope: DrawScope? = null,
    private val animationCU: AnimationCU? = null,
) : CircleScope {

    override var selectionFraction: Float = 0f

    override val circleSize: Float
        get() = originalSize - padding - originalStrokeWidth + calculateSelectionSize()

    override val circleOffset: Float
        get() = (originalSize - circleSize) / 2f

    override val strokeWidth: Float
        get() = originalStrokeWidth + calculateSelectionSize()

    override val maxDataValue: Float by lazy {
        var maxData = 0f
        for (value in data) {
            maxData += value.data
        }
        max(1f, maxData)
    }

    override fun calculateSweep(value: Float): Float {
        return value * 360 / maxDataValue
    }

    override fun drawItem(
        brush: Brush,
        item: ChartData,
        start: Float,
        useCenter: Boolean,
        size: Size,
        alpha: Float,
        colorFilter: ColorFilter?,
        blendMode: BlendMode
    ) {
        if ((animationCU?.contentFractionOf(item) ?: 0f) > 0f) {
            return
        }

        var finalSize = animationCU?.heightOf(item) {
            size.times(max(0.12f, 1f - it))
        } ?: size

        var sweep = calculateSweep(item.data)
        var stroke = strokeWidth
        val animation = animationCU?.fractionOf(item) ?: 0f

        if (animation > 0f) {
            requireNotNull(animationCU)
            stroke -= (strokeWidth - finalSize.width / 2) * animation
            println(stroke)
            sweep += (360 - sweep) * animation

            if (animation >= 1f) {
                finalSize = Size(
                    width = finalSize.width + stroke,
                    height = finalSize.height + stroke
                )
                stroke = 0f
            }
        }

        val fixedPos = animationCU?.positionOf(item)
        val finalPos = if (fixedPos != null) {
            Offset(
                x = fixedPos.x - finalSize.width / 2,
                y = fixedPos.y - finalSize.height / 2
            )
        } else {
            val pos = (originalSize - finalSize.width) / 2f
            Offset(pos, pos)
        }

        drawScope?.drawArc(
            brush = brush,
            startAngle = start,
            sweepAngle = sweep,
            useCenter = useCenter,
            topLeft = finalPos,
            size = finalSize,
            alpha = alpha,
            style = if (animation >= 1f) {
                Fill
            } else {
                Stroke(stroke)
            },
            colorFilter = colorFilter,
            blendMode = blendMode
        )
    }

    override fun drawCenterCircle(color: Color, radius: Float) {
        drawScope?.apply {
            var animatedRadius = radius
            val animation = animationCU?.fractionOf(KEY_CENTER_CIRCLE) ?: 0f
            if (animation > 0f) {
                animatedRadius = animatedRadius.times(1f - animation)
            }
            if (animatedRadius <= 0f) return

            val centerX = Offset(
                x = center.x,
                y = center.x
            )

            drawCircle(
                color = color,
                radius = animatedRadius,
                center = centerX
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White,
                        Color.Transparent
                    ),
                    center = centerX,
                    radius = animatedRadius,
                ),
                radius = animatedRadius,
                center = centerX,
                alpha = 0.1f
            )
        }
    }

    fun getTouchIndex(pos: Offset): Int {
        val size = circleSize + originalStrokeWidth
        val offset = (originalSize - size) / 2
        if (pos.x < offset ||
            pos.y < offset ||
            pos.x > size + offset ||
            pos.y > size + offset
        ) {
            return -1
        }

        val center = originalSize / 2.0
        val x2 = pos.x - center
        val y2 = pos.y - center
        val d1 = sqrt(center * center)
        val d2 = sqrt(x2 * x2 + y2 * y2)
        val arc = Math.toDegrees(acos((-center * y2) / (d1 * d2)))
        val angle = if (pos.x >= center) arc else 360 - arc

        var calculated = 0f
        repeat(data.size) { index ->
            val sweep = calculateSweep(data[index].data)
            if (calculated <= angle && calculated + sweep > angle) {
                return index
            }
            calculated += sweep
        }
        return -1
    }

    private fun calculateSelectionSize(): Float {
        return selectionSize * selectionFraction
    }
}

internal fun DrawScope.toCircleScope(
    padding: Float,
    strokeWidth: Float,
    selectionSize: Float,
    data: Array<out ChartData>,
    animationCU: AnimationCU
): CircleScope {
    return CircleScopeImpl(
        originalSize = size.width,
        padding = padding,
        originalStrokeWidth = strokeWidth,
        selectionSize = selectionSize,
        data = data,
        drawScope = this,
        animationCU = animationCU
    )
}

internal suspend fun PointerInputScope.detectCircleTap(
    padding: Float,
    strokeWidth: Float,
    selectionSize: Float,
    data: Array<out ChartData>,
    onTap: (index: Int) -> Unit
) {
    detectTapGestures {
        val newIndex = CircleScopeImpl(
            originalSize = size.width.toFloat(),
            padding = padding,
            originalStrokeWidth = strokeWidth,
            selectionSize = selectionSize,
            data = data
        ).getTouchIndex(it)
        if (newIndex >= 0) {
            onTap(newIndex)
        }
    }
}