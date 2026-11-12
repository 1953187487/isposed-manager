package com.wangucanghai.isposed.ui

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.wangucanghai.isposed.R
import com.wangucanghai.isposed.adb.ShellExecutor
import com.wangucanghai.isposed.core.AppPrefs
import com.wangucanghai.isposed.util.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Root-free shell terminal (WebUI-style quick shell).
 *
 * Copyright (C) 2026 万古沧海
 */
class ShellActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_shell)
        val input = findViewById<TextInputEditText>(R.id.input_cmd)
        val output = findViewById<TextView>(R.id.output)

        findViewById<MaterialButton>(R.id.btn_run).setOnClickListener {
            val cmd = input.text.toString().trim()
            if (cmd.isEmpty()) return@setOnClickListener
            output.text = "> $cmd\n"
            CoroutineScope(Dispatchers.Main).launch {
                val shell = ShellExecutor(AppPrefs(this@ShellActivity))
                val r = withContext(Dispatchers.IO) { shell.exec(cmd) }
                output.text = "> $cmd\n\n${r.output}"
                Logger.i("shell: $cmd -> ${r.output.take(100)}")
            }
        }
    }
}
