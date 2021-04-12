package com.smartsolutions.micubacel_client

import org.jsoup.Connection
import org.jsoup.nodes.Document

class MCubacelClient {

    private val baseHomeUrl = "https://mi.cubacel.net"

    private var cookies = mapOf<String, String>()

    private val urls = mutableMapOf<String, String>(
        Pair("products", "https://mi.cubacel.net/primary/_-iiVGcd3i"),
        Pair("myAccount", "https://mi.cubacel.net/primary/_-ijqJlSHh")
    )

    var username: String? = null
        private set

    fun resolveHomeUrl() : String? {
        val response = ConnectionFactory.newConnection(baseHomeUrl).execute()

        response.parse().select("a[class=\"link_msdp langChange\"]").forEach { url ->

        cookies = response.cookies()

            if (url.attr("id") == "spanishLanguage") {
                return baseHomeUrl + url.attr("href")
            }
        }

        return null
    }


    fun loadPage(url : String) : Connection.Response {
        return ConnectionFactory.newConnection(url = url, cookies = cookies).execute()
    }


    fun readHomePage(page : Document)  : Map<String, String> {
        page.select("div[class=\"collapse navbar-collapse navbar-main-collapse\"]")
            .first()
            .select("li").forEach { li ->
                when (li.text()) {
                    "Productos" -> urls["products"] = baseHomeUrl + li.select("a").first().attr("href")
                    "Mi Cuenta" -> urls["myAccount"] = baseHomeUrl + li.select("a").first().attr("href")
                }
            }

        val result = mutableMapOf<String, String>()

        val username = page.select("div[class=\"banner_bg_color mBottom20\"]").first().select("h2").text().replace("Bienvenido", "").replace("a MiCubacel", "").trimEnd().trimStart()
        if (username.isNotEmpty()) {
            result["username"] = username
        }

        val columns = page.select("div[class=\"greayheader_row\"]")

        if (columns.isNotEmpty()){
            val columPhone = columns.select("div[class=\"col1\"]")
            if (columPhone.size > 1){
                result["phone"] = columPhone[1].text()
            }

            val columCredit = columns.select("div[class=\"col2 btype\"]")
            if (columCredit.size > 1){
                result["credit"] = columCredit[1].text().trimStart().trimEnd()
            }

            val columnExpire = columns.select("div[class=\"col3 btype\"]")
            if (columnExpire.size > 1){
                result["expire"] = columnExpire[1].text()
            }
        }

        return result
    }


}