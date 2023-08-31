package com.aghajari.compose.chart

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInParent
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.aghajari.compose.ui.theme.AnimatedChartTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.math.abs

@Composable
fun AnimatedChart(
    vararg data: ChartData,
    maxValue: Float,
    modifier: Modifier = Modifier,
    padding: Float = 124.dpToPx(),
    strokeWidth: Float = 88.dpToPx(),
    selectionSize: Float = 12.dpToPx(),
    centerCircleRadius: Float = 24.dpToPx(),
    centerCircleColor: Color = MaterialTheme.colorScheme.onPrimary,
    defaultSelectedIndex: Int = -1,
    selectorAnimationSpec: AnimationSpec<Float> = tween(
        durationMillis = 500,
        easing = OverShootEasing
    ),
    animationSpec: AnimationSpec<Float> = tween(
        durationMillis = 2000,
        easing = LinearEasing
    )
) {
    val coroutineScope = rememberCoroutineScope()
    val cu = remember(data) { AnimationCU(data, animationSpec) }

    val selectorAnimation = remember { Animatable(1f) }
    var previousSelectedIndex by remember { mutableStateOf(-1) }
    var selectedIndex by remember { mutableStateOf(defaultSelectedIndex) }

    DrawBars(data, cu, maxValue)

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .detectSwipe(coroutineScope, cu, selectorAnimation)
            .pointerInput(Unit) {
                detectCircleTap(padding, strokeWidth, selectionSize, data) { index ->
                    if (index != selectedIndex &&
                        selectorAnimation.isRunning.not() &&
                        cu.isRunning.not()
                    ) {
                        coroutineScope.launch {
                            previousSelectedIndex = selectedIndex
                            selectedIndex = index
                            selectorAnimation.snapTo(0f)
                            selectorAnimation.animateTo(
                                targetValue = 1f,
                                animationSpec = selectorAnimationSpec
                            )
                        }
                    }
                }
            }
    ) {

        toCircleScope(padding, strokeWidth, selectionSize, data, cu)
            .drawAllCircles(
                data,
                selectedIndex,
                previousSelectedIndex,
                selectorAnimation,
                centerCircleColor,
                centerCircleRadius,
                cu.isReverseRunning
            )
    }
}

private fun Modifier.detectSwipe(
    coroutineScope: CoroutineScope,
    cu: AnimationCU,
    selectorAnimation: Animatable<Float, AnimationVector1D>
): Modifier = composed {
    var swiped by remember { mutableStateOf(false) }

    pointerInput(Unit) {
        detectDragGestures(
            onDrag = { change, dragAmount ->
                change.consume()
                swiped = abs(dragAmount.x) > abs(dragAmount.y)
            },
            onDragEnd = {
                if (swiped.not()) return@detectDragGestures

                if (cu.canPlayReverse) {
                    coroutineScope.launch {
                        cu.reverse()
                    }
                } else if (cu.isRunning.not() && selectorAnimation.isRunning.not()) {
                    coroutineScope.launch {
                        cu.start()
                    }
                }
            }
        )
    }
}

private fun CircleScope.drawAllCircles(
    data: Array<out ChartData>,
    selectedIndex: Int,
    previousSelectedIndex: Int,
    selectorAnimation: Animatable<Float, AnimationVector1D>,
    centerCircleColor: Color,
    centerCircleRadius: Float,
    reverseDraw: Boolean
) {
    val startValues = mutableListOf<Float>()
    var start = -90f
    data.forEach {
        startValues.add(start)
        start += calculateSweep(it.data)
    }

    for (index in if (reverseDraw) {
        data.indices
    } else {
        data.size - 1 downTo 0
    }) {
        if (index == selectedIndex) {
            continue
        }

        val value = data[index]
        selectionFraction = when (index) {
            previousSelectedIndex -> 1f - selectorAnimation.value
            else -> 0f
        }

        drawItem(
            brush = Brush.verticalGradient(
                colors = value.colors
            ),
            item = value,
            start = startValues[index]
        )
    }
    if (selectedIndex >= 0 && selectedIndex < data.size) {
        selectionFraction = selectorAnimation.value

        drawItem(
            brush = Brush.verticalGradient(
                colors = data[selectedIndex].colors
            ),
            item = data[selectedIndex],
            start = startValues[selectedIndex]
        )
    }

    drawCenterCircle(
        color = centerCircleColor,
        radius = centerCircleRadius
    )
}

