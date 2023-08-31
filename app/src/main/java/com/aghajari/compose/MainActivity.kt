package com.aghajari.compose

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.aghajari.compose.chart.AnimatedChart
import com.aghajari.compose.chart.createSampleData
import com.aghajari.compose.ui.theme.AnimatedChartTheme
import com.aghajari.compose.ui.theme.BackgroundGradient
import com.aghajari.compose.ui.theme.ChartBackgroundGradient

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AnimatedChartTheme {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            brush = Brush.verticalGradient(
                                colors = BackgroundGradient
                            )
                        )
                ) {

                    ChartBox(
                        modifier = Modifier
                            .padding(24.dp)
                            .fillMaxWidth()
                            .heightIn(min = 320.dp)
                            .aspectRatio(1f)
                    )
                }
            }
        }
    }
}

@Composable
fun ChartBox(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shadowElevation = 8.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.linearGradient(
                        colors = ChartBackgroundGradient
                    )
                )
        ) {

            AnimatedChart(
                *createSampleData(),
                maxValue = 150f
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun Preview() {
    AnimatedChartTheme {
        ChartBox()
    }
}