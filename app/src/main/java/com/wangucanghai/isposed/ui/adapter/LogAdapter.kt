package com.wangucanghai.isposed.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.wangucanghai.isposed.R

/**
 * Log line adapter.
 *
 * Copyright (C) 2026 万古沧海
 */
class LogAdapter : RecyclerView.Adapter<LogAdapter.VH>() {

    private val items = mutableListOf<String>()

    fun submit(list: List<String>) {
        items.clear(); items.addAll(list); notifyDataSetChanged()
    }

    class VH(v: android.view.View) : RecyclerView.ViewHolder(v) {
        val line: TextView = v.findViewById(R.id.line)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
        VH(LayoutInflater.from(parent.context).inflate(R.layout.item_log, parent, false))

    override fun getItemCount() = items.size

    override fun onBindViewHolder(h: VH, pos: Int) {
        val text = items[pos]
        h.line.text = text
        h.line.setTextColor(
            when {
                text.contains(" E  ") -> 0xFFB00020.toInt()
                text.contains(" W  ") -> 0xFFED6C02.toInt()
                else -> 0xFF1A1A1A.toInt()
            }
        )
    }
}
