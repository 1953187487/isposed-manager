package com.wangucanghai.isposed.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.wangucanghai.isposed.R
import com.wangucanghai.isposed.data.ModuleRepository

/**
 * App picker (scope) adapter.
 *
 * Copyright (C) 2026 万古沧海
 */
class AppCheckAdapter : RecyclerView.Adapter<AppCheckAdapter.VH>() {

    private val items = mutableListOf<ModuleRepository.AppEntry>()
    private val selected = mutableSetOf<String>()
    var onChanged: (() -> Unit)? = null

    fun submit(list: List<ModuleRepository.AppEntry>, scope: Set<String>) {
        items.clear(); items.addAll(list)
        selected.clear(); selected.addAll(scope)
        notifyDataSetChanged()
    }

    fun selected(): Set<String> = selected.toSet()

    class VH(v: android.view.View) : RecyclerView.ViewHolder(v) {
        val check: CheckBox = v.findViewById(R.id.check)
        val icon: ImageView = v.findViewById(R.id.icon)
        val label: TextView = v.findViewById(R.id.label)
        val pkg: TextView = v.findViewById(R.id.pkg)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
        VH(LayoutInflater.from(parent.context).inflate(R.layout.item_app_check, parent, false))

    override fun getItemCount() = items.size

    override fun onBindViewHolder(h: VH, pos: Int) {
        val app = items[pos]
        h.label.text = app.label
        h.pkg.text = app.packageName
        h.icon.setImageDrawable(app.icon)
        h.check.isChecked = selected.contains(app.packageName)
        h.itemView.setOnClickListener {
            if (selected.contains(app.packageName)) selected.remove(app.packageName)
            else selected.add(app.packageName)
            h.check.isChecked = selected.contains(app.packageName)
            onChanged?.invoke()
        }
        h.check.setOnClickListener {
            if (h.check.isChecked) selected.add(app.packageName)
            else selected.remove(app.packageName)
            onChanged?.invoke()
        }
    }
}
