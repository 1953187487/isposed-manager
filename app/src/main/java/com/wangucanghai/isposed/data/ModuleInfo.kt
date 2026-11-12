package com.wangucanghai.isposed.data

import android.graphics.drawable.Drawable

/**
 * An Xposed/LSPosed module descriptor.
 *
 * Copyright (C) 2026 万古沧海
 */
data class ModuleInfo(
    val packageName: String,
    val label: String,
    val versionName: String,
    val versionCode: Long,
    val icon: Drawable?,
    val description: String?,
    val minVersion: Int,
    val isEnabled: Boolean,
    val isActive: Boolean,
    val scope: List<String>,
)
