package com.mondiro.mtransmobile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mondiro.mtransmobile.AppViewModel
import com.mondiro.mtransmobile.Lang
import com.mondiro.mtransmobile.PackState
import com.mondiro.mtransmobile.Prefs

@Composable
fun SettingsScreen(vm: AppViewModel) {
    val ctx = LocalContext.current
    var showWhatsNew by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { vm.refreshPacks() }
    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 30.dp)
                .widthIn(max = 640.dp),
        ) {
            Label("Gói dịch offline")
            Lang.entries.forEachIndexed { i, l ->
                PackRow(vm, l)
                if (i < Lang.entries.size - 1) HorizontalDivider(color = C.Line)
            }
            Hint("Mỗi gói khoảng 30 MB, tải 1 lần. Có mạng thì MTrans vẫn dùng Google cho bản dịch tốt hơn; mất mạng thì tự dịch offline.")
            Spacer(Modifier.height(20.dp))

            Label("Claude API key")
            KeyField(vm)
            Spacer(Modifier.height(16.dp))
            if (vm.hasKey) {
                Label("Model Claude")
                ModelPicker(vm)
                Spacer(Modifier.height(20.dp))
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Luôn dịch offline", color = Color.White, fontSize = 15.sp)
                    Text("Không gửi chữ hay ảnh lên mạng", color = C.Muted, fontSize = 11.5.sp)
                }
                Switch(
                    checked = vm.alwaysOffline, onCheckedChange = vm::setAlwaysOfflineMode,
                    colors = SwitchDefaults.colors(checkedTrackColor = C.Red, checkedThumbColor = Color.White, uncheckedTrackColor = Color(0xFF474C55), uncheckedThumbColor = Color.White, uncheckedBorderColor = Color.Transparent),
                )
            }
            Spacer(Modifier.height(22.dp))

            Label("Phiên bản")
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("v${vm.version}", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.size(8.dp))
                if (vm.update?.hasUpdate == false) LatestBadge()
                Spacer(Modifier.weight(1f))
                OutlinedButton(onClick = { showWhatsNew = true }, shape = RoundedCornerShape(9.dp)) {
                    Icon(Icons.Filled.AutoAwesome, null, tint = C.Text, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.size(6.dp))
                    Text("What's new", color = C.Text, fontSize = 13.sp)
                }
            }
            val u = vm.update
            if (u?.hasUpdate == true) {
                Row(Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Có bản mới v${u.latest}", color = C.Green, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                    Button(onClick = { vm.installUpdate(ctx) }, colors = ButtonDefaults.buttonColors(containerColor = C.Red)) { Text("Cập nhật") }
                }
            }
        }
        Credit(Modifier.align(Alignment.BottomEnd).padding(end = 14.dp, bottom = 6.dp))
    }
    if (showWhatsNew) WhatsNewDialog(ctx) { showWhatsNew = false }
}

@Composable
private fun Label(t: String) {
    Text(t.uppercase(), color = C.Muted, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.6.sp, modifier = Modifier.padding(bottom = 8.dp))
}

@Composable
private fun Hint(t: String, color: Color = C.Muted) {
    Text(t, color = color, fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp))
}

@Composable
private fun PackRow(vm: AppViewModel, l: Lang) {
    val st = vm.packs[l] ?: PackState.MISSING
    Row(Modifier.fillMaxWidth().height(56.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(l.label, color = Color.White, fontSize = 15.sp)
            Text(
                when {
                    l == Lang.EN -> "Có sẵn"
                    st == PackState.DOWNLOADING -> "Đang tải…"
                    st == PackState.READY -> "Đã tải"
                    else -> "~30 MB"
                },
                color = C.Muted, fontSize = 11.5.sp,
            )
        }
        when {
            l == Lang.EN || (st == PackState.READY && l == Lang.EN) -> Text("✓ Sẵn sàng", color = C.Green, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            st == PackState.READY -> Row(verticalAlignment = Alignment.CenterVertically) {
                Text("✓ Đã tải", color = C.Green, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                TextButton(onClick = { vm.deletePack(l) }) { Text("Xoá", color = C.Muted, fontSize = 12.sp) }
            }
            st == PackState.DOWNLOADING -> LinearProgressIndicator(color = C.Red, trackColor = Color(0xFF3A3E46), modifier = Modifier.size(90.dp, 4.dp))
            else -> Button(
                onClick = { vm.downloadPack(l) }, enabled = vm.online,
                colors = ButtonDefaults.buttonColors(containerColor = C.Red), shape = RoundedCornerShape(9.dp),
            ) { Text("Tải", fontSize = 13.sp) }
        }
    }
}

@Composable
private fun KeyField(vm: AppViewModel) {
    var key by remember { mutableStateOf("") }
    var show by remember { mutableStateOf(false) }
    Row(
        Modifier.fillMaxWidth().height(48.dp).clip(RoundedCornerShape(10.dp)).background(C.Panel)
            .border(1.dp, C.Line, RoundedCornerShape(10.dp)).padding(start = 12.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BasicTextField(
            value = key, onValueChange = { key = it }, singleLine = true,
            textStyle = TextStyle(color = Color.White, fontSize = 14.sp),
            cursorBrush = SolidColor(C.Red),
            visualTransformation = if (show) VisualTransformation.None else PasswordVisualTransformation(),
            modifier = Modifier.weight(1f),
            decorationBox = { inner ->
                if (key.isEmpty()) Text(if (vm.hasKey) "Đã lưu key · dán key mới để thay" else "Dán API key để dùng \"Dịch với Claude\"", color = C.Dim, fontSize = 14.sp)
                inner()
            },
        )
        IconButton(onClick = { show = !show }) {
            Icon(if (show) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility, contentDescription = "Hiện/ẩn key", tint = C.Muted)
        }
        TextButton(onClick = { vm.saveKey(key); key = "" }, enabled = !vm.keyBusy && key.isNotBlank()) {
            Text(if (vm.keyBusy) "Đang kiểm tra…" else "Lưu", color = if (key.isNotBlank()) C.RedSoft else C.Dim, fontWeight = FontWeight.SemiBold)
        }
    }
    val st = vm.keyStatus
    when {
        st != null -> Hint(st.second, if (st.first) C.Green else C.Red)
        vm.hasKey -> Hint("✓ Đã có API key, nút Dịch với Claude đã sẵn sàng", C.Green)
        else -> Hint("Không nhập key thì MTrans dịch bằng Google (và offline khi mất mạng). Lấy key ở console.anthropic.com → API Keys.")
    }
    if (vm.hasKey) {
        Text(
            "Xoá key", color = C.Muted, fontSize = 12.sp,
            modifier = Modifier.padding(top = 6.dp).clickable { vm.saveKey("") },
        )
    }
}

@Composable
private fun ModelPicker(vm: AppViewModel) {
    var open by remember { mutableStateOf(false) }
    val label = Prefs.MODELS.firstOrNull { it.first == vm.model }?.second ?: vm.model
    Box {
        Row(
            Modifier.fillMaxWidth().height(48.dp).clip(RoundedCornerShape(10.dp)).background(C.Panel)
                .border(1.dp, if (open) C.Red else C.Line, RoundedCornerShape(10.dp)).clickable { open = true }
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(label, color = Color.White, fontSize = 14.sp)
            Icon(Icons.Filled.KeyboardArrowDown, null, tint = C.Muted)
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }, containerColor = Color(0xFF2B2E35)) {
            Prefs.MODELS.forEach { (id, name) ->
                DropdownMenuItem(text = { Text(name, color = C.Text) }, onClick = { open = false; vm.chooseModel(id) })
            }
        }
    }
}
