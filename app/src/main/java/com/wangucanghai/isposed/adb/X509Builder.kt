package com.wangucanghai.isposed.adb

import java.io.ByteArrayInputStream
import java.math.BigInteger
import java.security.KeyPair
import java.security.Signature
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.security.interfaces.RSAPublicKey
import java.util.Date

/**
 * Minimal DER encoder + self-signed X.509 certificate builder.
 *
 * Android's ADB TLS (both pairing and connect) requires the client to present
 * a certificate containing its RSA public key; adbd extracts the key from the
 * cert to verify the AUTH signature. This produces a valid self-signed cert
 * without pulling in BouncyCastle.
 *
 * Copyright (C) 2026 万古沧海
 */
internal object X509Builder {

    fun selfSigned(
        keyPair: KeyPair,
        cn: String = "Android Debug Bridge",
        org: String = "Android",
        country: String = "US",
        validDays: Int = 3650,
    ): X509Certificate {
        val now = Date()
        val notAfter = Date(now.time + validDays * 86400_000L)
        val pub = keyPair.public as RSAPublicKey
        val priv = keyPair.private

        val name = encodeName(cn, org, country)
        val spki = encodeSubjectPublicKeyInfo(pub)

        val tbs = DerWriter().apply {
            // version [0] EXPLICIT INTEGER 2 (v3)
            writeTagged(0, DerWriter.INTEGER(byteArrayOf(2)))
            write(encodeInteger(BigInteger.valueOf(System.currentTimeMillis())))
            write(encodeAlgorithmIdentifier(ALG_SHA256_RSA))
            write(name)
            write(
                DerWriter().apply {
                    write(encodeUtcTime(now))
                    write(encodeUtcTime(notAfter))
                }.toByteArray()
            )
            write(name)
            write(spki)
            // extensions [3] EXPLICIT SEQUENCE { basicConstraints critical }
            val bcBool = byteArrayOf(0x01, 0x01, 0xFF.toByte())
            val bcInner = DerWriter().apply {
                write(bcBool)
            }.toByteArray()
            val bcSeq = DerWriter().apply { write(bcInner) }.toByteArray()
            val bcOctet = DerWriter().apply { write(bcSeq) }.toByteArray()
            val bcExtSeq = DerWriter().apply {
                write(encodeOid(OID_BASIC_CONSTRAINTS))
                write(encodeOctetString(bcOctet))
            }.toByteArray()
            val bcExt = DerWriter().apply { write(bcExtSeq) }.toByteArray()
            writeTagged(3, bcExt)
        }.toByteArray()

        val sig = Signature.getInstance("SHA256withRSA")
        sig.initSign(priv)
        sig.update(tbs)
        val sigBytes = sig.sign()

        val cert = DerWriter().apply {
            write(tbs)
            write(encodeAlgorithmIdentifier(ALG_SHA256_RSA))
            write(encodeBitString(sigBytes))
        }.toByteArray()

        return CertificateFactory.getInstance("X.509")
            .generateCertificate(ByteArrayInputStream(cert)) as X509Certificate
    }

    // ---------- DER primitives ----------

    private fun encodeName(cn: String, org: String, country: String): ByteArray {
        val attrs = listOf(
            OID_COUNTRY to encodePrintableString(country),
            OID_ORG to encodePrintableString(org),
            OID_CN to encodeUtf8String(cn),
        )
        val rdns = DerWriter().apply {
            for ((oid, value) in attrs) {
                val attr = DerWriter().apply {
                    write(encodeOid(oid))
                    write(value)
                }.toByteArray()
                write(attr)
            }
        }.toByteArray()
        return DerWriter().apply { write(rdns) }.toByteArray()
    }

    private fun encodeSubjectPublicKeyInfo(pub: RSAPublicKey): ByteArray {
        val rsaPub = DerWriter().apply {
            write(encodeInteger(pub.modulus))
            write(encodeInteger(pub.publicExponent))
        }.toByteArray()
        return DerWriter().apply {
            write(encodeAlgorithmIdentifier(ALG_RSA))
            write(encodeBitString(rsaPub))
        }.toByteArray()
    }

    private fun encodeAlgorithmIdentifier(oid: ByteArray): ByteArray =
        DerWriter().apply {
            write(encodeOid(oid))
            write(DerWriter.TAG_NULL, byteArrayOf())
        }.toByteArray()

