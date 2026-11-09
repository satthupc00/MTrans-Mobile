package com.mondiro.mtransmobile

import com.google.mlkit.nl.translate.TranslateLanguage

/** The three languages the app works with. */
enum class Lang(val code: String, val label: String, val google: String, val mlkit: String) {
    VI("vi", "Tiếng Việt", "vi", TranslateLanguage.VIETNAMESE),
    EN("en", "Tiếng Anh", "en", TranslateLanguage.ENGLISH),
    ZH("zh", "Tiếng Trung", "zh-CN", TranslateLanguage.CHINESE);

    companion object {
        fun of(code: String?): Lang? = entries.firstOrNull { it.code == code }

        /** Languages offered in "Dịch sang" once [from] is picked. */
        fun targetsFor(from: Lang): List<Lang> = entries.filter { it != from }

        /** Sensible default target: Vietnamese unless translating from Vietnamese. */
        fun defaultTarget(from: Lang): Lang = if (from == VI) EN else VI
    }
}
