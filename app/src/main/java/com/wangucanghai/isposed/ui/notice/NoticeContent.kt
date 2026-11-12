package com.wangucanghai.isposed.ui.notice

/**
 * User agreement & AI usage notice content.
 *
 * Copyright (C) 2026 万古沧海
 */
object NoticeContent {

    /** 协议版本号：内容变更时递增，用于让新版本重新展示须知。 */
    const val AGREEMENT_VERSION = 3

    const val USER_AGREEMENT_TITLE = "用户须知协议"
    val USER_AGREEMENT = """
        【Isposed Manager 用户须知协议】

        感谢使用「Isposed Manager（沧海免root管理器）」。在使用本软件前，请仔细阅读并同意以下条款。

        一、软件性质
        1. 本软件是一款开源的 Android 免 root 管理器，基于 LSPosed（GPL-3.0）与 AxManager（Apache-2.0）开发。
        2. 本软件通过 ADB 无线调试（免 root）或 Root 方式获得系统管理能力。

        二、使用风险
        1. 修改系统参数、冻结应用、安装模块等操作可能影响设备稳定性，请自行承担风险。
        2. 请在操作前做好数据备份。因使用本软件造成的任何损失，作者不承担责任。
        3. 部分系统（如厂商定制 ROM）可能与本软件不兼容，请谨慎使用。

        三、合规使用
        1. 请勿将本软件用于违法、违规或侵犯他人权益的用途。
        2. 请遵守当地法律法规与设备厂商的服务条款。

        四、开源许可
        1. 本软件以 GPL-3.0 协议开源，衍生自 LSPosed（GPL-3.0）与 AxManager（Apache-2.0）。
        2. 详细开源声明见「设置 → 关于 → 第三方开源声明」。

        五、AI 功能
        1. 使用 AI 功能前请阅读「AI 使用须知」。

        六、免责与更新
        1. 本软件可能不定期更新，更新行为以应用内提示为准。
        2. 若你不同意本协议，请立即停止使用并卸载本软件。

        —— 万古沧海（B站同名）
    """.trimIndent()

    const val AI_NOTICE_TITLE = "AI 使用须知"

    val AI_NOTICE = """
        【AI 使用须知】

        本软件提供 AI 相关功能（AI 配置、AI 转换模块、AI 直接开发）。使用前请阅读：

        一、数据发送
        1. AI 功能会将你的提示词（以及你选择的模块元数据）发送到你配置的自定义 AI 服务商。
        2. 请勿在提示词中包含隐私、密码、密钥等敏感信息。
        3. 作者不会收集、存储或上传你的提示词内容。

        二、自定义 API
        1. AI 功能需要你自行配置兼容 OpenAI 的服务商 API（Base URL + API Key + 模型）。
        2. 你的 API Key 仅保存在本机，不会上传到任何第三方。

        三、生成结果
        1. AI 生成的模块代码可能存在缺陷，使用前请人工审查。
        2. AI 生成的模块及其行为由使用者自行承担风险与责任。

        四、责任
        1. 作者不对 AI 生成内容的准确性、安全性作担保。
        2. 使用 AI 功能即表示你已阅读并同意本须知。

        —— 万古沧海（B站同名）
    """.trimIndent()

    const val OSS_NOTICE_TITLE = "开源协议声明"
    val OSS_NOTICE = """
        【开源协议声明】

        本软件（Isposed Manager）为开源软件，以 GPL-3.0 协议发布。在开发过程中，本软件引入或参考了以下开源项目，特此声明并致谢：

        一、LSPosed
        · 项目：https://github.com/LSPosed/LSPosed
        · 许可：GPL-3.0
        · 引入内容：模块管理契约（xposedmodule 元数据）、模块启用与作用域管理逻辑、LSPosed 守护进程通信桥接。
        · 依据：作为 GPL-3.0 衍生作品，本软件整体以 GPL-3.0 发布，并保留原作者版权与许可声明。

        二、AxManager（Axeron Manager）
        · 项目：https://github.com/fahrez182/AxManager
        · 许可：Apache-2.0
        · 引入内容：免 root 激活方案（ADB 无线调试 mDNS 发现、TLS 配对与连接、TCP 模式切换）、Shell 执行器、插件（module.prop + customize.sh）安装机制、Re-ignite 热重启。
        · 依据：Apache-2.0 允许衍生使用，本软件保留其版权、许可与致谢声明（详见 THIRD_PARTY_NOTICES.md）。

        三、本软件自身
        · 作者：万古沧海（B站同名，GitHub: https://github.com/1953187487）
        · 版权：Copyright (C) 2026 万古沧海
        · 许可：GPL-3.0（见 LICENSE 文件）

        完整上游许可文本见应用内「关于 → 第三方开源声明」。
    """.trimIndent()
}
