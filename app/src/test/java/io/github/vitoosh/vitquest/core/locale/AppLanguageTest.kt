package io.github.vitoosh.vitquest.core.locale

import org.junit.Assert.assertEquals
import org.junit.Test

class AppLanguageTest {

    @Test
    fun mapsStoredLanguageTags() {
        assertEquals(AppLanguage.INDONESIAN, AppLanguage.fromLanguageTag("id"))
        assertEquals(AppLanguage.INDONESIAN, AppLanguage.fromLanguageTag("in-ID"))
        assertEquals(AppLanguage.ENGLISH, AppLanguage.fromLanguageTag("en-US"))
    }

    @Test
    fun unsupportedOrMissingTagsFollowSystem() {
        assertEquals(AppLanguage.SYSTEM, AppLanguage.fromLanguageTag(null))
        assertEquals(AppLanguage.SYSTEM, AppLanguage.fromLanguageTag(""))
        assertEquals(AppLanguage.SYSTEM, AppLanguage.fromLanguageTag("fr"))
    }
}
