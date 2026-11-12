package com.wangucanghai.isposed.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.materialswitch.MaterialSwitch
import com.wangucanghai.isposed.R
import com.wangucanghai.isposed.data.ModuleInfo

/**
 * Module list adapter.
 *
 * Copyright (C) 2026 万古沧海
 */
class ModuleAdapter : RecyclerView.Adapter<ModuleAdapter.VH>() {

    private val items = mutableListOf<ModuleInfo>()
    var onToggle: ((ModuleInfo, Boolean) -> Unit)? = null
    var onClick: ((ModuleInfo) -> Unit)? = null

    fun submit(list: List<ModuleInfo>) {
        items.clear(); items.addAll(list); notifyDataSetChanged()
    }

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val icon: ImageView = v.findViewById(R.id.icon)
        val label: TextView = v.findViewById(R.id.label)
        val subtitle: TextView = v.findViewById(R.id.subtitle)
        val desc: TextView = v.findViewById(R.id.desc)
        val switch: MaterialSwitch = v.findViewById(R.id.switch_enable)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
        VH(LayoutInflater.from(parent.context).inflate(R.layout.item_module, parent, false))

    override fun getItemCount() = items.size

    override fun onBindViewHolder(h: VH, pos: Int) {
        val m = items[pos]
        h.icon.setImageDrawable(m.icon)
        h.label.text = m.label
        h.subtitle.text = "${m.packageName} · v${m.versionName}"
        h.desc.text = m.description ?: "描述：无"
        h.switch.isChecked = m.isEnabled
        h.switch.setOnCheckedChangeListener { _, checked ->
            onToggle?.invoke(m, checked)
        }
        h.itemView.setOnClickListener { onClick?.invoke(m) }
    }
}
