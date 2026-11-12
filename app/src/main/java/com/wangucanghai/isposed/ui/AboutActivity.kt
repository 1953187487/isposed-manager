package com.wangucanghai.isposed.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.wangucanghai.isposed.R
import com.wangucanghai.isposed.ui.notice.NoticeContent
import com.wangucanghai.isposed.ui.notice.Notices

/**
 * About page: developer author (万古沧海 / B站同名), upstream projects and licenses.
 *
 * Copyright (C) 2026 万古沧海
 */
class AboutActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_about)

        findViewById<MaterialButton>(R.id.btn_third_party).setOnClickListener {
            Notices.showText(this, "第三方开源声明", THIRD_PARTY)
        }
    }

    private val THIRD_PARTY = """
        LSPosed (GPL-3.0)
        https://github.com/LSPosed/LSPosed
        引入：模块检测/作用域/守护进程接口

        AxManager / Axeron (Apache-2.0)
        https://github.com/fahrez182/AxManager
        引入：无线调试激活/shell/插件安装/BusyBox

        本软件整体以 GPL-3.0 开源。
        严格遵守上述两个项目的开源二改协议，
        保留上游版权声明与许可文本。
    """.trimIndent()
}
