package com.samupdater.app.ui.device

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.samupdater.app.ui.theme.OneUiPalette

/** Blue gradient banner with the big version numeral, like Samsung's own update screen. */
@Composable
internal fun HeroBanner(brand: String, numeral: String, caption: String, modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .height(BANNER_HEIGHT.dp)
            .clip(RoundedCornerShape(BANNER_CORNER.dp))
            .background(Brush.linearGradient(listOf(OneUiPalette.HeroStart, OneUiPalette.HeroEnd))),
    ) {
        BannerDecor()
        Column(Modifier.align(Alignment.BottomStart).padding(22.dp)) {
            Row {
                Text(
                    brand,
                    style = MaterialTheme.typography.headlineMedium,
                    color = OneUiPalette.White,
                    modifier = Modifier.alignByBaseline().padding(end = 10.dp),
                )
                Text(
                    numeral,
                    style = MaterialTheme.typography.displayLarge,
                    color = OneUiPalette.White,
                    modifier = Modifier.alignByBaseline(),
                )
            }
            Text(caption.uppercase(), style = MaterialTheme.typography.labelSmall, color = OneUiPalette.HeroCaption)
        }
    }
}

/** Soft circles and a thin ring in the top-right corner. Purely decorative. */
@Composable
private fun BannerDecor() {
    Canvas(Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        drawCircle(OneUiPalette.HeroGlow, radius = h * 0.75f, center = Offset(w * 0.95f, h * 0.05f))
        drawCircle(OneUiPalette.HeroGlow, radius = h * 0.32f, center = Offset(w * 0.78f, h * 0.62f))
        drawCircle(
            OneUiPalette.HeroRing,
            radius = h * 0.42f,
            center = Offset(w * 0.9f, h * 0.78f),
            style = Stroke(width = RING_STROKE.dp.toPx()),
        )
    }
}

private const val BANNER_HEIGHT = 210
private const val BANNER_CORNER = 22
private const val RING_STROKE = 1.5f
