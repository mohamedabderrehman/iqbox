package com.iqbox.app.data.local

import android.content.Context
import android.content.res.Configuration
import java.util.Locale

/**
 * LocaleManager - Manages app language preference (Arabic/English)
 */
class LocaleManager(private val context: Context) {
    
    companion object {
        private const val PREFS_NAME = "iqbox_locale"
        private const val KEY_LANGUAGE = "language"
        const val LANG_ARABIC = "ar"
        const val LANG_ENGLISH = "en"
    }
    
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    
    fun getLanguage(): String {
        return prefs.getString(KEY_LANGUAGE, LANG_ARABIC) ?: LANG_ARABIC
    }
    
    fun setLanguage(language: String) {
        prefs.edit().putString(KEY_LANGUAGE, language).apply()
    }
    
    fun isArabic(): Boolean = getLanguage() == LANG_ARABIC
    
    fun toggleLanguage(): String {
        val newLang = if (isArabic()) LANG_ENGLISH else LANG_ARABIC
        setLanguage(newLang)
        return newLang
    }
    
    fun applyLocale(context: Context): Context {
        // Use Locale("ar", "MA") for Arabic to force Western digits (1,2,3) instead of Arabic-Indic (١٢٣)
        val locale = if (getLanguage() == LANG_ARABIC) Locale("ar", "MA") else Locale(getLanguage())
        Locale.setDefault(locale)
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        config.setLayoutDirection(locale)
        return context.createConfigurationContext(config)
    }
}