    private fun encodeInteger(value: BigInteger): ByteArray {
        var b = value.toByteArray()
        if (b.size > 1 && b[0] == 0.toByte() && (b[1].toInt() and 0x80) == 0) {
            b = b.copyOfRange(1, b.size)
        }
        return DerWriter().apply { write(DerWriter.INTEGER(b)) }.toByteArray()
    }

    private fun encodeOid(oid: ByteArray): ByteArray =
        DerWriter().apply { write(DerWriter.OID(oid)) }.toByteArray()

    private fun encodeUtf8String(s: String): ByteArray =
        DerWriter().apply { write(DerWriter.TAG_UTF8, s.toByteArray(Charsets.UTF_8)) }.toByteArray()

    private fun encodePrintableString(s: String): ByteArray =
        DerWriter().apply { write(DerWriter.TAG_PRINTABLE, s.toByteArray(Charsets.US_ASCII)) }.toByteArray()

    private fun encodeUtcTime(d: Date): ByteArray {
        val fmt = java.text.SimpleDateFormat("yyMMddHHmmss'Z'", java.util.Locale.US)
        fmt.timeZone = java.util.TimeZone.getTimeZone("UTC")
        return DerWriter().apply { write(DerWriter.TAG_UTC, fmt.format(d).toByteArray(Charsets.US_ASCII)) }.toByteArray()
    }

    private fun encodeOctetString(data: ByteArray): ByteArray =
        DerWriter().apply { write(DerWriter.TAG_OCTET, data) }.toByteArray()

    private fun encodeBitString(data: ByteArray): ByteArray =
        DerWriter().apply {
            write(DerWriter.TAG_BITSTRING, byteArrayOf(0) + data)
        }.toByteArray()

    // DER OIDs (content bytes only, first two components packed)
    private val OID_CN = byteArrayOf(0x55, 0x04, 0x03)
    private val OID_ORG = byteArrayOf(0x55, 0x04, 0x0A)
    private val OID_COUNTRY = byteArrayOf(0x55, 0x04, 0x06)
    private val OID_BASIC_CONSTRAINTS = byteArrayOf(0x55, 0x1D, 0x13)
    private val ALG_RSA = byteArrayOf(0x2A, 0x86.toByte(), 0x48, 0x86.toByte(), 0xF7.toByte(), 0x0D, 0x01, 0x01, 0x01)
    private val ALG_SHA256_RSA = byteArrayOf(0x2A, 0x86.toByte(), 0x48, 0x86.toByte(), 0xF7.toByte(), 0x0D, 0x01, 0x01, 0x0B)
}

/**
 * Minimal DER TLV writer.
 */
internal class DerWriter {
    private val out = java.io.ByteArrayOutputStream()

    companion object {
        const val TAG_SEQUENCE = 0x30
        const val TAG_INTEGER = 0x02
        const val TAG_OID = 0x06
        const val TAG_NULL = 0x05
        const val TAG_UTF8 = 0x0C
        const val TAG_PRINTABLE = 0x13
        const val TAG_UTC = 0x17
        const val TAG_OCTET = 0x04
        const val TAG_BITSTRING = 0x03

        fun INTEGER(content: ByteArray) = byteArrayOf(TAG_INTEGER.toByte()) + lengthPrefix(content) + content
        fun OID(content: ByteArray) = byteArrayOf(TAG_OID.toByte()) + lengthPrefix(content) + content

        private fun lengthPrefix(content: ByteArray): ByteArray {
            val tmp = DerWriter()
            tmp.writeLength(content.size)
            return tmp.out.toByteArray()
        }
    }

    fun write(b: ByteArray) {
        out.write(b)
    }

    fun write(tag: Int, content: ByteArray) {
        out.write(tag)
        writeLength(content.size)
        out.write(content)
    }

    fun writeTagged(tag: Int, content: ByteArray) {
        write(0xA0 or tag, content)
    }

    fun toByteArray(): ByteArray = out.toByteArray()

    private fun writeLength(len: Int) {
        if (len < 0x80) {
            out.write(len)
        } else {
            var l = len
            var bytes = 0
            var t = len
            while (t > 0) { t = t ushr 8; bytes++ }
            out.write(0x80 or bytes)
            for (i in bytes - 1 downTo 0) {
                out.write((l ushr (i * 8)) and 0xFF)
            }
        }
    }
}