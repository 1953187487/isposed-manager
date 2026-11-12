package com.wangucanghai.isposed.core

/**
 * Application-wide constants & attribution.
 *
 * Copyright (C) 2026 万古沧海 (WanGu CangHai) — Bilibili: 万古沧海
 */
object Constants {
    const val APP_NAME = "Isposed Manager"
    const val APP_NAME_CN = "Isposed Manager"

    /** 开发作者 */
    const val AUTHOR = "万古沧海"
    const val AUTHOR_BILIBILI = "万古沧海"
    const val AUTHOR_GITHUB = "https://github.com/1953187487"
    const val AUTHOR_GITHUB_USER = "1953187487"

    /** 原作者/上游项目（严格遵守其开源二改协议） */
    const val UPSTREAM_LSPOSED_NAME = "LSPosed"
    const val UPSTREAM_LSPOSED_URL = "https://github.com/LSPosed/LSPosed"
    const val UPSTREAM_LSPOSED_LICENSE = "GPL-3.0"
    const val UPSTREAM_AXMANAGER_NAME = "AxManager (Axeron)"
    const val UPSTREAM_AXMANAGER_URL = "https://github.com/fahrez182/AxManager"
    const val UPSTREAM_AXMANAGER_LICENSE = "Apache-2.0"

    /** 更新检查：GitHub release 地址（发布后改为实际仓库） */
    const val UPDATE_REPO = "1953187487/isposed-manager"
    const val UPDATE_API_URL = "https://api.github.com/repos/$UPDATE_REPO/releases/latest"

    /** 无线调试 */
    const val ADB_DEFAULT_PORT = 5555

    /** AxManager 兼容的插件根目录（shell 模式下） */
    const val AXERON_PLUGIN_DIR = "/data/user_de/0/com.android.shell/axeron/plugins"
    const val AXERON_ENV = "AXERONDIR=/data/user_de/0/com.android.shell/axeron"

    /** 本应用作为 LSPosed 模块时的作用域存储 key */
    const val PREF_SCOPE = "module_scope"
}
