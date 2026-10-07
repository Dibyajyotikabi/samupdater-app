package com.samupdater.app.ui.theme

import androidx.compose.ui.graphics.Color

/** Raw One UI colors. Screens read them through MaterialTheme, except the hero gradient. */
object OneUiPalette {
    val White = Color(0xFFFFFFFF)
    val Black = Color(0xFF000000)

    val Blue = Color(0xFF2B54D6)
    val BlueDark = Color(0xFF5C82F5)
    val BlueSoft = Color(0xFFD6E0FF)
    val Navy = Color(0xFF16267A)
    val BlueTint = Color(0xFFE8EDFC)
    val BlueTintDark = Color(0xFF1E2A4D)
    val Green = Color(0xFF1E9E5A)
    val GreenDark = Color(0xFF4CCB85)

    val PageLight = Color(0xFFF2F2F5)
    val FieldLight = Color(0xFFEDEEF2)
    val DividerLight = Color(0xFFE4E4E8)
    val Ink = Color(0xFF111114)
    val Grey = Color(0xFF7C7C84)

    val CardDark = Color(0xFF1C1C1E)
    val FieldDark = Color(0xFF2C2C30)
    val DividerDark = Color(0xFF2E2E32)
    val InkDark = Color(0xFFF4F4F6)
    val GreyDark = Color(0xFF9A9AA2)

    /** The hero banner runs from royal blue down to navy. Keep in sync with brand_gradient_* in colors.xml. */
    val HeroStart = Color(0xFF3A6BF0)
    val HeroEnd = Color(0xFF0F1A63)
    val HeroRing = Color(0x33FFFFFF)
    val HeroGlow = Color(0x1FFFFFFF)
    val HeroCaption = Color(0xCCFFFFFF)
}
