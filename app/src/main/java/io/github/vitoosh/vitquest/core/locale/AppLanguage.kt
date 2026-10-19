package io.github.vitoosh.vitquest.core.locale

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat

/** Languages the user can pick in Settings. SYSTEM follows the device language. */
enum class AppLanguage(val languageTag: String?) {
    SYSTEM(null),
    INDONESIAN("id"),
    ENGLISH("en"),
    ;

    companion object {
        /** Maps a stored per-app language tag back to an option; "in" is the legacy code for Indonesian. */
        fun fromLanguageTag(tag: String?): AppLanguage = when (tag?.substringBefore('-')?.lowercase()) {
            null, "" -> SYSTEM
            "id", "in" -> INDONESIAN
            "en" -> ENGLISH
            else -> SYSTEM
        }
    }
}

/**
 * Thin wrapper over Android's per-app language API. AppCompat persists the
 * choice itself (the system does on Android 13+), so nothing is duplicated in DataStore.
 */
object AppLocales {

    fun current(): AppLanguage {
        val locales = AppCompatDelegate.getApplicationLocales()
        return if (locales.isEmpty) AppLanguage.SYSTEM else AppLanguage.fromLanguageTag(locales[0]?.toLanguageTag())
    }

    fun apply(language: AppLanguage) {
        val locales = language.languageTag
            ?.let { LocaleListCompat.forLanguageTags(it) }
            ?: LocaleListCompat.getEmptyLocaleList()
        AppCompatDelegate.setApplicationLocales(locales)
    }
}
