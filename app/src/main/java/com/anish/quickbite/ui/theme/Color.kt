package com.anish.quickbite.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// =========================================================================
// QuickBite Editorial AMOLED Black & White + High-Fashion Food Palette
// Reference: Bold Neo-Brutalist Food Editorial (Acid Lime, Coral, Sunflower, Lilac)
// =========================================================================

// --- Signature Palette Colors from Reference ---
val AcidLime = Color(0xFFD4E75A)        // Iconic Chartreuse / Acid Lime from centerpiece
val AcidLimeDark = Color(0xFFBDD245)    // Slightly deeper lime for subtle gradient
val CoralPunch = Color(0xFFEA5A56)      // Warm watermelon coral from top-right / bottom-left
val CoralPunchDark = Color(0xFFD64743)  // Deep coral for subtle gradient
val SunflowerGold = Color(0xFFF5C544)   // Vibrant sunny yellow from top-left
val SunflowerGoldDark = Color(0xFFE2B030)
val LilacLavender = Color(0xFFCDB7E5)   // Soft chic lilac from bottom-right
val LilacLavenderDark = Color(0xFFBBA2D4)
val CreamPaper = Color(0xFFFBF6EF)      // Crisp cream from food photo cards

// --- Full AMOLED Dark Mode (True Pitch Black #000000) ---
val amoledBlack = Color(0xFF000000)      // 100% Pure AMOLED Pitch Black
val amoledCard = Color(0xFF0D0D10)       // Deep Obsidian surface for cards
val amoledCardBorder = Color(0xFF222228) // Subtle sharp border
val amoledField = Color(0xFF17171C)      // Input fields and elevated chips
val amoledTextPrimary = Color(0xFFFFFFFF)// 100% Crisp Pure White
val amoledTextSecondary = Color(0xFFA1A1AA) // Platinum / Zinc muted subtext
val amoledTextMuted = Color(0xFF71717A)

val primaryDark = AcidLime
val onPrimaryDark = Color(0xFF000000)    // High contrast black on acid lime
val primaryContainerDark = Color(0xFF232A08)
val onPrimaryContainerDark = Color(0xFFE5F57B)

val secondaryDark = CoralPunch
val onSecondaryDark = Color(0xFFFFFFFF)
val secondaryContainerDark = Color(0xFF3B1211)
val onSecondaryContainerDark = Color(0xFFFFDAD6)

val tertiaryDark = SunflowerGold
val onTertiaryDark = Color(0xFF000000)
val tertiaryContainerDark = Color(0xFF382C00)
val onTertiaryContainerDark = Color(0xFFFFE082)

val errorDark = Color(0xFFFF5252)
val onErrorDark = Color(0xFF000000)
val errorContainerDark = Color(0xFF450A0A)
val onErrorContainerDark = Color(0xFFFFDAD6)

val backgroundDark = amoledBlack
val onBackgroundDark = amoledTextPrimary
val surfaceDark = amoledCard
val onSurfaceDark = amoledTextPrimary
val surfaceVariantDark = amoledField
val onSurfaceVariantDark = amoledTextSecondary

// --- Stark Editorial Light Mode (Clean White + Pitch Black) ---
val primaryLight = Color(0xFF000000)    // Stark Pitch Black
val onPrimaryLight = Color(0xFFFFFFFF)  // Crisp White
val primaryContainerLight = AcidLime
val onPrimaryContainerLight = Color(0xFF000000)

val secondaryLight = CoralPunch
val onSecondaryLight = Color(0xFFFFFFFF)
val secondaryContainerLight = Color(0xFFFFECEB)
val onSecondaryContainerLight = Color(0xFF6B1210)

val tertiaryLight = SunflowerGold
val onTertiaryLight = Color(0xFF000000)
val tertiaryContainerLight = Color(0xFFFFF6D6)
val onTertiaryContainerLight = Color(0xFF5E4500)

val errorLight = Color(0xFFDC2626)
val onErrorLight = Color(0xFFFFFFFF)
val errorContainerLight = Color(0xFFFEE2E2)
val onErrorContainerLight = Color(0xFF7F1D1D)

val backgroundLight = Color(0xFFFAFAFA)
val onBackgroundLight = Color(0xFF0A0A0A)
val surfaceLight = Color(0xFFFFFFFF)
val onSurfaceLight = Color(0xFF0A0A0A)
val surfaceVariantLight = Color(0xFFF2F2F5)
val onSurfaceVariantLight = Color(0xFF52525B)

/**
 * High-end subtle tone-on-tone gradients based on the editorial reference
 */
object QuickBiteGradients {
    // Acid Lime subtle monochromatic gradient
    val primary = Brush.horizontalGradient(
        colors = listOf(AcidLime, AcidLimeDark)
    )
    val primaryVertical = Brush.verticalGradient(
        colors = listOf(AcidLime, AcidLimeDark)
    )

    // Coral subtle monochromatic gradient
    val coral = Brush.horizontalGradient(
        colors = listOf(CoralPunch, CoralPunchDark)
    )

    // Sunflower Gold subtle monochromatic gradient
    val sunflower = Brush.horizontalGradient(
        colors = listOf(SunflowerGold, SunflowerGoldDark)
    )

    // Lilac subtle monochromatic gradient
    val lilac = Brush.horizontalGradient(
        colors = listOf(LilacLavender, LilacLavenderDark)
    )

    // True AMOLED Card Gradient (deep obsidian to pure pitch black)
    val amoledCard = Brush.verticalGradient(
        colors = listOf(Color(0xFF131316), Color(0xFF09090B))
    )

    // Stark Light Card Gradient
    val lightCard = Brush.verticalGradient(
        colors = listOf(Color(0xFFFFFFFF), Color(0xFFF8F8FA))
    )
}
