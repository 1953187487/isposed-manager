package com.wangucanghai.isposed

import android.app.Application

/**
 * Isposed Manager application entry.
 *
 * Copyright (C) 2026 万古沧海 (WanGu CangHai) — Bilibili: 万古沧海
 * Licensed under GPL-3.0. Derived from LSPosed (GPL-3.0) & AxManager (Apache-2.0).
 */
class App : Application() {
    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        lateinit var instance: App
            private set
    }
}
