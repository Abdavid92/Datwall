package com.smartsolutions.paquetes.micubacel

import com.smartsolutions.paquetes.exceptions.UnprocessableRequestException
import com.smartsolutions.paquetes.micubacel.models.DataType
import com.smartsolutions.paquetes.micubacel.models.Product
import com.smartsolutions.paquetes.micubacel.models.ProductGroup
import com.smartsolutions.paquetes.repositories.models.UserDataBytes
import org.jsoup.Connection
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import kotlin.jvm.Throws
import kotlin.math.pow

/**
 * Cliente de mi.cubacel.net
 * */
class MCubacelClient @Inject constructor() {

    /**
     * Url base de la página principal.
     * */
    private val baseHomeUrl = "https://mi.cubacel.net"

    /**
     * Urls principales del sitio.
     * */
    private val urls = mutableMapOf(
        Pair("products", "https://mi.cubacel.net/primary/_-iiVGcd3i"),
        Pair("myAccount", "https://mi.cubacel.net/primary/_-ijqJlSHh"),
        Pair("login", "https://mi.cubacel.net:8443/login/Login"),
        Pair("create", "https://mi.cubacel.net:8443/login/NewUserRegistration"),
        Pair("verify", "https://mi.cubacel.net:8443/login/VerifyRegistrationCode"),
        Pair("passwordCreation", "https://mi.cubacel.net:8443/login/recovery/RegisterPasswordCreation")
    )

    /**
     * Claves de los tag html que contienen los datos de la
     * cuenta del usuario.
     * */
    private val dataKeys = mapOf(
        Pair("myStat_3001", UserDataBytes.DataType.International), //Paquete de navegación
        Pair("myStat_30012", UserDataBytes.DataType.Bonus), //Navegación LTE
        Pair("myStat_2001", UserDataBytes.DataType.DailyBag), //Bolsa diaria
        Pair("myStat_bonusDataN", UserDataBytes.DataType.National), //Navegación nacional
        Pair("myStat_bonusData", UserDataBytes.DataType.PromoBonus) //Navegación promocional
    )

    /**
     * Resuelve la url de la página principal en el idioma español.
     * Este método actualiza las cookies en caso de que se le indique.
     * */
    fun resolveHomeUrl() : String? {
        val url = urls["home"]
        if (url != null && url.isNotEmpty()) {
            return url
        }

        val response = ConnectionFactory.newConnection(baseHomeUrl, COOKIES).execute()

        updateCookies(response.cookies())

        response.parse().select("a[class=\"link_msdp langChange\"]").forEach { url ->
            if (url.attr("id") == "spanishLanguage") {
                urls["home"] = baseHomeUrl + url.attr("href")
                return urls["home"]
            }
        }
        return null
    }


    /**
     * Carga la página de la url dada.
     *
     * @param url - Url de la página a cargar.
     *
     * @return Document con la página cargada
     * */
    @Throws(Exception::class)
    fun loadPage(url: String) : Document {
        val response = ConnectionFactory.newConnection(url = url, cookies = COOKIES).execute()
        updateCookies(response.cookies())
        return response.parse()
    }

    /**
     * Lee la página principal y obtiene los datos principales del usuario en
     * caso de que esté autenticado.
     *
     * @param page - Página principal.
     *
     * @return Un mapa con los datos esenciales de usuario tales como
     * el teléfono y el saldo.
     *
     * @see USERNAME
     * @see PHONE
     * @see CREDIT
     * @see EXPIRE
     *
     * */
    fun readHomePage(page: Document): Map<String, String> {
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
            result[USERNAME] = username
        }

        val columns = page.select("div[class=\"greayheader_row\"]")

        fun readColumn(cssQuery: String, key: String) {
            val column = columns.select(cssQuery)

            if (column.size == 1 && column[0].childrenSize() == 2) {
                result[key] = column[0].child(1).text().trimStart().trimEnd()
            }
        }

        if (columns.isNotEmpty()) {
            readColumn("div[class=\"col1\"]", PHONE)
            readColumn("div[class=\"col2 btype\"]", CREDIT)
            readColumn("div[class=\"col3 btype\"]", EXPIRE)
        }

