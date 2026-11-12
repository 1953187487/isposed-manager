package com.wangucanghai.isposed.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.wangucanghai.isposed.R
import com.wangucanghai.isposed.core.AppPrefs
import com.wangucanghai.isposed.ui.notice.NoticeContent
import com.wangucanghai.isposed.ui.notice.Notices

/**
 * Settings page: AI config, update, agreement, author & about.
 *
 * Copyright (C) 2026 万古沧海
 */
class SettingsFragment : Fragment() {

    private val prefs by lazy { AppPrefs(requireContext()) }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_settings, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // AI 配置：先弹 AI 须知
        view.findViewById<View>(R.id.row_ai_config).setOnClickListener {
            Notices.showAiNotice(requireActivity()) {
                startActivity(Intent(requireContext(), AiConfigActivity::class.java))
            }
        }
        view.findViewById<View>(R.id.row_ai_notice).setOnClickListener {
            Notices.showText(
                requireActivity(),
                NoticeContent.AI_NOTICE_TITLE,
                NoticeContent.AI_NOTICE
            )
        }
        view.findViewById<View>(R.id.row_update).setOnClickListener {
            startActivity(Intent(requireContext(), UpdateActivity::class.java))
        }
        view.findViewById<View>(R.id.row_agreement).setOnClickListener {
            Notices.showText(
                requireActivity(),
                NoticeContent.USER_AGREEMENT_TITLE,
                NoticeContent.USER_AGREEMENT
            )
        }
        view.findViewById<View>(R.id.row_shell).setOnClickListener {
            startActivity(Intent(requireContext(), ShellActivity::class.java))
        }
        view.findViewById<View>(R.id.row_about).setOnClickListener {
            startActivity(Intent(requireContext(), AboutActivity::class.java))
        }
        view.findViewById<View>(R.id.row_author).setOnClickListener {
            startActivity(Intent(requireContext(), AboutActivity::class.java))
        }
    }
}
