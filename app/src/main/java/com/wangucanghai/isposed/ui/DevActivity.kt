package com.wangucanghai.isposed.ui

import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AlertDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.wangucanghai.isposed.R
import com.wangucanghai.isposed.ai.AiClient
import com.wangucanghai.isposed.ai.ModuleCodeGen
import com.wangucanghai.isposed.util.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * AI direct development: generate a complete LSPosed module from a prompt.
 *
 * Copyright (C) 2026 万古沧海
 */
class DevActivity : AppCompatActivity() {

    private val client by lazy { AiClient(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dev)
        val prompt = findViewById<TextInputEditText>(R.id.input_prompt)
        val result = findViewById<TextView>(R.id.result)

        findViewById<MaterialButton>(R.id.btn_generate).setOnClickListener {
            val text = prompt.text.toString().trim()
            if (text.isEmpty()) { Toast.makeText(this, "请输入模块功能描述", Toast.LENGTH_SHORT).show(); return@setOnClickListener }
            if (!client.isConfigured()) {
                Toast.makeText(this, "请先配置 AI 服务商 API", Toast.LENGTH_LONG).show()
                startActivity(android.content.Intent(this, AiConfigActivity::class.java))
                return@setOnClickListener
            }
            result.text = "AI 生成中…（可能需要几十秒）"
            CoroutineScope(Dispatchers.Main).launch {
                val gen = ModuleCodeGen(this@DevActivity)
                val r = withContext(Dispatchers.IO) { gen.generate(text) }
                if (r.ok) {
                    val files = r.files.keys.joinToString("\n")
                    result.text = "生成成功，共 ${r.files.size} 个文件：\n$files"
                    Logger.i("AI codegen: ${r.files.size} files")
                    AlertDialog.Builder(this@DevActivity)
                        .setTitle("生成结果")
                        .setMessage(r.message + "\n\n" + files)
                        .setPositiveButton("查看代码") { _, _ ->
                            result.text = r.files.entries.joinToString("\n\n") { (k, v) ->
                                "===== $k =====\n$v"
                            }
                        }
                        .setNegativeButton("关闭", null)
                        .show()
                } else {
                    result.text = r.message
                }
            }
        }
    }
}
