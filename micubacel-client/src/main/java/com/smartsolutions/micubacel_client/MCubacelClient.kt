package com.smartsolutions.micubacel_client

import com.smartsolutions.micubacel_client.exceptions.UnprocessableRequestException
import org.jsoup.Connection
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import kotlin.math.pow

class MCubacelClient {

    private val baseHomeUrl = "https://mi.cubacel.net"

    private var cookies = mutableMapOf<String, String>()

    private val urls = mutableMapOf(
        Pair("products", "https://mi.cubacel.net/primary/_-iiVGcd3i"),
        Pair("myAccount", "https://mi.cubacel.net/primary/_-ijqJlSHh"),
        Pair("login", "https://mi.cubacel.net:8443/login/Login"),
        Pair("create", "https://mi.cubacel.net:8443/login/NewUserRegistration"),
        Pair("verify", "https://mi.cubacel.net:8443/login/VerifyRegistrationCode"),
        Pair("passwordCreation", "https://mi.cubacel.net:8443/login/recovery/RegisterPasswordCreation")
    )

    private val dataKeys = mapOf(
        Pair("myStat_3001", DATA_BYTES), //Paquete de navegación
        Pair("myStat_30012", DATA_BONUS_BYTES), //Navegación LTE
        Pair("myStat_2001", DATA_DAILY_BAG), //Bolsa diaria
        Pair("myStat_bonusDataN", DATA_BONUS_CU_BYTES), //Navegación nacional
        Pair("myStat_bonusData", DATA_PROMO_BYTES) //Navegación promocional
    )

    private val dataIds = listOf(
        "myStat_3001", //Paquete de datos (megas de navegacion internacional)
        "myStat_30012", //
        "myStat_bonusDataN",
        "myStat_2001",
        "myStat_bonusData"
    )


    fun resolveHomeUrl(updateCookies: Boolean) : String? {
        val url = urls["home"]
        if (url != null && url.isNotEmpty()){
            return url
        }

        val response = ConnectionFactory.newConnection(baseHomeUrl).execute()
        if (updateCookies) {
            updateCookies(response.cookies())
        }

        response.parse().select("a[class=\"link_msdp langChange\"]").forEach { url ->
            if (url.attr("id") == "spanishLanguage") {
                urls["home"] = baseHomeUrl + url.attr("href")
                return urls["home"]
            }
        }
        return null
    }


    fun loadPage(url : String, updateCookies : Boolean = false) : Document {
        val response = ConnectionFactory.newConnection(url = url, cookies = cookies).execute()
        if (updateCookies){
            updateCookies(response.cookies())
        }
        return response.parse()
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

        val username = page.select("div[class=\"banner_bg_color mBottom20\"]")
            .first()
            .select("h2")
            .text()
            .replace("Bienvenido", "")
            .replace("a MiCubacel", "")
            .trimEnd()
            .trimStart()

        if (username.isNotEmpty()) {
            result["username"] = username
        }

        val columns = page.select("div[class=\"greayheader_row\"]")

        if (columns.isNotEmpty()){

            val columPhone = columns.select("div[class=\"col1\"]")
            if (columPhone.size == 1 && columPhone[0].childrenSize() == 2) {
                result["phone"] = columPhone[0].child(1).text().trimStart().trimEnd()
            }

            val columCredit = columns.select("div[class=\"col2 btype\"]")
            if (columCredit.size == 1 && columCredit[0].childrenSize() == 2) {
                result["credit"] = columCredit[0].child(1).text().trimStart().trimEnd()
            }

            val columnExpire = columns.select("div[class=\"col3 btype\"]")
            if (columnExpire.size == 1 && columnExpire[0].childrenSize() == 2){
                result["expire"] = columnExpire[0].child(1).text().trimStart().trimEnd()
            }
        }

        return result
    }


    fun signIn(phone: String, password: String)  {
        val data = mapOf(
            Pair("language", "es_ES"),
            Pair("username", phone),
            Pair("password", password)
        )
        val response = ConnectionFactory.newConnection(url = urls["login"]!!, cookies = cookies, data = data)
            .method(Connection.Method.POST)
            .execute()

        val page = response.parse()

        if (isErrorPage(page)){
            throw UnprocessableRequestException(errorMessage(page))
        }

        updateCookies(response.cookies())
    }


