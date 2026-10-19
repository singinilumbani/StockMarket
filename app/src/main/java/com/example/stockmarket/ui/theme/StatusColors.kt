package com.example.stockmarket.ui.theme



import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

@Composable
fun gainColor(): Color = if (isSystemInDarkTheme()) GainDark else GainLight

@Composable
fun lossColor(): Color = if (isSystemInDarkTheme()) LossDark else LossLight