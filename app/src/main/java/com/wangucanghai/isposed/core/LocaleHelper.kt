package com.wangucanghai.isposed.core

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import java.util.Locale

/**
 * Per-app language switching.
 *
 * On Android 13+ the platform [android.app.LocaleManager] owns the per-app
 * locale. On older releases the locale is applied by overriding the activity
 * configuration through [applyLocale].
 *
 * Copyright (C) 2026 万古沧海
 */
object LocaleHelper {

    const val LANGUAGE_SYSTEM = "system"
    const val LANGUAGE_ZH = "zh"
    const val LANGUAGE_EN = "en"

    /** BCP-47 tag for the stored language, empty string means follow the system. */
    fun tagFor(language: String): String = when (language) {
        LANGUAGE_ZH -> "zh-CN"
        LANGUAGE_EN -> "en"
        else -> ""
    }

    /** Apply the stored language to the platform locale manager (Android 13+). */
    fun applyToSystem(context: Context, language: String) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val tag = tagFor(language)
        try {
            val manager = context.getSystemService(android.app.LocaleManager::class.java) ?: return
            val current = manager.applicationLocales.toLanguageTags()
            val target = if (tag.isEmpty()) "" else tag
            if (current != target) {
                manager.applicationLocales = if (target.isEmpty()) LocaleList.getEmptyLocaleList()
                else LocaleList.forLanguageTags(target)
            }
        } catch (_: Throwable) {
        }
    }

    /** Wrap [base] with a configuration carrying the requested language. */
    fun applyLocale(base: Context, language: String): Context {
        val tag = tagFor(language)
        if (tag.isEmpty()) return base
        val locale = Locale.forLanguageTag(tag)
        Locale.setDefault(locale)
        val config = Configuration(base.resources.configuration)
        config.setLocale(locale)
        config.setLayoutDirection(locale)
        return base.createConfigurationContext(config)
    }
}
