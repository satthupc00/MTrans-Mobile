package com.mondiro.mtransmobile

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/** Google Translate through the free public endpoint (no key). */
object GoogleTranslate {
    val http: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    suspend fun translate(text: String, from: Lang?, to: Lang): String = withContext(Dispatchers.IO) {
        val url = "https://translate.googleapis.com/translate_a/single".toHttpUrl().newBuilder()
            .addQueryParameter("client", "gtx")
            .addQueryParameter("sl", from?.google ?: "auto")
            .addQueryParameter("tl", to.google)
            .addQueryParameter("dt", "t")
            .addQueryParameter("dj", "1")
            .addQueryParameter("q", text)
            .build()
        val req = Request.Builder().url(url).header("User-Agent", "Mozilla/5.0").build()
        http.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) throw TranslateException("Google Dịch trả lỗi ${resp.code}")
            val body = resp.body?.string() ?: throw TranslateException("Google Dịch không trả kết quả")
            val json = try {
                JSONObject(body)
            } catch (e: Exception) {
                throw TranslateException("Google Dịch đang tạm chặn, thử lại sau")
            }
            val sentences = json.optJSONArray("sentences") ?: throw TranslateException("Google Dịch không trả kết quả")
            val sb = StringBuilder()
            for (i in 0 until sentences.length()) {
                sb.append(sentences.getJSONObject(i).optString("trans", ""))
            }
            sb.toString().trim().ifEmpty { throw TranslateException("Google Dịch không trả kết quả") }
        }
    }
}

class TranslateException(message: String) : Exception(message)
