package com.aghajari.compose.chart

import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.Color
import com.aghajari.compose.R
import java.math.RoundingMode
import java.text.DecimalFormat

data class ChartData(
    val colors: List<Color>,
    val data: Float,
    @DrawableRes val imageRes: Int,
    val text: (fraction: Float) -> String
)

internal fun createSampleData(): Array<ChartData> {
    return arrayOf(
        createSample(
            colors = listOf(
                Color(0xFF64C9C4),
                Color(0xFF4897B2)
            ),
            data = 130.42f,
            imageRes = R.drawable.chandler
        ),
        createSample(
            colors = listOf(
                Color(0xFFDB4E80),
                Color(0xFFB23972)
            ),
            data = 90.12f,
            imageRes = R.drawable.rachel
        ),
        createSample(
            colors = listOf(
                Color(0xFFEE7E71),
                Color(0xFFC25F59)
            ),
            data = 30.21f,
            imageRes = R.drawable.joey
        ),
        createSample(
            colors = listOf(
                Color(0xFF364699),
                Color(0xFF3D4DBC),
            ),
            data = 60.19f,
            imageRes = R.drawable.phoebe
        )
    )
}

private fun createSample(
    colors: List<Color>,
    data: Float,
    imageRes: Int
): ChartData {
    return ChartData(
        colors = colors,
        data = data,
        imageRes = imageRes
    ) { fraction ->
        "$" + round(500 + (500 + data * 10) * fraction)
    }
}

private fun round(number: Float): String {
    return DecimalFormat("#.##").run {
        roundingMode = RoundingMode.FLOOR
        format(number)
    }
}