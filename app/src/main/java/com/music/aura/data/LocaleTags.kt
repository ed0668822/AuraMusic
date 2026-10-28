package com.music.aura.data

import java.util.Locale

/**
 * Single place that knows zh-Hant / zh-Hans.
 *
 * Internally the app stores and compares full script tags ("zh-Hant",
 * "zh-Hans"). Downstream services that care about region (translation
 * endpoints, etc.) get the dialect they understand ("zh-TW" / "zh-CN").
 *
 * Aura-specific: the original BitChord version also exposed YouTube-specific
 * helpers (ytHl, ytGl, acceptLanguage). Those are omitted because Aura is
 * local-only and will never build an Innertube context.
 */
object LocaleTags {
    const val ZH_HANT = "zh-Hant"
    const val ZH_HANS = "zh-Hans"
    const val ZH_TW_WIRE = "zh-TW"
    const val ZH_CN_WIRE = "zh-CN"

    /**
     * Normalize any incoming tag ("zh_TW", "zh-HK", "zh", "zh-Hant-TW", ...)
     * to "zh-Hant" / "zh-Hans" / base language. Bare "zh" stays "zh-Hans"
     * because every existing zh catalogue is Simplified.
     */
    fun normalizeAppTag(raw: String?): String {
        val tag = raw?.trim()?.replace('_', '-')?.takeIf { it.isNotBlank() } ?: return "en"
        val lower = tag.lowercase(Locale.ROOT)
        if (lower.startsWith("zh")) {
            // Explicit script wins.
            if ("hant" in lower) return ZH_HANT
            if ("hans" in lower) return ZH_HANS
            // Region implies script.
            if ("tw" in lower || "hk" in lower || "mo" in lower) return ZH_HANT
            if ("cn" in lower || "sg" in lower || "my" in lower) return ZH_HANS
            // Bare "zh" (or "zh-Latn" etc.): existing catalogues are Simplified.
            return ZH_HANS
        }
        // Other languages: base language only ("en-US" -> "en"), with legacy fixes.
        val base = Locale.forLanguageTag(tag).language.lowercase(Locale.ROOT).ifBlank { lower.substringBefore('-') }
        return when (base) {
            "iw" -> "he"
            "in" -> "id"
            "ji" -> "yi"
            else -> base.ifBlank { "en" }
        }
    }

    /** Full tag for the current device/app locale, script-preserving. */
    fun systemTag(): String = normalizeAppTag(Locale.getDefault().toLanguageTag())

    /**
     * What the lyrics translation endpoint wants as `tl`. Google understands
     * zh-TW / zh-CN as scripts; sending bare "zh" always returns Simplified,
     * and sending "zh-Hant" is not reliably honoured — so map script to region.
     * An explicit translator choice in zh-TW/zh-CN form passes through.
     */
    fun translationWireTarget(tag: String): String {
        val t = tag.trim().replace('_', '-')
        if (t.equals("zh-TW", ignoreCase = true) || t.equals("zh-Hant", ignoreCase = true)) return ZH_TW_WIRE
        if (t.equals("zh-CN", ignoreCase = true) || t.equals("zh-Hans", ignoreCase = true)) return ZH_CN_WIRE
        val norm = normalizeAppTag(t)
        return when (norm) {
            ZH_HANT -> ZH_TW_WIRE
            ZH_HANS -> ZH_CN_WIRE
            else -> t.ifBlank { "en" }
        }
    }

    /** Script-aware equality: zh-Hant != zh-Hans != zh, unlike base-language compare. */
    fun sameLanguageScriptAware(first: String, second: String): Boolean {
        fun canon(tag: String): String {
            val t = tag.trim().replace('_', '-')
            val lower = t.lowercase(Locale.ROOT)
            if (lower.startsWith("zh")) {
                if ("tw" in lower || "hant" in lower || "hk" in lower || "mo" in lower) return "zh-hant"
                if ("cn" in lower || "hans" in lower || "sg" in lower) return "zh-hans"
                return "zh"
            }
            return Locale.forLanguageTag(t).language.lowercase(Locale.ROOT).ifBlank { lower.substringBefore('-') }
        }
        return canon(first) == canon(second)
    }

    fun isChineseTag(tag: String): Boolean =
        tag.trim().replace('_', '-').lowercase(Locale.ROOT).startsWith("zh")
}