@Composable
private fun DrawBars(
    data: Array<out ChartData>,
    cu: AnimationCU,
    maxValue: Float
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(
                vertical = 32.dp
            )
    ) {
        data.forEach { item ->
            Box(Modifier.height(80.dp)) {
                BarLine(item, cu, maxValue)
                ItemImage(item, cu)
                ItemText(item, cu)
            }
        }
    }
}

@Composable
private fun ItemText(
    item: ChartData,
    cu: AnimationCU
) {
    val fraction = cu.contentFractionOf(item)

    Text(
        text = item.text(fraction),
        color = MaterialTheme.colorScheme.onSurface,
        style = MaterialTheme.typography.bodyLarge,
        modifier = Modifier
            .alpha(fraction)
            .padding(
                top = 14.dp,
                start = 90.dp,
                end = 24.dp
            )
    )
}

@Composable
private fun ItemImage(
    item: ChartData,
    cu: AnimationCU
) {
    val fraction = cu.contentFractionOf(item)

    Surface(
        shape = CircleShape,
        shadowElevation = 8.dp,
        modifier = Modifier
            .size(
                width = 68.dp,
                height = 44.dp
            )
            .alpha(fraction)
            .offset(x = 68.dp * (fraction - 1f))
            .padding(start = 24.dp)
    ) {
        Image(
            painter = painterResource(id = item.imageRes),
            contentDescription = null,
            contentScale = ContentScale.Crop
        )
    }
}

@Composable
private fun BarLine(
    item: ChartData,
    cu: AnimationCU,
    maxValue: Float
) {
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl

    Box(
        contentAlignment = Alignment.CenterStart,
        modifier = Modifier
            .fillMaxWidth()
            .height(12.dp)
            .padding(
                start = 90.dp,
                end = 24.dp
            )
            .createMovementPath(isRtl, item, cu)
            .background(
                color = Color.Black.copy(
                    alpha = 0.2f * cu.fractionOf(KEY_BAR)
                ),
                shape = CircleShape
            )
    ) {
        val maxFraction = item.data / maxValue
        val fraction = cu.contentFractionOf(item)
        Spacer(
            modifier = Modifier
                .fillMaxHeight()
                .widthIn(
                    min = if (fraction > 0) {
                        12.dp
                    } else {
                        0.dp
                    }
                )
                .fillMaxWidth(
                    fraction = fraction * maxFraction
                )
                .background(
                    brush = Brush.horizontalGradient(
                        colors = item.colors
                    ),
                    shape = CircleShape
                )
        )
    }
}

private fun Modifier.createMovementPath(
    isRtl: Boolean,
    item: ChartData,
    cu: AnimationCU,
): Modifier {
    return onGloballyPositioned {
        val root = it.parentLayoutCoordinates!!
            .parentLayoutCoordinates!!
            .parentLayoutCoordinates!!

        val bounds = root.localBoundingBoxOf(it)
        val pos = if (isRtl) {
            bounds.centerRight
        } else {
            bounds.centerLeft
        }

        cu.setPath(item, Path().apply {
            val rootCenter = root.boundsInParent().center

            moveTo(
                x = rootCenter.x,
                y = rootCenter.x
            )
            quadraticBezierTo(
                x1 = pos.x + if (isRtl) 300 else -300,
                y1 = pos.y + 250,
                x2 = pos.x + 10,
                y2 = pos.y
            )
        }, it.size.height.toFloat())
    }
}

@Composable
private fun Int.dpToPx() = this * LocalDensity.current.density

@Preview(
    showBackground = true,
    backgroundColor = 0xFF273A81
)
@Composable
private fun PreviewAnimatedChart() {
    AnimatedChartTheme {
        AnimatedChart(
            *createSampleData(),
            maxValue = 150f
        )
    }
}