        return result
    }

    /**
     * Inicia sesión y guarda las cookies de inicio de sesión.
     *
     * @param phone - Teléfono de usuario.
     * @param password - Contraseña.
     * */
    fun signIn(phone: String, password: String): Map<String, String>  {
        val data = mapOf(
            Pair("language", "es_ES"),
            Pair("username", phone),
            Pair("password", password)
        )
        val response = ConnectionFactory.newConnection(
            url = urls["login"]!!,
            cookies = COOKIES,
            data = data
        )
            .method(Connection.Method.POST)
            .execute()

        val page = response.parse()

        if (isErrorPage(page)){
            throw UnprocessableRequestException(UnprocessableRequestException.Reason.BAG_CREDENTIALS, errorMessage(page))
        }

        return updateCookies(response.cookies())
    }

    /**
     * Inicia el proceso de creación de cuenta.
     *
     * @param firstName - Nombres.
     * @param lastName - Apellidos.
     * @param phone - Teléfono.
     * */
    fun signUp(firstName: String, lastName: String, phone: String): Map<String, String> {
        val data = mapOf(
            Pair("msisdn", phone),
            Pair("firstname", firstName),
            Pair("lastname", lastName),
            Pair("agree", "on")
        )

        val response = ConnectionFactory.newConnection(urls["create"]!!, data, COOKIES)
            .method(Connection.Method.POST)
            .execute()

        val page = response.parse()

        if (isErrorPage(page) || page.select("form[action=\"/login/VerifyRegistrationCode\"]").first() == null) {
            throw UnprocessableRequestException(errorMessage(page))
        }

        return updateCookies(response.cookies())
    }

    /**
     * Verifica el código recibido por sms en el proceso de creación de cuenta.
     * */
    fun verifyCode(code: String): Map<String, String> {
        val data = mapOf(
            Pair("username", code)
        )
        val response = ConnectionFactory.newConnection(urls["verify"]!!, data, COOKIES)
            .method(Connection.Method.POST)
            .execute()

        val page = response.parse()

        if (isErrorPage(page) || page.select("form[action=\"/login/recovery/RegisterPasswordCreation\"]").first() == null){
            throw UnprocessableRequestException(UnprocessableRequestException.Reason.WRONG_CODE, errorMessage(page))
        }

        return updateCookies(response.cookies())
    }

    /**
     * Completa el proceso de creación de cuenta con la contraseña.
     *
     * @param password - Contraseña.
     * */
    fun createPassword(password: String): Map<String, String> {
        val data = mapOf(
            Pair("newPassword", password),
            Pair("cnewPassword", password)
        )

        val response = ConnectionFactory.newConnection(urls["passwordCreation"]!!, data, COOKIES)
            .method(Connection.Method.POST)
            .execute()

        val page = response.parse()

        if (isErrorPage(page) ||
            page.select("form[action=\"/login/jsp/welcome-login.jsp?language=es\"]")
                .first() == null
        ) {
            throw UnprocessableRequestException(errorMessage(page))
        }
        return updateCookies(response.cookies())
    }

    /**
     * Obtiene todos los productos disponibles a la venta.
     *
     * @return Una lista de productos organizados en grupos.
     * */
    fun getProducts(): List<ProductGroup> {
        val page = ConnectionFactory.newConnection(url = urls["products"]!!, cookies = COOKIES)
            .get()

        val productGroup = mutableListOf<ProductGroup>()

        val bags = "Bolsas"
        val packages = "Paquetes"
        val packagesLTE = "Paquetes_LTE"

        fun buildProductGroup(element: Element, productType: ProductGroup.GroupType) {
            val products = mutableListOf<Product>()

            element.nextElementSibling().select("div[class=\"product_inner_block\"]").forEach {
                products.add(buildProduct(it))
            }

            productGroup.add(
                ProductGroup(
                    productType,
                    products.toTypedArray()
                ))
        }

        page.select("h3[class=\"product_block_title\"]").forEach { element ->

            when (element.text()) {
                bags -> {
                    buildProductGroup(element, ProductGroup.GroupType.Bag)
                }
                packages -> {
                    buildProductGroup(element, ProductGroup.GroupType.Packages)
                }
                packagesLTE -> {
                    buildProductGroup(element, ProductGroup.GroupType.PackagesLTE)
                }
            }
        }

        if (productGroup.size < 2) {
            throw UnprocessableRequestException(UnprocessableRequestException.Reason.NO_LOGIN)
        }

        return productGroup
    }

    /**
     * Resuelve la url de confirmación de compra de un producto.
     *
     * @param productUrl - Url de compra del producto.
     *
     * @return Url de confirmación de compra o null en caso de
     * no poder resolverla.
     * */
    fun resolveUrlBuyProductConfirmation(productUrl: String) : String? {
        val pageConfirmation = ConnectionFactory.newConnection(productUrl, cookies = COOKIES)
            .get()

        val urlConfirmation = pageConfirmation
            .select("a[class=\"offerPresentationProductBuyLink_msdp button_style link_button\"]")
            .first()

        if (urlConfirmation.text().trimStart().trimEnd().equals("Confirmar", true) ){
            val url = urlConfirmation.attr("href")
            return if (url.startsWith(baseHomeUrl)) {
                url
            } else {
                baseHomeUrl + url
            }
        }

        return null
    }

    /**
     * Compra un producto.
     *
     * @param url - Url del producto a comprar.
     *
     * @throws UnprocessableRequestException En caso de ocurrir un
     * error en la compra.
     * */
    @Throws(UnprocessableRequestException::class)
    fun buyProduct(url: String) {
        val result = ConnectionFactory.newConnection(url, cookies = COOKIES)
            .get()

        result.select("div[class=\"products_purchase_details_block\"]")
            .first()
            .select("p")
            .forEach { p ->
                if (p.text().contains("error", true)) {
                    throw UnprocessableRequestException(p.text())
                }
            }
    }

    /**
     * Obtiene todos los datos de la cuenta del cliente. Este método se debe llamar
     * una vez se inició sesión.
     *
     * @return Una lista de claves, valor y fecha de expiración en caso
     * de que aplique.
     *
     * @throws UnprocessableRequestException En caso de no tener la sesión iniciada.
     * */
    @Throws(UnprocessableRequestException::class)
    fun obtainPackagesInfo() : List<DataType> {
        val response = ConnectionFactory.newConnection(urls["myAccount"]!!, cookies = COOKIES)
            .execute()

        val page = response.parse()

        if (!isLogged(page)) {
            throw UnprocessableRequestException(UnprocessableRequestException.Reason.NO_LOGIN, "Debe inciar sesión.")
        }

        val data = mutableListOf<DataType>()

        dataKeys.forEach {
            page.getElementById(it.key)?.let { element ->
                data.add(DataType(it.value, getValue(element), getDateExpired(element)))
            }
        }

        return data
    }

    /**
     * Obtiene la fecha de expiración.
     * */
    private fun getDateExpired(element: Element): Long {
        try {
            val dateText = element.parent()
                .parent()
                .parent()
                .select("div[class=\"expiry_date_right\"]")
                .first()
                .select("span[class=\"date_value\"]")
                .first()
                .text()
                .trimStart()
                .trimEnd()

            return if (dateText.endsWith("AM", true) || dateText.endsWith("PM", true)) {
                SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.US)
                    .parse(dateText)!!
                    .time
            } else {
                SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.US)
                    .parse(dateText)!!
                    .time
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return 0
    }

    /**
     * Obtiene el valor en bytes
     * */
    private fun getValue(element: Element): Long {
        val text = element.attr("data-text")

        val value: Float

        try {
            value = text.toFloat()
        } catch (e: Exception) {
            return 0
        }

        return when (element.attr("data-info")) {
            "KB" -> value * 1024
            "MB" -> value * 1024.0.pow(2)
            "GB" -> value * 1024.0.pow(3)
            else -> value
        }.toLong()
    }

    /**
     * Contruye una instancia de Product y llena sus propiedades.
     * */
    private fun buildProduct(element: Element): Product {
        return Product(
            title = element.select("h4").first().text(),
            description = element.select("div[class=\"offerPresentationProductDescription_msdp product_desc\"]")
                .first().select("span").first().text(),
            price = element.select("div[class=\"offerPresentationProductDescription_msdp product_desc\"]")
                .first().select("span[class=\"bold\"]").first().text().replace(",", "").toFloat(),
            urlBuy = baseHomeUrl + element.select("div[class=\"offerPresentationProductBuyAction_msdp ptype\"]").first()
                .select("a[class=\"offerPresentationProductBuyLink_msdp button_style link_button\"]").first()
                .attr("href")
        )
    }

    /**
     * Verifica si se ha iniciado sesión correctamente.
     * */
    private fun isLogged(page: Document): Boolean {
        return page.select("div[class=\"myaccount_details\"]")
            .first() != null &&
                page.select("a[id=\"mySignin\"]")
                    .first() == null
    }

    /**
     * Verifica si es la página de error.
     * */
    private fun isErrorPage(page: Document): Boolean {
        return page.select("div[class=\"body_wrapper error_page\"]").first() != null
    }

    /**
     * Obtiene el mensaje de error de la página de error.
     * Si se le pasa otra página retorna null.
     * */
    private fun errorMessage(page: Document): String? {
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

    /**
     * Actualiza las cookies.
     * */
    private fun updateCookies(updateCookies: Map<String, String>): Map<String, String> {
        updateCookies.forEach {
            COOKIES[it.key] = it.value
        }
        return COOKIES
    }

    companion object {

        /**
         * Mapa de cookies que guardan las sesiones y otros datos.
         * */
        var COOKIES = mutableMapOf<String, String>()

        /**
         * Nombre de usuario.
         * */
        const val USERNAME = "username"
        /**
         * Teléfono.
         * */
        const val PHONE = "phone"
        /**
         * Saldo principal.
         * */
        const val CREDIT = "credit"
        /**
         * Fecha de expiración de la linea.
         * */
        const val EXPIRE = "expire"

    }
}