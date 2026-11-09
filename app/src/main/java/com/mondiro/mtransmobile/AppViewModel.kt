package com.mondiro.mtransmobile

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Rect
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

enum class Tab { CAMERA, TEXT, SETTINGS }

enum class Engine(val label: String) { GOOGLE("Google"), OFFLINE("Offline"), CLAUDE("Claude") }

enum class PackState { READY, DOWNLOADING, MISSING }

data class CamItem(val original: String, val box: Rect?, val translated: String? = null, val failed: Boolean = false)

/** All app state lives here so folding/unfolding the phone keeps it. */
class AppViewModel(app: Application) : AndroidViewModel(app) {
    val prefs = Prefs(app)
    val version: String = BuildConfig.VERSION_NAME

    var tab by mutableStateOf(Tab.TEXT)

    // ---------- network ----------
    var online by mutableStateOf(true)
        private set

    // ---------- text ----------
    var from by mutableStateOf(prefs.from)
        private set
    var to by mutableStateOf(if (prefs.to == prefs.from) Lang.defaultTarget(prefs.from) else prefs.to)
        private set
    var input by mutableStateOf("")
        private set
    var output by mutableStateOf("")
        private set
    var engine by mutableStateOf<Engine?>(null)
        private set
    var busy by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    private var textJob: Job? = null

    // ---------- camera ----------
    var photo by mutableStateOf<Bitmap?>(null)
        private set
    var items by mutableStateOf<List<CamItem>>(emptyList())
        private set
    var detected by mutableStateOf<Lang?>(null)
        private set
    var camTarget by mutableStateOf(Lang.VI)
        private set
    var selected by mutableStateOf(-1)
    var overlay by mutableStateOf(prefs.overlayMode)
        private set
    var camBusy by mutableStateOf(false)
        private set
    var camError by mutableStateOf<String?>(null)
        private set
    var camEngine by mutableStateOf<Engine?>(null)
        private set
    private var camJob: Job? = null

    /** List mode: the blocks the user picked (by dragging a region or tapping boxes). */
    var picked by mutableStateOf<Set<Int>>(emptySet())
        private set
    /** Translation of the picked blocks, read together as one passage. */
    var pickResult by mutableStateOf<CamItem?>(null)
        private set
    var pickEngine by mutableStateOf<Engine?>(null)
        private set
    var pickBusy by mutableStateOf(false)
        private set
    /** List mode shows every block (after "Dịch tất cả"). */
    var showAll by mutableStateOf(false)
        private set
    private var pickJob: Job? = null

    // ---------- settings ----------
    var alwaysOffline by mutableStateOf(prefs.alwaysOffline)
        private set
    var model by mutableStateOf(prefs.model)
        private set
    var hasKey by mutableStateOf(prefs.apiKey.isNotEmpty())
        private set
    var keyStatus by mutableStateOf<Pair<Boolean, String>?>(null)
    var keyBusy by mutableStateOf(false)
        private set
    val packs = mutableStateMapOf<Lang, PackState>()

    // ---------- update ----------
    var update by mutableStateOf<UpdateInfo?>(null)
        private set
    var checking by mutableStateOf(false)
        private set
    var updateMsg by mutableStateOf<String?>(null)

    val claudeReady get() = hasKey && !alwaysOffline && online

    init {
        watchNetwork()
        refreshPacks()
        viewModelScope.launch {
            delay(1500)
            checkUpdate(manual = false)
        }
    }

