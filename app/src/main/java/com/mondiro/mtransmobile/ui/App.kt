package com.mondiro.mtransmobile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.TextFields
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mondiro.mtransmobile.AppViewModel
import com.mondiro.mtransmobile.Tab
import kotlinx.coroutines.delay

@Composable
fun App(vm: AppViewModel, wide: Boolean) {
    Column(
        Modifier.fillMaxSize().background(C.Chrome).windowInsetsPadding(WindowInsets.safeDrawing),
    ) {
        if (vm.tab != Tab.CAMERA || vm.photo != null || wide) TopRow(vm)
        Box(Modifier.weight(1f).fillMaxWidth()) {
            when (vm.tab) {
                Tab.TEXT -> TextScreen(vm, wide)
                Tab.CAMERA -> CameraScreen(vm, wide)
                Tab.SETTINGS -> SettingsScreen(vm)
            }
            val msg = vm.updateMsg
            if (msg != null) {
                LaunchedEffect(msg) {
                    delay(3000)
                    vm.updateMsg = null
                }
                Snackbar(
                    modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth(0.92f),
                    containerColor = C.Panel, contentColor = C.Text,
                ) { Text(msg) }
            }
        }
        HorizontalDivider(color = C.Line)
        BottomNav(vm)
    }
}

@Composable
private fun BottomNav(vm: AppViewModel) {
    Row(Modifier.fillMaxWidth().height(64.dp).background(C.Chrome)) {
        NavItem(Icons.Outlined.CameraAlt, "Camera", vm.tab == Tab.CAMERA) { vm.tab = Tab.CAMERA }
        NavItem(Icons.Outlined.TextFields, "Văn bản", vm.tab == Tab.TEXT) { vm.tab = Tab.TEXT }
        NavItem(Icons.Outlined.Settings, "Cài đặt", vm.tab == Tab.SETTINGS) { vm.tab = Tab.SETTINGS }
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.NavItem(icon: ImageVector, label: String, on: Boolean, onClick: () -> Unit) {
    Box(Modifier.weight(1f).fillMaxHeight().clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        if (on) {
            Box(
                Modifier.align(Alignment.TopCenter).fillMaxWidth(0.4f).height(2.dp)
                    .clip(RoundedCornerShape(2.dp)).background(C.Red),
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Icon(icon, contentDescription = label, tint = if (on) Color.White else C.Muted, modifier = Modifier.size(23.dp))
            Text(label, fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold, color = if (on) Color.White else C.Muted)
        }
    }
}
