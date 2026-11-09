package com.mondiro.mtransmobile.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

object C {
    val Chrome = Color(0xFF1F2126)
    val Panel = Color(0xFF26292F)
    val Line = Color(0xFF363A42)
    val Hover = Color(0xFF2F333A)
    val Stage = Color(0xFF2B2D31)
    val Result = Color(0xFF22252B)
    val Text = Color(0xFFE8E9EC)
    val Muted = Color(0xFF8D929C)
    val Dim = Color(0xFF6B707A)
    val Red = Color(0xFFE5383B)
    val RedSoft = Color(0xFFE86A6C)
    val Warn = Color(0xFFF2B84B)
    val SelectedBg = Color(0xFF3A2426)
    val Green = Color(0xFF2EB872)
    val Cream = Color(0xFFF4F0E8)
}

@Composable
fun MTransTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = C.Red,
            onPrimary = Color.White,
            background = C.Chrome,
            onBackground = C.Text,
            surface = C.Chrome,
            onSurface = C.Text,
            surfaceVariant = C.Panel,
            onSurfaceVariant = C.Muted,
            outline = C.Line,
            surfaceContainer = C.Panel,
            surfaceContainerHigh = Color(0xFF2B2E35),
        ),
        content = content,
    )
}