    private fun watchNetwork() {
        val cm = getApplication<Application>().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        fun current(): Boolean {
            val caps = cm.getNetworkCapabilities(cm.activeNetwork) ?: return false
            return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        }
        online = current()
        cm.registerDefaultNetworkCallback(object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                viewModelScope.launch { online = true }
            }

            override fun onLost(network: Network) {
                viewModelScope.launch { online = current() }
            }
        })
    }

    /** Google when allowed and reachable, otherwise the on-device translator. */
    private suspend fun smart(text: String, src: Lang, dst: Lang): Pair<String, Engine> {
        if (!alwaysOffline && online) {
            try {
                return GoogleTranslate.translate(text, src, dst) to Engine.GOOGLE
            } catch (_: Exception) {
                // fall back to offline
            }
        }
        return OfflineTranslate.translate(text, src, dst, mayDownload = online) to Engine.OFFLINE
    }

    // ---------- text actions ----------

    fun pickFrom(l: Lang) {
        from = l
        if (to == l) to = Lang.defaultTarget(l)
        prefs.from = from
        prefs.to = to
        retranslate()
    }

    fun pickTo(l: Lang) {
        if (l == from) return
        to = l
        prefs.to = to
        retranslate()
    }

    fun swap() {
        val f = from
        from = to
        to = f
        prefs.from = from
        prefs.to = to
        if (output.isNotBlank()) input = output
        retranslate()
    }

    fun onInput(t: String) {
        input = t
        textJob?.cancel()
        if (t.isBlank()) {
            output = ""
            engine = null
            error = null
            busy = false
            return
        }
        textJob = viewModelScope.launch {
            delay(600)
            runText()
        }
    }

    fun clearInput() = onInput("")

    private fun retranslate() {
        textJob?.cancel()
        if (input.isBlank()) return
        textJob = viewModelScope.launch { runText() }
    }

    private suspend fun runText() {
        val text = input
        busy = true
        error = null
        try {
            val (out, e) = smart(text, from, to)
            if (text == input) {
                output = out
                engine = e
            }
        } catch (e: Exception) {
            error = e.message ?: "Có lỗi khi dịch"
        } finally {
            busy = false
        }
    }

    fun claudeText() {
        val key = prefs.apiKey
        if (key.isEmpty() || input.isBlank()) return
        textJob?.cancel()
        textJob = viewModelScope.launch {
            busy = true
            error = null
            try {
                output = ClaudeService.translateText(key, model, input, from, to)
                engine = Engine.CLAUDE
            } catch (e: Exception) {
                error = e.message
            } finally {
                busy = false
            }
        }
    }

    // ---------- camera actions ----------

    fun onPhoto(bitmap: Bitmap) {
        photo = scaleDown(bitmap, 2000)
        items = emptyList()
        detected = null
        selected = -1
        camError = null
        camEngine = null
        clearPick()
        showAll = false
        camJob?.cancel()
        camJob = viewModelScope.launch { readImage() }
    }

    fun retake() {
        camJob?.cancel()
        pickJob?.cancel()
        photo = null
        items = emptyList()
        detected = null
        camError = null
        camBusy = false
        clearPick()
        showAll = false
    }

    fun setOverlayMode(on: Boolean) {
        overlay = on
        prefs.overlayMode = on
        // Drawing over the photo needs every block translated.
        if (on && items.any { it.translated == null && !it.failed }) translateAll()
    }

    fun pickCamTarget(l: Lang) {
        if (l == detected || l == camTarget) return
        camTarget = l
        items = items.map { it.copy(translated = null, failed = false) }
        camEngine = null
        if (overlay || showAll) translateAll()
        if (picked.isNotEmpty()) translatePick(Engine.GOOGLE)
    }

    private suspend fun readImage() {
        val p = photo ?: return
        camBusy = true
        try {
            val r = Ocr.read(p)
            if (r.blocks.isEmpty()) {
                camError = "Không thấy chữ trong ảnh. Thử \"Dịch với Claude\" nếu chữ khó đọc."
                return
            }
            detected = r.lang
            camTarget = Lang.defaultTarget(r.lang)
            items = r.blocks.map { CamItem(it.text, it.box) }
        } catch (e: Exception) {
            camError = e.message ?: "Không đọc được ảnh"
            return
        } finally {
            camBusy = false
        }
        // List mode waits for the user to pick a region; overlay mode needs everything.
        if (overlay) translateItems()
    }

    /** "Dịch tất cả": translate every block and list them. */
    fun translateAll() {
        clearPick()
        showAll = true
        camJob?.cancel()
        camJob = viewModelScope.launch { translateItems() }
    }

    private suspend fun translateItems() {
        val src = detected ?: return
        val dst = camTarget
        camBusy = true
        camError = null
        val gate = Semaphore(4)
        val jobs = items.indices.filter { items[it].translated == null }.map { i ->
            viewModelScope.launch {
                gate.withPermit {
                    try {
                        val text = items.getOrNull(i)?.original ?: return@withPermit
                        val (t, e) = smart(text, src, dst)
                        if (i < items.size) items = items.toMutableList().also { it[i] = it[i].copy(translated = t, failed = false) }
                        camEngine = e
                    } catch (e: kotlinx.coroutines.CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        if (i < items.size) items = items.toMutableList().also { it[i] = it[i].copy(failed = true) }
                        if (camError == null) camError = e.message
                    }
                }
            }
        }
        jobs.forEach { it.join() }
        camBusy = false
    }

    // ---------- list mode: pick a region ----------

    /** Blocks touched by a dragged rectangle (image coordinates) become the selection. */
    fun pickRegion(region: Rect) {
        val hit = items.indices.filter { i -> items[i].box?.let { Rect.intersects(it, region) } == true }.toSet()
        if (hit.isEmpty()) return
        picked = hit
        showAll = false
        translatePick(Engine.GOOGLE)
    }

    /** Tapping a box adds it to / removes it from the selection. */
    fun toggleBlock(i: Int) {
        picked = if (i in picked) picked - i else picked + i
        showAll = false
        if (picked.isEmpty()) clearPick() else translatePick(Engine.GOOGLE, debounce = true)
    }

    fun clearPick() {
        pickJob?.cancel()
        picked = emptySet()
        pickResult = null
        pickEngine = null
        pickBusy = false
    }

    /** The picked blocks in reading order, joined into one passage. */
    private fun pickedText(): String {
        val sep = if (detected == Lang.ZH) "" else " "
        return picked.mapNotNull { items.getOrNull(it) }
            .sortedWith(compareBy({ (it.box?.top ?: 0) / 24 }, { it.box?.left ?: 0 }))
            .joinToString(sep) { it.original }
    }

    private fun translatePick(engine: Engine, debounce: Boolean = false) {
        val src = detected ?: return
        val text = pickedText()
        if (text.isBlank()) return
        pickJob?.cancel()
        pickJob = viewModelScope.launch {
            if (debounce) delay(350)
            pickBusy = true
            camError = null
            try {
                if (engine == Engine.CLAUDE) {
                    val t = ClaudeService.translateText(prefs.apiKey, model, text, src, camTarget)
                    pickResult = CamItem(text, null, t)
                    pickEngine = Engine.CLAUDE
                } else {
                    val (t, e) = smart(text, src, camTarget)
                    pickResult = CamItem(text, null, t)
                    pickEngine = e
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                camError = e.message
            } finally {
                pickBusy = false
            }
        }
    }

    // ---------- Claude ----------

    /** "Dịch với Claude" on the camera screen: the picked region, or every block (keeps their places
     *  so the overlay still works), or the whole photo when OCR found nothing. */
    fun claudeCamera() {
        val key = prefs.apiKey
        val p = photo ?: return
        if (key.isEmpty()) return
        if (!overlay && picked.isNotEmpty()) {
            translatePick(Engine.CLAUDE)
            return
        }
        camJob?.cancel()
        camJob = viewModelScope.launch {
            camBusy = true
            camError = null
            try {
                if (items.isEmpty()) {
                    val (orig, tr) = ClaudeService.translateImage(key, model, p, camTarget)
                    items = listOf(CamItem(orig, null, tr))
                    detected = detected ?: Ocr.detect(orig)
                    showAll = true
                    setOverlayMode(false)
                } else {
                    val out = ClaudeService.translateBlocks(key, model, p, items.map { it.original }, camTarget)
                    items = items.mapIndexed { i, it ->
                        val t = out.getOrNull(i)
                        if (t.isNullOrBlank()) it else it.copy(translated = t, failed = false)
                    }
                    if (!overlay) showAll = true
                }
                camEngine = Engine.CLAUDE
            } catch (e: Exception) {
                camError = e.message
            } finally {
                camBusy = false
            }
        }
    }

    // ---------- settings actions ----------

    fun setAlwaysOfflineMode(on: Boolean) {
        alwaysOffline = on
        prefs.alwaysOffline = on
        retranslate()
    }

    fun chooseModel(m: String) {
        model = m
        prefs.model = m
    }

    fun saveKey(key: String) {
        val k = key.trim()
        viewModelScope.launch {
            keyBusy = true
            keyStatus = null
            try {
                if (k.isNotEmpty()) ClaudeService.testKey(k)
                prefs.apiKey = k
                hasKey = k.isNotEmpty()
                keyStatus = true to if (k.isEmpty()) "Đã xoá API key" else "✓ Key hoạt động, nút Dịch với Claude đã sẵn sàng"
            } catch (e: Exception) {
                keyStatus = false to (e.message ?: "Key không dùng được")
            } finally {
                keyBusy = false
            }
        }
    }

    fun refreshPacks() {
        viewModelScope.launch {
            try {
                val have = OfflineTranslate.downloaded()
                Lang.entries.forEach { l ->
                    if (packs[l] != PackState.DOWNLOADING) packs[l] = if (l in have) PackState.READY else PackState.MISSING
                }
            } catch (_: Exception) {
            }
        }
    }

    fun downloadPack(l: Lang) {
        packs[l] = PackState.DOWNLOADING
        viewModelScope.launch {
            try {
                OfflineTranslate.download(l)
                packs[l] = PackState.READY
            } catch (e: Exception) {
                packs[l] = PackState.MISSING
                updateMsg = "Tải gói ${l.label} lỗi: ${e.message ?: "kiểm tra mạng"}"
            }
        }
    }

    fun deletePack(l: Lang) {
        viewModelScope.launch {
            try {
                OfflineTranslate.delete(l)
            } catch (_: Exception) {
            }
            refreshPacks()
        }
    }

    // ---------- update ----------

    fun checkUpdate(manual: Boolean) {
        if (checking) return
        viewModelScope.launch {
            checking = true
            try {
                update = Updater.check(version)
                if (manual && update?.hasUpdate == false) updateMsg = "Đã là bản mới nhất"
            } catch (e: Exception) {
                if (manual) updateMsg = e.message
            } finally {
                checking = false
            }
        }
    }

    fun installUpdate(context: Context) {
        val url = update?.url ?: return
        viewModelScope.launch {
            try {
                updateMsg = "Đang tải bản mới…"
                val started = Updater.install(context, url)
                updateMsg = if (started) null else "Cho phép MTrans cài ứng dụng, rồi bấm Cập nhật lại"
            } catch (e: Exception) {
                updateMsg = e.message
            }
        }
    }
}
