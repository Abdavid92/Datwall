package com.smartsolutions.micubacel_client.models

import org.jsoup.nodes.Element

class ProductOld internal constructor(private val element: Element)  {

    /**
     * Título del producto.
     * */
    val title: String
        get() {
            return element.select("h4").first().text()
        }

    /**
     * Descripción.
     * */
    val description: String
        get() {
            return element.select("div[class=\"offerPresentationProductDescription_msdp product_desc\"]")
                .first().select("span").first().text()
        }

    /**
     * Precio.
     * */
    val price: Float
        get() {
            return element.select("div[class=\"offerPresentationProductDescription_msdp product_desc\"]")
                .first().select("span[class=\"bold\"]").first().text().toFloat()
        }

    /**
     * Enlace que se usa para comprar el producto.
     * */
    val urlBuyAction: String
        get() {
            return element.select("div[class=\"offerPresentationProductBuyAction_msdp ptype\"]").first()
                .select("a[class=\"offerPresentationProductBuyLink_msdp button_style link_button\"]").first()
                .attr("href")
        }
}