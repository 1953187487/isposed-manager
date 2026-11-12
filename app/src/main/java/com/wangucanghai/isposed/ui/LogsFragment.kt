package com.wangucanghai.isposed.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.wangucanghai.isposed.R
import com.wangucanghai.isposed.ui.adapter.LogAdapter
import com.wangucanghai.isposed.util.Logger

/**
 * Logs page.
 *
 * Copyright (C) 2026 万古沧海
 */
class LogsFragment : Fragment() {

    private lateinit var adapter: LogAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_logs, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        adapter = LogAdapter()
        view.findViewById<RecyclerView>(R.id.recycler_logs).apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@LogsFragment.adapter
        }
        view.findViewById<MaterialButton>(R.id.btn_clear_logs).setOnClickListener {
            Logger.clear()
            adapter.submit(emptyList())
        }
        reload()
    }

    override fun onResume() {
        super.onResume()
        reload()
    }

    private fun reload() {
        adapter.submit(Logger.entries())
    }
}
