// cosmicjoke
package com.aum.aumhora

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.aum.aumhora.ui.theme.AumhoraTheme

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AumhoraTheme {
                HoraCircle(horaColor = Color.Red)
            }
        }
    }
}

@Composable
fun HoraCircle(horaColor: Color = Color.Red) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier) {
            val radius = size.minDimension / 4f
            drawCircle(
                color = horaColor,
                radius = radius,
                center = center
            )
        }
    }
}