package com.wangucanghai.isposed.adb

import android.content.Context
import com.wangucanghai.isposed.util.Logger
import java.io.File
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.cert.X509Certificate
import javax.net.ssl.KeyManager
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

/**
 * Persistent ADB authentication identity (RSA-2048 key + self-signed cert).
 *
 * The cert subject is `CN=Android Debug Bridge, O=Android, C=US` to match what
 * `adb keygen` produces; AOSP adbd extracts the public key from the presented
 * certificate to verify the AUTH signature during pairing/connect.
 *
 * Derived from AxManager's adb identity handling (Apache-2.0).
 *
 * Copyright (C) 2026 万古沧海
 */
class AdbKeyStore(context: Context) {

    private val file: File = File(context.filesDir, "adb_key.p12")
    private val password: CharArray = "isposed-adb-key".toCharArray()

    @Volatile
    private var loaded: Boolean = false

    private lateinit var keyPair: KeyPair
    private lateinit var certificate: X509Certificate
    private lateinit var keyManager: KeyManager
    private lateinit var trustManager: TrustManager

    @Synchronized
    fun ensure() {
        if (loaded) return
        if (file.exists()) {
            try {
                load()
                loaded = true
                Logger.d("ADB identity loaded from disk")
                return
            } catch (t: Throwable) {
                Logger.w("ADB identity load failed, regenerating: ${t.message}")
            }
        }
        generate()
        save()
        loaded = true
        Logger.d("ADB identity generated")
    }

    private fun generate() {
        val gen = KeyPairGenerator.getInstance("RSA")
        gen.initialize(2048)
        keyPair = gen.generateKeyPair()
        certificate = X509Builder.selfSigned(keyPair)
        keyManager = buildKeyManager()
        trustManager = acceptAllTrustManager()
    }

    private fun load() {
        val ks = KeyStore.getInstance("PKCS12")
        file.inputStream().use { ks.load(it, password) }
        val cert = ks.getCertificate(ALIAS) as X509Certificate
        val priv = ks.getKey(ALIAS, password)
        keyPair = KeyPair(cert.publicKey, priv as java.security.PrivateKey)
        certificate = cert
        keyManager = buildKeyManager()
        trustManager = acceptAllTrustManager()
    }

    private fun save() {
        try {
            val ks = KeyStore.getInstance("PKCS12")
            ks.load(null, password)
            ks.setKeyEntry(ALIAS, keyPair.private, password, arrayOf(certificate))
            file.outputStream().use { ks.store(it, password) }
        } catch (t: Throwable) {
            Logger.e("ADB identity save failed: ${t.message}")
        }
    }

    private fun buildKeyManager(): KeyManager {
        val ks = KeyStore.getInstance("PKCS12")
        ks.load(null, password)
        ks.setKeyEntry(ALIAS, keyPair.private, password, arrayOf(certificate))
        val kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm())
        kmf.init(ks, password)
        return kmf.keyManagers.first()
    }

    private fun acceptAllTrustManager(): TrustManager = object : X509TrustManager {
        override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {}
        override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {}
        override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()
    }

    fun sslContext(): SSLContext {
        ensure()
        val ctx = SSLContext.getInstance("TLS")
        ctx.init(arrayOf(keyManager), arrayOf(trustManager), null)
        return ctx
    }

    fun key(): KeyPair {
        ensure()
        return keyPair
    }

    fun cert(): X509Certificate {
        ensure()
        return certificate
    }

    companion object {
        private const val ALIAS = "adb"
    }
}
