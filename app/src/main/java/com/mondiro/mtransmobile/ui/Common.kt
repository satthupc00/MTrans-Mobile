package com.mondiro.mtransmobile.ui

import android.content.Context
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mondiro.mtransmobile.AppViewModel
import com.mondiro.mtransmobile.Engine
import com.mondiro.mtransmobile.Lang
import org.json.JSONObject

/** Thin row at the top, icons on the right (Mondiro Android rule): offline chip, latest, update. */
@Composable
fun TopRow(vm: AppViewModel) {
    var askInstall by remember { mutableStateOf(false) }
    Row(
        Modifier.fillMaxWidth().height(44.dp).padding(horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (!vm.online || vm.alwaysOffline) {
            Row(
                Modifier.padding(start = 8.dp).clip(RoundedCornerShape(20.dp)).background(C.Warn.copy(alpha = .12f))
                    .padding(horizontal = 9.dp, vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.size(6.dp).clip(CircleShape).background(C.Warn))
                Spacer(Modifier.width(5.dp))
                Text("Offline", color = C.Warn, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.weight(1f))
        val u = vm.update
        if (u != null && !u.hasUpdate) LatestBadge()
        val dl = vm.downloadProgress
        if (dl != null) {
            Text(
                "Đang tải ${(dl * 100).toInt()}%", color = C.Green, fontSize = 11.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(end = 2.dp),
            )
        } else if (u != null && u.hasUpdate) {
            Text(
                "v${u.latest}", color = C.Green, fontSize = 11.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(end = 2.dp),
            )
        }
        val spin = rememberInfiniteTransition(label = "spin")
        val angle by spin.animateFloat(0f, 360f, infiniteRepeatable(tween(800, easing = LinearEasing)), label = "a")
        val pulse by spin.animateFloat(1f, 1.18f, infiniteRepeatable(tween(700), RepeatMode.Reverse), label = "p")
        IconButton(
            onClick = { if (u?.hasUpdate == true) askInstall = true else vm.checkUpdate(manual = true) },
            modifier = Modifier.size(40.dp),
        ) {
            Icon(
                Icons.Filled.Refresh, contentDescription = "Kiểm tra cập nhật",
                tint = if (u != null) C.Green else C.Muted,
                modifier = Modifier.size(21.dp)
                    .then(if (vm.checking || vm.downloadProgress != null) Modifier.rotate(angle) else Modifier)
                    .then(if (u?.hasUpdate == true) Modifier.scale(pulse) else Modifier),
            )
        }
    }
    if (askInstall) {
        val ctx = androidx.compose.ui.platform.LocalContext.current
        AlertDialog(
            onDismissRequest = { askInstall = false },
            confirmButton = {
                TextButton(onClick = { askInstall = false; vm.installUpdate(ctx) }) { Text("Cập nhật", color = C.Red) }
            },
            dismissButton = { TextButton(onClick = { askInstall = false }) { Text("Để sau", color = C.Muted) } },
            title = { Text("Có bản mới v${vm.update?.latest}") },
            text = { Text("Tải và cài bản mới ngay? Ảnh và bản dịch đang xem sẽ được đóng lại.") },
            containerColor = C.Chrome,
        )
    }
}

@Composable
fun LatestBadge() {
    Text(
        "latest", color = C.Green, fontSize = 10.5.sp, fontWeight = FontWeight.ExtraBold,
        modifier = Modifier.clip(RoundedCornerShape(20.dp)).background(C.Green.copy(alpha = .14f))
            .padding(horizontal = 7.dp, vertical = 1.dp),
    )
}

@Composable
fun Credit(modifier: Modifier = Modifier) {
    Text("Created by Mondiro", color = Color(0xFF5D616A), fontSize = 10.sp, modifier = modifier)
}

@Composable
fun EngineChip(e: Engine?) {
    if (e == null) return
    val off = e == Engine.OFFLINE
    Text(
        e.label, fontSize = 11.sp, color = if (off) C.Warn else C.Muted,
        modifier = Modifier.border(1.dp, if (off) C.Warn.copy(alpha = .4f) else C.Line, RoundedCornerShape(20.dp))
            .padding(horizontal = 8.dp, vertical = 1.dp),
    )
}

/** "Dịch từ" / "Dịch sang" picker with a dropdown. */
@Composable
fun LangPicker(label: String, value: Lang, options: List<Lang>, onPick: (Lang) -> Unit, modifier: Modifier = Modifier) {
    var open by remember { mutableStateOf(false) }
    Box(modifier) {
        Column(
            Modifier.fillMaxWidth().height(50.dp).clip(RoundedCornerShape(12.dp)).background(C.Panel)
                .border(1.dp, if (open) C.Red else C.Line, RoundedCornerShape(12.dp))
                .clickable { open = true }.padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(label, fontSize = 10.5.sp, color = C.Muted, lineHeight = 12.sp)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(value.label, fontSize = 14.5.sp, color = Color.White, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                Icon(Icons.Filled.KeyboardArrowDown, null, tint = C.Muted, modifier = Modifier.size(18.dp))
            }
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }, containerColor = Color(0xFF2B2E35)) {
            options.forEach { l ->
                DropdownMenuItem(
                    text = { Text(l.label, color = if (l == value) Color.White else C.Text, fontWeight = if (l == value) FontWeight.SemiBold else FontWeight.Normal) },
                    onClick = { open = false; onPick(l) },
                    modifier = if (l == value) Modifier.background(C.SelectedBg) else Modifier,
                )
            }
        }
    }
}

@Composable
fun SwapButton(onClick: () -> Unit) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(46.dp).clip(CircleShape).background(C.Panel).border(1.dp, C.Line, CircleShape),
    ) { Icon(Icons.Filled.SwapHoriz, contentDescription = "Đảo chiều", tint = Color.White) }
}

