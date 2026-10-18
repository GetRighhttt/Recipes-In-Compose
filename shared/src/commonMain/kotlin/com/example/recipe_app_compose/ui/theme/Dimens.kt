package com.example.recipe_app_compose.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

// Shared design tokens remove one-off sizing decisions from individual screens.
object AppSpacing {
    val ExtraSmall = 4.dp
    val Small = 8.dp
    val Medium = 12.dp
    val Large = 16.dp
    val ExtraLarge = 24.dp
    val ExtraExtraLarge = 32.dp
}

object AppSizes {
    val MinimumTouchTarget = 48.dp
    val MinimumCompactGridCardWidth = 104.dp
    val MinimumGridCardWidth = 152.dp
    val RestaurantCardImage = 136.dp
    val MaximumReadableWidth = 640.dp
}

val AppCardShape = RoundedCornerShape(16.dp)
val AppControlShape = RoundedCornerShape(12.dp)
