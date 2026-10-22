package com.example.stockmarket.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color

// Tells every composable underneath whether the APP is in dark mode
// (which can be different from the phone's own setting)
val LocalDarkTheme = compositionLocalOf { false }

@Composable
fun gainColor(): Color = if (LocalDarkTheme.current) GainDark else GainLight

@Composable
fun lossColor(): Color = if (LocalDarkTheme.current) LossDark else LossLight