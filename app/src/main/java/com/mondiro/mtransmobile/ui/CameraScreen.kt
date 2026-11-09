package com.mondiro.mtransmobile.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.geometry.Offset
import android.graphics.Rect
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FlashOff
import androidx.compose.material.icons.outlined.FlashOn
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.Replay
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.mondiro.mtransmobile.AppViewModel
import com.mondiro.mtransmobile.Engine
import com.mondiro.mtransmobile.Lang

@Composable
fun CameraScreen(vm: AppViewModel, wide: Boolean) {
    val ctx = LocalContext.current
    val pick = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) loadImage(ctx, uri)?.let(vm::onPhoto) ?: run { vm.updateMsg = "Không mở được ảnh" }
    }
    val openGallery = { pick.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }

    val photo = vm.photo
    if (wide) {
        Row(Modifier.fillMaxSize()) {
            Box(Modifier.weight(1f).fillMaxHeight().background(Color(0xFF0D0E10))) {
                if (photo == null) Viewfinder(vm, openGallery) else PhotoView(vm, photo, showToggle = false)
            }
            Box(Modifier.width(1.dp).fillMaxHeight().background(C.Line))
            Box(Modifier.weight(1f).fillMaxHeight()) {
                if (photo == null) {
                    Text(
                        "Chụp ảnh có chữ (biển hiệu, menu, sách…) hoặc chọn ảnh có sẵn.\nMTrans tự nhận ra Tiếng Anh / Tiếng Trung và dịch sang Tiếng Việt.",
                        color = C.Muted, fontSize = 14.sp, modifier = Modifier.align(Alignment.Center).padding(24.dp),
                    )
                } else ResultList(vm, Modifier.fillMaxSize().padding(top = 4.dp, start = 16.dp, end = 16.dp))
                Credit(Modifier.align(Alignment.BottomEnd).padding(end = 14.dp, bottom = 4.dp))
            }
        }
    } else {
        when {
            photo == null -> Viewfinder(vm, openGallery)
            vm.overlay -> PhotoView(vm, photo, showToggle = true)
            else -> Column(Modifier.fillMaxSize()) {
                Box(Modifier.weight(0.52f).fillMaxWidth().background(Color(0xFF0D0E10))) { PhotoView(vm, photo, showToggle = false) }
                Box(Modifier.width(1.dp))
                ResultList(vm, Modifier.weight(0.48f).fillMaxWidth().padding(top = 10.dp, start = 14.dp, end = 14.dp))
            }
        }
    }
}

// ---------------------------------------------------------------- viewfinder

