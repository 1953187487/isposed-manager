package com.wangucanghai.isposed.ui.notice

import android.app.Activity
import android.app.AlertDialog
import android.widget.TextView
import com.wangucanghai.isposed.core.AppPrefs

/**
 * Dialogs for the user agreement and the AI usage notice.
 *
 * The AI notice MUST pop up every time a user taps any AI feature.
 *
 * Copyright (C) 2026 万古沧海
 */
object Notices {

    fun showAgreement(activity: Activity, prefs: AppPrefs) {
        AlertDialog.Builder(activity)
            .setTitle(NoticeContent.USER_AGREEMENT_TITLE)
            .setMessage(NoticeContent.USER_AGREEMENT)
            .setCancelable(true)
            .setPositiveButton("我已阅读并同意") { _, _ -> prefs.agreementAccepted = true }
            .setNegativeButton("取消", null)
            .show()
    }

    /**
     * Show AI usage notice. Called before EVERY AI feature entry point.
     * Returns via [onAgree] when the user confirms.
     */
    fun showAiNotice(activity: Activity, onAgree: () -> Unit) {
        AlertDialog.Builder(activity)
            .setTitle(NoticeContent.AI_NOTICE_TITLE)
            .setMessage(NoticeContent.AI_NOTICE)
            .setCancelable(true)
            .setPositiveButton("同意并继续") { _, _ -> onAgree() }
            .setNegativeButton("取消", null)
            .show()
    }

    fun showText(activity: Activity, title: String, text: String) {
        val tv = TextView(activity).apply {
            setText(text)
            textSize = 13f
            setPadding(48, 24, 48, 24)
        }
        AlertDialog.Builder(activity)
            .setTitle(title)
            .setView(tv)
            .setPositiveButton("关闭", null)
            .show()
    }
}
