package com.wangucanghai.isposed.lsposed

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import com.wangucanghai.isposed.util.Logger
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Reflection-based bridge to the LSPosed daemon (org.lsposed.lspd.service.LSPosedService).
 *
 * We do not compile against the LSPosed AIDL; we invoke it reflectively so the app
 * builds and runs standalone. When the LSPosed framework is absent, all calls return
 * null/empty and the repository falls back to self-managed preferences.
 *
 * Derived from LSPosed manager (GPL-3.0).
 *
 * Copyright (C) 2026 万古沧海
 */
class LsposedBridge private constructor(private val service: Any) {

    private val managerClass = service.javaClass.interfaces.firstOrNull()?.javaClass
        ?: service.javaClass.superclass

    private fun call(method: String, vararg args: Any?): Any? {
        return try {
            val m = managerClass?.methods?.firstOrNull {
                it.name == method && it.parameterCount == args.size
            }
            if (m == null) {
                Logger.w("LSPosed method $method not found")
                null
            } else {
                if (!m.isAccessible) m.isAccessible = true
                m.invoke(service, *args)
            }
        } catch (t: Throwable) {
            Logger.w("LSPosed bridge call $method failed: ${t.message}")
            null
        }
    }

    fun isModuleEnabled(pkg: String): Boolean? {
        val enabled = call("getEnabledModules") as? List<*> ?: return null
        return enabled.any { it.toString() == pkg }
    }

    fun setModuleEnabled(pkg: String, enabled: Boolean) {
        call("setModuleEnabled", pkg, enabled)
    }

    fun getModuleScope(pkg: String): List<String>? {
        val scope = call("getModuleScope", pkg)
        return (scope as? List<*>)?.map { it.toString() }
    }

    fun setModuleScope(pkg: String, scope: List<String>) {
        call("setModuleScope", pkg, scope)
    }

    companion object {
        private const val SERVICE_CLASS = "org.lsposed.lspd.service.LSPosedService"

        /**
         * Try to connect to the LSPosed daemon. Blocks up to 1.5s.
         * Returns null when LSPosed is not installed/running.
         */
        fun tryConnect(context: Context): LsposedBridge? {
            return try {
                Class.forName(SERVICE_CLASS)
                val binder = bind(context)
                if (binder == null) {
                    Logger.d("LSPosed daemon bind failed")
                    null
                } else {
                    // ILSPosedManager.Stub.asInterface(binder) via reflection
                    val stubClass = Class.forName("org.lsposed.lspd.ILSPosedManager\$Stub")
                    val asInterface = stubClass.getMethod("asInterface", IBinder::class.java)
                    val service = asInterface.invoke(null, binder)
                    if (service == null) {
                        Logger.d("LSPosed daemon asInterface returned null")
                        null
                    } else {
                        Logger.i("Connected to LSPosed daemon")
                        LsposedBridge(service)
                    }
                }
            } catch (t: Throwable) {
                Logger.d("LSPosed daemon not reachable: ${t.message}")
                null
            }
        }

        private fun bind(context: Context): IBinder? {
            val intent = Intent().apply {
                component = ComponentName("org.lsposed.manager", SERVICE_CLASS)
            }
            val latch = CountDownLatch(1)
            var ref: IBinder? = null
            val conn = object : ServiceConnection {
                override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
                    ref = binder
                    latch.countDown()
                }
                override fun onServiceDisconnected(name: ComponentName?) {}
            }
            val ok = try {
                context.bindService(intent, conn, Context.BIND_AUTO_CREATE)
            } catch (_: Exception) {
                false
            }
            if (!ok) return null
            try {
                latch.await(1500, TimeUnit.MILLISECONDS)
            } catch (_: InterruptedException) {
                Thread.currentThread().interrupt()
            }
            try { context.unbindService(conn) } catch (_: Exception) {}
            return ref
        }
    }
}
