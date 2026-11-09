package com.mondiro.mtransmobile

import android.graphics.Bitmap
import android.graphics.Rect
import com.google.mlkit.nl.languageid.LanguageIdentification
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.tasks.await

data class OcrBlock(val text: String, val box: Rect)

data class OcrResult(val blocks: List<OcrBlock>, val lang: Lang)

/** On-device text recognition (Latin + Chinese) and language detection. */
object Ocr {
    private val latin by lazy { TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS) }
    private val chinese by lazy { TextRecognition.getClient(ChineseTextRecognizerOptions.Builder().build()) }
    private val langId by lazy { LanguageIdentification.getClient() }

    private fun cjkRatio(s: String): Float {
        val letters = s.count { it.isLetter() }
        if (letters == 0) return 0f
        val cjk = s.count { Character.UnicodeScript.of(it.code) == Character.UnicodeScript.HAN }
        return cjk.toFloat() / letters
    }

    suspend fun read(bitmap: Bitmap): OcrResult = coroutineScope {
        val image = InputImage.fromBitmap(bitmap, 0)
        val a = async { latin.process(image).await() }
        val b = async { chinese.process(image).await() }
        val la = a.await()
        val zh = b.await()
        // The Chinese model also reads Latin letters, but the Latin model is better at accents.
        val useChinese = cjkRatio(zh.text) > 0.2f
        val src = if (useChinese) zh else la
        val blocks = src.textBlocks.mapNotNull { blk ->
            val box = blk.boundingBox ?: return@mapNotNull null
            val text = blk.lines.joinToString(if (useChinese) "" else " ") { it.text }.trim()
            if (text.isEmpty()) null else OcrBlock(text, box)
        }
        val all = blocks.joinToString("\n") { it.text }
        val lang = when {
            useChinese -> Lang.ZH
            all.isBlank() -> Lang.EN
            else -> when (langId.identifyLanguage(all).await()) {
                "vi" -> Lang.VI
                "zh", "zh-Latn" -> Lang.ZH
                else -> Lang.EN
            }
        }
        OcrResult(blocks, lang)
    }

    suspend fun detect(text: String): Lang? {
        if (cjkRatio(text) > 0.2f) return Lang.ZH
        return when (langId.identifyLanguage(text).await()) {
            "vi" -> Lang.VI
            "en" -> Lang.EN
            "zh", "zh-Latn" -> Lang.ZH
            else -> null
        }
    }
}