@Composable
private fun Viewfinder(vm: AppViewModel, openGallery: () -> Unit) {
    val ctx = LocalContext.current
    var granted by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(ctx, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }
    val ask = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted = it }
    LaunchedEffect(Unit) { if (!granted) ask.launch(Manifest.permission.CAMERA) }

    Box(Modifier.fillMaxSize().background(Color(0xFF0D0E10))) {
        var flash by remember { mutableStateOf(false) }
        val capture = remember {
            ImageCapture.Builder().setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY).build()
        }
        LaunchedEffect(flash) { capture.flashMode = if (flash) ImageCapture.FLASH_MODE_ON else ImageCapture.FLASH_MODE_OFF }
        var shooting by remember { mutableStateOf(false) }

        if (granted) {
            CameraPreview(capture)
        } else {
            Column(Modifier.align(Alignment.Center).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("MTrans cần quyền camera để chụp chữ cần dịch.", color = C.Text, fontSize = 15.sp)
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = { ask.launch(Manifest.permission.CAMERA) },
                    colors = ButtonDefaults.buttonColors(containerColor = C.Red),
                ) { Text("Cho phép camera") }
                TextButton(onClick = openGallery) { Text("Hoặc chọn ảnh có sẵn", color = C.RedSoft) }
            }
        }

        // Framing guide
        if (granted) {
            Box(
                Modifier.fillMaxSize().padding(start = 26.dp, end = 26.dp, top = 70.dp, bottom = 150.dp)
                    .border(2.dp, Color.White.copy(alpha = .5f), RoundedCornerShape(14.dp)),
            )
        }

        Row(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(120.dp)
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = .65f)))),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RoundIcon(Icons.Outlined.PhotoLibrary, "Chọn ảnh", openGallery)
            Box(
                Modifier.size(74.dp).clip(CircleShape).border(4.dp, Color.White, CircleShape)
                    .clickable(enabled = granted && !shooting) {
                        shooting = true
                        capture.takePicture(ContextCompat.getMainExecutor(ctx), object : ImageCapture.OnImageCapturedCallback() {
                            override fun onCaptureSuccess(image: ImageProxy) {
                                val rot = image.imageInfo.rotationDegrees
                                val bmp = image.toBitmap()
                                image.close()
                                shooting = false
                                vm.onPhoto(rotate(bmp, rot))
                            }

                            override fun onError(exception: ImageCaptureException) {
                                shooting = false
                                vm.updateMsg = "Chụp ảnh lỗi, thử lại"
                            }
                        })
                    },
                contentAlignment = Alignment.Center,
            ) {
                Box(Modifier.size(58.dp).clip(CircleShape).background(if (shooting) C.Red.copy(alpha = .5f) else C.Red))
            }
            RoundIcon(if (flash) Icons.Outlined.FlashOn else Icons.Outlined.FlashOff, "Đèn flash") { flash = !flash }
        }
    }
}

@Composable
private fun CameraPreview(capture: ImageCapture) {
    val ctx = LocalContext.current
    val owner = LocalLifecycleOwner.current
    val view = remember { PreviewView(ctx).apply { scaleType = PreviewView.ScaleType.FILL_CENTER } }
    DisposableEffect(owner) {
        val future = ProcessCameraProvider.getInstance(ctx)
        var provider: ProcessCameraProvider? = null
        future.addListener({
            try {
                val p = future.get()
                provider = p
                val preview = Preview.Builder().build()
                preview.setSurfaceProvider(view.surfaceProvider)
                p.unbindAll()
                p.bindToLifecycle(owner, CameraSelector.DEFAULT_BACK_CAMERA, preview, capture)
            } catch (_: Exception) {
            }
        }, ContextCompat.getMainExecutor(ctx))
        onDispose { provider?.unbindAll() }
    }
    AndroidView(factory = { view }, modifier = Modifier.fillMaxSize())
}

@Composable
private fun RoundIcon(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    IconButton(onClick = onClick, modifier = Modifier.size(48.dp).clip(CircleShape).background(Color.White.copy(alpha = .14f))) {
        Icon(icon, contentDescription = label, tint = Color.White)
    }
}

// ---------------------------------------------------------------- captured photo

