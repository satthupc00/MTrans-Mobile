package com.mondiro.mtransmobile

import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.common.model.RemoteModelManager
import com.google.mlkit.nl.translate.TranslateRemoteModel
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
import kotlinx.coroutines.tasks.await

/** On-device translation with ML Kit (one ~30 MB pack per language). */
object OfflineTranslate {
    private val manager = RemoteModelManager.getInstance()

    suspend fun downloaded(): Set<Lang> {
        val models = manager.getDownloadedModels(TranslateRemoteModel::class.java).await()
        val codes = models.map { it.language }.toSet()
        return Lang.entries.filter { it == Lang.EN || it.mlkit in codes }.toSet()
    }

    suspend fun download(lang: Lang) {
        val model = TranslateRemoteModel.Builder(lang.mlkit).build()
        manager.download(model, DownloadConditions.Builder().build()).await()
    }

    suspend fun delete(lang: Lang) {
        if (lang == Lang.EN) return
        manager.deleteDownloadedModel(TranslateRemoteModel.Builder(lang.mlkit).build()).await()
    }

    /** Translate on the device. Downloads missing packs only when [mayDownload]. */
    suspend fun translate(text: String, from: Lang, to: Lang, mayDownload: Boolean): String {
        val have = downloaded()
        val missing = listOf(from, to).filter { it !in have }
        if (missing.isNotEmpty()) {
            if (!mayDownload) {
                throw TranslateException("Chưa tải gói offline ${missing.joinToString { it.label }} (vào Cài đặt để tải)")
            }
            missing.forEach { download(it) }
        }
        val translator = Translation.getClient(
            TranslatorOptions.Builder().setSourceLanguage(from.mlkit).setTargetLanguage(to.mlkit).build()
        )
        try {
            return translator.translate(text).await()
        } finally {
            translator.close()
        }
    }
}
