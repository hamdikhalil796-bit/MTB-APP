package com.mtb.mtb.app

import android.app.LocaleManager
import android.content.Context
import android.os.LocaleList

/**
 * Wraps the platform per-app language API (API 33+, guaranteed by minSdk 34).
 * Changing the applied locale triggers the system to recreate the Activity,
 * so every stringResource() call across the app resolves against the new
 * language with no custom reactive plumbing needed.
 */
object LocaleHelper {
    const val FRENCH = "fr"
    const val ENGLISH = "en"
    const val ITALIAN = "it"

    fun currentLanguageCode(context: Context): String {
        val localeManager = context.getSystemService(LocaleManager::class.java)
        val current = localeManager?.applicationLocales?.get(0)?.language
        return current ?: FRENCH
    }

    fun setLanguage(context: Context, languageCode: String) {
        val localeManager = context.getSystemService(LocaleManager::class.java)
        localeManager?.applicationLocales = LocaleList.forLanguageTags(languageCode)
    }
}
