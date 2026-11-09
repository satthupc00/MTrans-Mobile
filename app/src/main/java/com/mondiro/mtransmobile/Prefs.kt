package com.mondiro.mtransmobile

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/** Plain settings plus the Claude API key in encrypted storage. */
class Prefs(context: Context) {
    private val plain: SharedPreferences = context.getSharedPreferences("mtrans", Context.MODE_PRIVATE)
    private val secret: SharedPreferences? = try {
        val key = MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()
        EncryptedSharedPreferences.create(
            context, "mtrans_secret", key,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    } catch (e: Exception) {
        null
    }

    var from: Lang
        get() = Lang.of(plain.getString("from", null)) ?: Lang.EN
        set(v) = plain.edit().putString("from", v.code).apply()

    var to: Lang
        get() = Lang.of(plain.getString("to", null)) ?: Lang.VI
        set(v) = plain.edit().putString("to", v.code).apply()

    var alwaysOffline: Boolean
        get() = plain.getBoolean("offline", false)
        set(v) = plain.edit().putBoolean("offline", v).apply()

    var model: String
        get() = plain.getString("model", MODELS[0].first) ?: MODELS[0].first
        set(v) = plain.edit().putString("model", v).apply()

    var overlayMode: Boolean
        get() = plain.getBoolean("overlay", false)
        set(v) = plain.edit().putBoolean("overlay", v).apply()

    var apiKey: String
        get() = secret?.getString("key", "") ?: ""
        set(v) {
            secret?.edit()?.putString("key", v.trim())?.apply()
        }

    companion object {
        val MODELS = listOf(
            "claude-haiku-5-5" to "Haiku 5.5 — nhanh nhất, rẻ nhất",
            "claude-sonnet-5-5" to "Sonnet 5.5 — dịch mượt hơn",
            "claude-opus-5-5" to "Opus 5.5 — mạnh nhất, chậm",
        )
    }
}
