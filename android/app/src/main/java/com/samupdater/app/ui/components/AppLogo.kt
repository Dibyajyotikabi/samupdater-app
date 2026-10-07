package com.samupdater.app.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.samupdater.app.R

/** The launcher icon as a rounded tile, used next to the app name on the home screen. */
@Composable
fun AppLogo(modifier: Modifier = Modifier) {
    // Same color resources as ic_launcher_background, so the logo always matches the home screen icon.
    val gradient = Brush.linearGradient(
        listOf(colorResource(R.color.brand_gradient_start), colorResource(R.color.brand_gradient_end)),
    )
    Box(
        modifier
            .size(LOGO_SIZE.dp)
            .clip(RoundedCornerShape(LOGO_CORNER.dp))
            .background(gradient),
    ) {
        Image(
            painter = painterResource(R.drawable.ic_launcher_foreground),
            contentDescription = "Sam Updater logo",
            modifier = Modifier.fillMaxSize().scale(GLYPH_SCALE),
        )
    }
}

private const val LOGO_SIZE = 56
private const val LOGO_CORNER = 16

/** The launcher glyph sits in a small safe zone, so enlarge it to fill the tile. */
private const val GLYPH_SCALE = 1.5f