@Composable
private fun PhotoView(vm: AppViewModel, photo: Bitmap, showToggle: Boolean) {
    var peek by remember { mutableStateOf(false) } // overlay: holding a finger shows the original text
    var dragFrom by remember { mutableStateOf<Offset?>(null) }
    var dragTo by remember { mutableStateOf<Offset?>(null) }
    val density = LocalDensity.current
    BoxWithConstraints(Modifier.fillMaxSize().background(Color(0xFF0D0E10))) {
        val w = constraints.maxWidth.toFloat()
        val h = constraints.maxHeight.toFloat()
        val s = minOf(w / photo.width, h / photo.height)
        val dx = (w - photo.width * s) / 2f
        val dy = (h - photo.height * s) / 2f
        fun toImage(o: Offset) = Offset((o.x - dx) / s, (o.y - dy) / s)
        val image = remember(photo) { photo.asImageBitmap() }
        val overlay = vm.overlay
        Image(
            image, contentDescription = null, contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize()
                .pointerInput(overlay, vm.items) {
                    if (overlay) {
                        detectTapGestures(onPress = { peek = true; tryAwaitRelease(); peek = false })
                    } else {
                        // Tap a box to add/remove it from the selection.
                        detectTapGestures { pos ->
                            val p = toImage(pos)
                            val i = vm.items.indexOfFirst { it.box?.contains(p.x.toInt(), p.y.toInt()) == true }
                            if (i >= 0) vm.toggleBlock(i)
                        }
                    }
                }
                .pointerInput(overlay, vm.items) {
                    if (!overlay) {
                        // Drag to draw a selection rectangle over the text to translate.
                        detectDragGestures(
                            onDragStart = { dragFrom = it; dragTo = it },
                            onDrag = { change, amount -> change.consume(); dragTo = (dragTo ?: change.position) + amount },
                            onDragEnd = {
                                val a = dragFrom
                                val b = dragTo
                                if (a != null && b != null) {
                                    val p1 = toImage(a)
                                    val p2 = toImage(b)
                                    vm.pickRegion(
                                        Rect(
                                            minOf(p1.x, p2.x).toInt(), minOf(p1.y, p2.y).toInt(),
                                            maxOf(p1.x, p2.x).toInt(), maxOf(p1.y, p2.y).toInt(),
                                        )
                                    )
                                }
                                dragFrom = null
                                dragTo = null
                            },
                            onDragCancel = { dragFrom = null; dragTo = null },
                        )
                    }
                },
        )
        vm.items.forEachIndexed { i, item ->
            val box = item.box ?: return@forEachIndexed
            val x = dx + box.left * s
            val y = dy + box.top * s
            val bw = with(density) { (box.width() * s).toDp() }
            val bh = with(density) { (box.height() * s).toDp() }
            val pos = Modifier.offset { IntOffset(x.toInt(), y.toInt()) }.size(bw, bh)
            if (overlay && !peek) {
                val tr = item.translated
                if (tr != null) {
                    val lines = (tr.length / 18 + 1).coerceAtMost(6)
                    val fs = with(density) { (box.height() * s / lines * 0.62f).toSp() }.value.coerceIn(9f, 26f)
                    Box(
                        pos.clip(RoundedCornerShape(4.dp)).background(C.Cream).padding(horizontal = 4.dp),
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        Text(tr, color = Color(0xFF1A1A1A), fontSize = fs.sp, lineHeight = (fs * 1.2f).sp, fontWeight = FontWeight.SemiBold, overflow = TextOverflow.Ellipsis)
                    }
                }
            } else if (!overlay) {
                val on = i in vm.picked || (vm.showAll && i == vm.selected)
                Box(
                    pos.border(if (on) 2.dp else 1.5.dp, C.Red.copy(alpha = if (on) 1f else .6f), RoundedCornerShape(5.dp))
                        .background(C.Red.copy(alpha = if (on) .24f else .05f), RoundedCornerShape(5.dp)),
                )
            }
        }
        // Rectangle being dragged
        val a = dragFrom
        val b = dragTo
        if (a != null && b != null) {
            val left = minOf(a.x, b.x)
            val top = minOf(a.y, b.y)
            Box(
                Modifier.offset { IntOffset(left.toInt(), top.toInt()) }
                    .size(with(density) { kotlin.math.abs(b.x - a.x).toDp() }, with(density) { kotlin.math.abs(b.y - a.y).toDp() })
                    .border(2.dp, C.Red, RoundedCornerShape(3.dp))
                    .background(C.Red.copy(alpha = .15f)),
            )
        }
        DetectChip(vm, Modifier.align(Alignment.TopStart).padding(10.dp))
        IconButton(
            onClick = vm::retake,
            modifier = Modifier.align(Alignment.TopEnd).padding(8.dp).size(42.dp).clip(CircleShape).background(C.Chrome.copy(alpha = .85f)),
        ) { Icon(Icons.Outlined.Replay, contentDescription = "Chụp lại", tint = Color.White) }
        if (vm.camBusy) {
            Row(
                Modifier.align(Alignment.Center).clip(RoundedCornerShape(20.dp)).background(C.Chrome.copy(alpha = .9f))
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CircularProgressIndicator(color = C.Red, strokeWidth = 2.dp, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text(if (vm.items.isEmpty()) "Đang đọc chữ…" else "Đang dịch…", color = C.Text, fontSize = 13.sp)
            }
        }
        if (showToggle) {
            Column(
                Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp).fillMaxWidth(0.86f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                // Overlay mode: who translated + the Claude button
                Row(
                    Modifier.clip(RoundedCornerShape(20.dp)).background(C.Chrome.copy(alpha = .9f)).padding(start = 10.dp, end = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    EngineChip(vm.camEngine)
                    if (vm.camEngine == Engine.CLAUDE) {
                        Text("  Bản dịch của Claude", color = C.Muted, fontSize = 12.sp, modifier = Modifier.padding(end = 10.dp, top = 8.dp, bottom = 8.dp))
                    } else {
                        ClaudeButton(vm)
                    }
                }
                val err = vm.camError
                if (err != null) {
                    Text(err, color = C.Warn, fontSize = 12.sp, modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(C.Chrome.copy(alpha = .9f)).padding(horizontal = 10.dp, vertical = 4.dp))
                }
                Segmented(listOf("Danh sách", "Đè lên ảnh"), 1, { vm.setOverlayMode(it == 1) }, Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun ClaudeButton(vm: AppViewModel, label: String = "Dịch với Claude") {
    if (vm.alwaysOffline || vm.photo == null) return
    TextButton(onClick = {
        if (vm.hasKey) vm.claudeCamera() else vm.updateMsg = "Nhập API key trong Cài đặt để dịch với Claude"
    }) { Text(label, color = C.RedSoft, fontWeight = FontWeight.SemiBold, fontSize = 13.sp) }
}

@Composable
private fun DetectChip(vm: AppViewModel, modifier: Modifier) {
    val d = vm.detected ?: return
    val targets = Lang.targetsFor(d)
    Row(
        modifier.clip(RoundedCornerShape(20.dp)).background(C.Chrome.copy(alpha = .9f))
            .border(1.dp, C.Line, RoundedCornerShape(20.dp))
            .clickable { vm.pickCamTarget(targets[(targets.indexOf(vm.camTarget) + 1) % targets.size]) }
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("Đã nhận ra: ", color = C.Text, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        Text(d.label, color = C.Red, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Text("  →  ${vm.camTarget.label}", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun ResultList(vm: AppViewModel, modifier: Modifier) {
    val list = rememberLazyListState()
    LaunchedEffect(vm.selected) { if (vm.selected >= 0 && vm.showAll) list.animateScrollToItem(vm.selected) }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Segmented(listOf("Danh sách", "Đè lên ảnh"), if (vm.overlay) 1 else 0, { vm.setOverlayMode(it == 1) }, Modifier.fillMaxWidth())
        val err = vm.camError
        if (err != null) Text(err, color = C.Warn, fontSize = 13.sp)
        val pick = vm.pickResult
        when {
            vm.picked.isNotEmpty() -> {
                // The picked region, translated as one passage
                Column(
                    Modifier.weight(1f).fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(C.SelectedBg)
                        .border(1.dp, C.Red, RoundedCornerShape(12.dp)).padding(horizontal = 12.dp, vertical = 10.dp)
                        .verticalScroll(rememberScrollState()),
                ) {
                    Text("Vùng đã chọn · ${vm.picked.size} đoạn", color = C.Muted, fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    Text(pick?.original ?: "", color = C.Muted, fontSize = 13.sp)
                    Spacer(Modifier.height(6.dp))
                    when {
                        pick?.translated != null -> SelectionContainer {
                            Text(pick.translated, color = Color.White.copy(alpha = if (vm.pickBusy) .5f else 1f), fontSize = 16.sp, lineHeight = 23.sp)
                        }
                        vm.pickBusy -> CircularProgressIndicator(color = C.Red, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
                    }
                }
                Row(Modifier.fillMaxWidth().padding(bottom = 18.dp), verticalAlignment = Alignment.CenterVertically) {
                    EngineChip(vm.pickEngine)
                    TextButton(onClick = vm::clearPick) { Text("Bỏ chọn", color = C.Muted, fontSize = 13.sp) }
                    Spacer(Modifier.weight(1f))
                    if (vm.pickEngine != Engine.CLAUDE) ClaudeButton(vm)
                }
            }
            vm.showAll -> {
                LazyColumn(Modifier.weight(1f), state = list, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    itemsIndexed(vm.items) { i, item ->
                        val sel = i == vm.selected
                        Column(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                                .background(if (sel) C.SelectedBg else C.Panel)
                                .border(1.dp, if (sel) C.Red else C.Line, RoundedCornerShape(12.dp))
                                .clickable { vm.selected = if (sel) -1 else i }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                        ) {
                            Text(item.original, color = C.Muted, fontSize = 12.5.sp, maxLines = if (sel) 20 else 3, overflow = TextOverflow.Ellipsis)
                            Spacer(Modifier.height(2.dp))
                            when {
                                item.translated != null -> Text(item.translated, color = Color.White, fontSize = 15.5.sp, lineHeight = 22.sp)
                                item.failed -> Text("Chưa dịch được đoạn này", color = C.Warn, fontSize = 13.sp)
                                else -> Text("…", color = C.Muted, fontSize = 15.sp)
                            }
                        }
                    }
                }
                Row(Modifier.fillMaxWidth().padding(bottom = 18.dp), verticalAlignment = Alignment.CenterVertically) {
                    EngineChip(vm.camEngine)
                    Spacer(Modifier.weight(1f))
                    if (vm.camEngine != Engine.CLAUDE) ClaudeButton(vm)
                }
            }
            else -> {
                // Nothing picked yet: explain how to pick
                Column(
                    Modifier.weight(1f).fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(C.Panel)
                        .border(1.dp, C.Line, RoundedCornerShape(12.dp)).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    if (vm.items.isEmpty()) {
                        Text(if (vm.camBusy) "Đang đọc chữ trong ảnh…" else "Chưa thấy chữ nào.", color = C.Muted, fontSize = 14.sp)
                    } else {
                        Text("Kéo ngón tay trên ảnh để khoanh vùng chữ cần dịch", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                        Text("Hoặc chạm vào từng khung đỏ để chọn / bỏ chọn. Các đoạn đã chọn được dịch chung thành 1 đoạn.", color = C.Muted, fontSize = 13.sp)
                        Button(
                            onClick = vm::translateAll,
                            colors = ButtonDefaults.buttonColors(containerColor = C.Red), shape = RoundedCornerShape(9.dp),
                        ) { Text("Dịch tất cả (${vm.items.size} đoạn)") }
                    }
                }
                Row(Modifier.fillMaxWidth().padding(bottom = 18.dp), verticalAlignment = Alignment.CenterVertically) {
                    Spacer(Modifier.weight(1f))
                    ClaudeButton(vm, if (vm.items.isEmpty()) "Dịch với Claude" else "Dịch tất cả với Claude")
                }
            }
        }
    }
}

// ---------------------------------------------------------------- helpers

private fun rotate(b: Bitmap, degrees: Int): Bitmap {
    if (degrees == 0) return b
    val m = Matrix().apply { postRotate(degrees.toFloat()) }
    return Bitmap.createBitmap(b, 0, 0, b.width, b.height, m, true)
}

private fun loadImage(ctx: Context, uri: Uri): Bitmap? = try {
    if (Build.VERSION.SDK_INT >= 28) {
        ImageDecoder.decodeBitmap(ImageDecoder.createSource(ctx.contentResolver, uri)) { decoder, info, _ ->
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            val m = maxOf(info.size.width, info.size.height)
            if (m > 2400) decoder.setTargetSampleSize(m / 2000)
        }
    } else {
        ctx.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
    }
} catch (e: Exception) {
    null
}
