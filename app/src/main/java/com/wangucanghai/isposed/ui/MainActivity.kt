package com.wangucanghai.isposed.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.wangucanghai.isposed.R
import com.wangucanghai.isposed.ui.notice.Notices

/**
 * Main shell with bottom navigation.
 *
 * Copyright (C) 2026 万古沧海
 */
class MainActivity : AppCompatActivity() {

    private lateinit var nav: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        nav = findViewById(R.id.bottom_nav)
        nav.setOnItemSelectedListener { item ->
            val fragment: Fragment = when (item.itemId) {
                R.id.nav_modules -> ModulesFragment()
                R.id.nav_logs -> LogsFragment()
                R.id.nav_settings -> SettingsFragment()
                else -> HomeFragment()
            }
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit()
            true
        }
        if (savedInstanceState == null) {
            nav.selectedItemId = R.id.nav_home
            // 首次进入展示用户须知
            Notices.showAgreement(this, com.wangucanghai.isposed.core.AppPrefs(this))
        }
    }
}
