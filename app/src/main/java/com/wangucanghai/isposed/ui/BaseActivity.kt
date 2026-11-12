package com.wangucanghai.isposed.ui

import android.content.Context
import androidx.activity.ComponentActivity
import com.wangucanghai.isposed.core.AppPrefs
import com.wangucanghai.isposed.core.LocaleHelper

/**
 * Base activity that applies the user selected in-app language to its context.
 *
 * Copyright (C) 2026 万古沧海
 */
abstract class BaseActivity : ComponentActivity() {

    override fun attachBaseContext(newBase: Context) {
        val prefs = AppPrefs(newBase)
        super.attachBaseContext(LocaleHelper.applyLocale(newBase, prefs.appLanguage))
    }
}
