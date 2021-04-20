package com.smartsolutions.micubacel_api

import org.jsoup.Connection
import org.jsoup.Jsoup
import java.io.IOException
import java.security.KeyManagementException
import java.security.NoSuchAlgorithmException
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager
import kotlin.math.pow


object ConnectionFactory {

    fun newConnection(
        url: String,
        data: Map<String, String>? = null,
        cookies: Map<String, String>? = null): Connection {

        val connection = Jsoup.connect(url)
            .userAgent("Mozilla/5.0")
            .timeout(10000)
            .maxBodySize(1024.0.pow(2.0).toInt() * 5)
            .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
            .header("Accept-Language", "es-ES,es;q=0.8,en-US;q=0.5,en;q=0.3")
            .header("Accept-Encoding", "gzip, deflate, br")
            .sslSocketFactory(socketFactory())

        data?.let {
            connection.data(it)
        }

        cookies?.let {
            connection.cookies(it)
        }
        return connection
    }

    private fun socketFactory(): SSLSocketFactory? {
        val trustAllCerts: Array<TrustManager> = arrayOf(object : X509TrustManager {
            override fun getAcceptedIssuers(): Array<X509Certificate?>? {
                return null
            }

            @Suppress("TrustAllX509TrustManager")
            override fun checkClientTrusted(certs: Array<X509Certificate?>?, authType: String?) {}
            @Suppress("TrustAllX509TrustManager")
            override fun checkServerTrusted(certs: Array<X509Certificate?>?, authType: String?) {}
        })
        return try {
            val sslContext: SSLContext = SSLContext.getInstance("SSL")
            sslContext.init(null, trustAllCerts, SecureRandom())
            sslContext.socketFactory
        } catch (e: NoSuchAlgorithmException) {
            throw RuntimeException("Failed to create a SSL socket factory", e)
        } catch (e: KeyManagementException) {
            throw RuntimeException("Failed to create a SSL socket factory", e)
        }
    }

    @Throws(IOException::class)
    fun getCookies(url: String): MutableMap<String, String> {
        return newConnection(url).execute().cookies()
    }

    @Throws(IOException::class)
    fun getCaptcha(url: String, cookies: Map<String, String>): ByteArray {
        return Jsoup.connect(url)
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .header("Accept-Language", "es-ES,es;q=0.8,en-US;q=0.5,en;q=0.3")
                .header("Accept-Encoding", "gzip, deflate, br")
                .ignoreContentType(true).timeout(25000).cookies(cookies)
                .execute().bodyAsBytes()
    }

    @Throws(IOException::class)
    fun getImg(url: String): ByteArray {
        return Jsoup.connect(url)
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .header("Accept-Language", "es-ES,es;q=0.8,en-US;q=0.5,en;q=0.3")
                .header("Accept-Encoding", "gzip, deflate, br")
                .ignoreContentType(true).timeout(25000)
                .execute().bodyAsBytes()
    }
}