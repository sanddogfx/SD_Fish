package com.sdfish.app

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import java.util.Locale

/** App languages. An empty tag means "follow the phone's language". */
object AppLanguage {
    val supported = listOf("ru", "pl", "en")

    /** Tag picked in the app, or "" when following the system. */
    fun selectedTag(): String {
        val locales = AppCompatDelegate.getApplicationLocales()
        return if (locales.isEmpty) "" else locales[0]?.language.orEmpty()
    }

    fun select(tag: String) {
        AppCompatDelegate.setApplicationLocales(
            if (tag.isEmpty()) LocaleListCompat.getEmptyLocaleList() else LocaleListCompat.forLanguageTags(tag)
        )
    }

    fun locale(): Locale {
        val tag = selectedTag()
        return if (tag.isEmpty()) Locale.getDefault() else Locale.forLanguageTag(tag)
    }

    /** "ru", "pl" or "en": the language texts are shown in (English if the phone uses another one). */
    fun current(): String = locale().language.let { if (it in supported) it else "en" }

    /** Language names are written in their own language so anyone can find theirs. */
    fun nativeName(tag: String): String = when (tag) {
        "ru" -> "Русский"
        "pl" -> "Polski"
        "en" -> "English"
        else -> tag
    }
}
