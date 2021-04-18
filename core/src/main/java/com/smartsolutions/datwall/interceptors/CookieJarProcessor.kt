package com.smartsolutions.datwall.interceptors

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.smartsolutions.datwall.PreferencesKeys
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import okhttp3.*
import org.apache.commons.lang.time.DateUtils
import java.util.*

class CookieJarProcessor(
        private val dataStore: DataStore<Preferences>
) : CookieJar {

    override fun saveFromResponse(p0: HttpUrl, p1: MutableList<Cookie>) {

    }

    override fun loadForRequest(p0: HttpUrl): MutableList<Cookie> {
        return runBlocking {
            val cookies = mutableListOf<Cookie>()

            var exp = Date()
            exp = DateUtils.setYears(exp, 2037)
            exp = DateUtils.setMonths(exp, 11)
            exp = DateUtils.setDays(exp, 31)
            exp = DateUtils.setHours(exp, 23)
            exp = DateUtils.setMinutes(exp, 55)
            exp = DateUtils.setSeconds(exp, 55)

            var value: String? = null

            dataStore.data.map {
                value = it[PreferencesKeys.DATWALL_COOKIES]
            }

            cookies.add(Cookie.Builder()
                .name("__test")
                .value(value ?: "empty")
                .domain("smartsolutions-apps.infinityfreeapp.com")
                .expiresAt(exp.time)
                .path("/")
                .build())

            return@runBlocking cookies
        }
    }

    companion object {
        const val COOKIE_VALUE = "cookie_value"
    }
}