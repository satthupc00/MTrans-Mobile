package com.mondiro.mtransmobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import com.mondiro.mtransmobile.ui.App
import com.mondiro.mtransmobile.ui.MTransTheme

class MainActivity : ComponentActivity() {
    private val vm: AppViewModel by viewModels()

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(0xFF1F2126.toInt()),
            navigationBarStyle = SystemBarStyle.dark(0xFF1F2126.toInt()),
        )
        setContent {
            val size = calculateWindowSizeClass(this)
            MTransTheme {
                // Z Fold 7: outer screen is Compact (one column), inner screen is Medium/Expanded (split).
                App(vm, wide = size.widthSizeClass != WindowWidthSizeClass.Compact)
            }
        }
    }
}