    fun signUp(firstName: String, lastName: String, phone: String) {
        val data = mapOf(
            Pair("msisdn", phone),
            Pair("firstname", firstName),
            Pair("lastname", lastName),
            Pair("agree", "on")
        )

        val response = ConnectionFactory.newConnection(urls["create"]!!, data, cookies )
            .method(Connection.Method.POST)
            .execute()

        val page = response.parse()

        if (isErrorPage(page) || page.select("form[action=\"/login/VerifyRegistrationCode\"]").first() == null){
            throw UnprocessableRequestException(errorMessage(page))
        }

        updateCookies(response.cookies())
    }

    fun verifyCode(code: String){
        val data = mapOf(
            Pair("username", code)
        )
        val response = ConnectionFactory.newConnection(urls["verify"]!!, data, cookies)
            .method(Connection.Method.POST)
            .execute()

        val page = response.parse()

        if (isErrorPage(page) || page.select("form[action=\"/login/recovery/RegisterPasswordCreation\"]").first() == null){
            throw UnprocessableRequestException(errorMessage(page))
        }

        updateCookies(response.cookies())
    }

    fun createPassword(password: String){
        val data = mapOf(
            Pair("newPassword", password),
            Pair("cnewPassword", password)
        )

        val response = ConnectionFactory.newConnection(urls["passwordCreation"]!!, data, cookies).method(Connection.Method.POST).execute()

        val page = response.parse()

        if (isErrorPage(page) || page.select("form[action=\"/login/jsp/welcome-login.jsp?language=es\"]").first() == null){
            throw UnprocessableRequestException(errorMessage(page))
        }
        updateCookies(response.cookies())
    }

    fun obtainPackagesInfo() : Map<String, Double>/*Pair<Document, List<Element>>*/ {
        val response = ConnectionFactory.newConnection(urls["myAccount"]!!, cookies = cookies).execute()

        val page = response.parse()
        //val elements = mutableListOf<Element>()

        if (!isLogged(page)){
            throw UnprocessableRequestException("Login Fail")
        }

        val data = mutableMapOf<String, Double>()

        dataKeys.forEach {
            page.getElementById(it.key)?.let { element ->
                data[it.value] = getValue(element)
            }
        }

        return data

        /*dataIds.forEach { id ->

            page.getElementById(id)?.let { element ->
                data.put(getKey(id), getValue(element))
            }

            page.getElementById(id)?.let { element ->
                elements.add(element)
            }
        }*/

        //return Pair(page, elements)
    }

    private fun getKey(id: String): String {
        TODO("Not yet implemented")
    }

    private fun getValue(element: Element): Double {
        val text = element.attr("data-text")

        val value: Double

        try {
            value = text.toDouble()
        } catch (e: Exception) {
            return 0.0
        }

        return when (element.attr("data-info")) {
            "KB" -> value * 1024
            "MB" -> value * 1024.0.pow(2)
            "GB" -> value * 1024.0.pow(3)
            else -> value
        }
    }


    private fun isLogged(page: Document) : Boolean {
        return page.select("div[class=\"myaccount_details\"]").first() != null && page.select("a[id=\"mySignin\"]").first() == null
    }

    private fun isErrorPage(page: Document) : Boolean{
        return page.select("div[class=\"body_wrapper error_page\"]").first() != null
    }

    private fun errorMessage(page: Document): String?{
        if (isErrorPage(page)){
            return try {
                page.select("div[class=\"body_wrapper error_page\"]").first()
                    .select("div[class=\"welcome_login error_Block\"]").first()
                    .select("div[class=\"container\"]").first()
                    .select("b").first().text()
            } catch (e: NullPointerException) {
                page.select("div[class=\"body_wrapper error_page\"]").first()
                    .select("div[class=\"welcome_login error_Block\"]").first()
                    .select("div[class=\"container\"]").first().text()
            }
        }
        return null
    }

    private fun updateCookies(updateCookies : Map<String, String>){
        updateCookies.forEach {
            cookies[it.key] = it.value
        }
    }

    companion object {

        const val DATA_BYTES = "bytes"
        const val DATA_BONUS_BYTES = "bonus_bytes"
        const val DATA_BONUS_CU_BYTES = "bonus_cu_bytes"
        const val DATA_PROMO_BYTES = "promo_bytes"
        const val DATA_DAILY_BAG = "daily_bag"

    }
}