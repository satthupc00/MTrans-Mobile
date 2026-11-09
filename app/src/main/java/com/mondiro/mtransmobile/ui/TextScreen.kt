package com.mondiro.mtransmobile.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mondiro.mtransmobile.AppViewModel
import com.mondiro.mtransmobile.Lang

@Composable
fun TextScreen(vm: AppViewModel, wide: Boolean) {
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().padding(start = 14.dp, end = 14.dp, bottom = 22.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                LangPicker("Dịch từ", vm.from, Lang.entries, vm::pickFrom, Modifier.weight(1f))
                SwapButton(vm::swap)
                LangPicker("Dịch sang", vm.to, Lang.targetsFor(vm.from), vm::pickTo, Modifier.weight(1f))
            }
            if (wide) {
                Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    InputCard(vm, Modifier.weight(1f).fillMaxSize())
                    ResultCard(vm, Modifier.weight(1f).fillMaxSize())
                }
            } else {
                InputCard(vm, Modifier.fillMaxWidth().weight(1f))
                ResultCard(vm, Modifier.fillMaxWidth().weight(1f))
            }
        }
        Credit(Modifier.align(Alignment.BottomEnd).padding(end = 14.dp, bottom = 4.dp))
    }
}

@Composable
private fun InputCard(vm: AppViewModel, modifier: Modifier) {
    val ctx = LocalContext.current
    Box(modifier.clip(RoundedCornerShape(14.dp)).background(C.Panel).border(1.dp, C.Line, RoundedCornerShape(14.dp))) {
        BasicTextField(
            value = vm.input,
            onValueChange = vm::onInput,
            textStyle = TextStyle(color = Color.White, fontSize = 17.sp, lineHeight = 25.sp),
            cursorBrush = SolidColor(C.Red),
            modifier = Modifier.fillMaxSize().padding(start = 14.dp, end = 14.dp, top = 12.dp, bottom = 48.dp),
            decorationBox = { inner ->
                if (vm.input.isEmpty()) Text("Nhập hoặc dán chữ cần dịch…", color = C.Dim, fontSize = 17.sp)
                inner()
            },
        )
        Row(Modifier.align(Alignment.BottomEnd).padding(4.dp)) {
            IconButton(onClick = {
                val cb = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val t = cb.primaryClip?.getItemAt(0)?.coerceToText(ctx)?.toString().orEmpty()
                if (t.isNotBlank()) vm.onInput(t)
            }) { Icon(Icons.Outlined.ContentPaste, contentDescription = "Dán", tint = C.Muted) }
            if (vm.input.isNotEmpty()) {
                IconButton(onClick = vm::clearInput) { Icon(Icons.Outlined.Close, contentDescription = "Xoá", tint = C.Muted) }
            }
        }
    }
}

@Composable
private fun ResultCard(vm: AppViewModel, modifier: Modifier) {
    val ctx = LocalContext.current
    Column(modifier.clip(RoundedCornerShape(14.dp)).background(C.Result).border(1.dp, C.Line, RoundedCornerShape(14.dp))) {
        Box(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 14.dp, vertical = 12.dp)) {
            when {
                vm.error != null -> Text(vm.error!!, color = C.Warn, fontSize = 15.sp)
                vm.output.isNotEmpty() -> SelectionContainer {
                    Text(vm.output, color = Color.White.copy(alpha = if (vm.busy) .5f else 1f), fontSize = 17.sp, lineHeight = 25.sp)
                }
                vm.busy -> CircularProgressIndicator(color = C.Red, strokeWidth = 2.dp, modifier = Modifier.size(22.dp))
                else -> Text("Bản dịch sẽ hiện ở đây", color = C.Dim, fontSize = 15.sp)
            }
        }
        Row(Modifier.fillMaxWidth().padding(start = 12.dp, end = 4.dp, bottom = 2.dp), verticalAlignment = Alignment.CenterVertically) {
            EngineChip(vm.engine)
            if (vm.engine == com.mondiro.mtransmobile.Engine.OFFLINE && vm.online && !vm.alwaysOffline) {
                Text("  Mạng chập chờn, đã dịch offline", fontSize = 11.5.sp, color = C.Dim)
            }
            Spacer(Modifier.weight(1f))
            if (vm.output.isNotEmpty()) {
                IconButton(onClick = {
                    val cb = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    cb.setPrimaryClip(ClipData.newPlainText("MTrans", vm.output))
                    vm.updateMsg = "Đã copy bản dịch"
                }) { Icon(Icons.Outlined.ContentCopy, contentDescription = "Copy", tint = C.Muted, modifier = Modifier.size(20.dp)) }
            }
            if (vm.input.isNotBlank() && !vm.alwaysOffline && vm.engine != com.mondiro.mtransmobile.Engine.CLAUDE) {
                TextButton(onClick = {
                    if (vm.hasKey) vm.claudeText() else vm.updateMsg = "Nhập API key trong Cài đặt để dịch với Claude"
                }) { Text("Dịch với Claude", color = C.RedSoft, fontWeight = FontWeight.SemiBold, fontSize = 13.sp) }
            }
        }
    }
}
