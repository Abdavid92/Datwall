package com.smartsolutions.paquetes.serverApis.middlewares

import com.smartsolutions.paquetes.serverApis.HttpDelegate
import okhttp3.*
import org.apache.commons.lang3.time.DateUtils
import java.util.*

class CookieJarProcessor : CookieJar {

    override fun saveFromResponse(p0: HttpUrl, p1: MutableList<Cookie>) {

    }

    override fun loadForRequest(p0: HttpUrl): MutableList<Cookie> {
        val cookies = mutableListOf<Cookie>()

        var exp = Date()
        exp = DateUtils.setYears(exp, 2037)
        exp = DateUtils.setMonths(exp, 11)
        exp = DateUtils.setDays(exp, 31)
        exp = DateUtils.setHours(exp, 23)
        exp = DateUtils.setMinutes(exp, 55)
        exp = DateUtils.setSeconds(exp, 55)

        val value = HttpDelegate.COOKIES

        cookies.add(Cookie.Builder()
                .name("__test")
                .value(value ?: "empty")
                .domain("smartsolutions-apps.infinityfreeapp.com")
                .expiresAt(exp.time)
                .path("/")
                .build())

        return cookies
    }
}