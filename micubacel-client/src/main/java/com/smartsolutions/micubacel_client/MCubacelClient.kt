package com.smartsolutions.micubacel_client

import org.jsoup.Connection

class MCubacelClient {

    private val baseHomeUrl = "https://mi.cubacel.net"

    private var cookies = mapOf<String, String>()

    private val urls = mutableMapOf<String, String>()

    var username: String? = null
        private set

    fun loadHome() {
        var response = ConnectionFactory.newConnection(baseHomeUrl)
            .execute()

        response.parse().select("a[class=\"link_msdp langChange\"]").forEach { url ->

        cookies = response.cookies()

            if (url.attr("id") == "spanishLanguage") {
                response = ConnectionFactory.newConnection(baseHomeUrl + url.attr("href"), cookies = cookies)
                    .execute()

                readHomePage(response)
            }
        }
    }

    private fun readHomePage(response: Connection.Response) {

        val page = response.parse()

        page.select("div[class=\"collapse navbar-collapse navbar-main-collapse\"]")
            .first()
            .select("li").forEach { li ->
                when (li.text()) {
                    "Productos" -> urls["products"] = baseHomeUrl + li.select("a").first().attr("href")
                    "Mi Cuenta" -> urls["myAccount"] = baseHomeUrl + li.select("a").first().attr("href")
                }
            }


    }
}