@Composable
fun Segmented(options: List<String>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier.clip(RoundedCornerShape(10.dp)).background(C.Panel).border(1.dp, C.Line, RoundedCornerShape(10.dp)).padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        options.forEachIndexed { i, label ->
            val on = i == selected
            Box(
                Modifier.weight(1f).height(36.dp).clip(RoundedCornerShape(7.dp))
                    .background(if (on) C.SelectedBg else Color.Transparent)
                    .then(if (on) Modifier.border(1.dp, C.Red, RoundedCornerShape(7.dp)) else Modifier)
                    .clickable { onSelect(i) },
                contentAlignment = Alignment.Center,
            ) { Text(label, color = if (on) Color.White else C.Muted, fontSize = 13.sp, fontWeight = FontWeight.SemiBold) }
        }
    }
}

/** What's new dialog, read from the bundled whatsnew.json. */
@Composable
fun WhatsNewDialog(context: Context, onClose: () -> Unit) {
    val data = remember {
        try {
            JSONObject(context.assets.open("whatsnew.json").bufferedReader().use { it.readText() })
        } catch (e: Exception) {
            null
        }
    }
    AlertDialog(
        onDismissRequest = onClose,
        confirmButton = { TextButton(onClick = onClose) { Text("Đóng", color = C.Red) } },
        title = {
            Column {
                Text("What's new", fontWeight = FontWeight.Bold)
                val d = data?.optString("date").orEmpty().split("-").reversed().joinToString("/")
                Text("v${data?.optString("version")}  ·  $d", fontSize = 12.sp, color = C.Muted)
            }
        },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                listOf("new" to "Mới", "improved" to "Cải thiện", "fixed" to "Sửa lỗi").forEach { (k, title) ->
                    val arr = data?.optJSONArray(k)
                    if (arr != null && arr.length() > 0) {
                        Text(title.uppercase(), color = C.Red, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 10.dp, bottom = 4.dp))
                        for (i in 0 until arr.length()) Text("•  " + arr.getString(i), color = C.Text, fontSize = 14.sp, modifier = Modifier.padding(vertical = 2.dp))
                    }
                }
            }
        },
        containerColor = C.Chrome,
    )
}
