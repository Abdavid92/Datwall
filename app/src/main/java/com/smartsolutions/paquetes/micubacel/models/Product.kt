package com.smartsolutions.paquetes.micubacel.models

import com.smartsolutions.paquetes.helpers.createDataPackageId

data class Product(
    val title: String,
    val description: String,
    val price: Float,
    val urlBuy: String
) {

    val id: String
        get() = createDataPackageId(title, price)

}