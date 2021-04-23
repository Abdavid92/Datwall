package com.smartsolutions.paquetes.micubacel.models

data class ProductGroup(
    val type: GroupType,
    val products: Array<Product>
) {

    enum class GroupType {
        Packages,
        PackagesLTE,
        Bag
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ProductGroup

        if (type != other.type) return false
        if (!products.contentEquals(other.products)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = type.hashCode()
        result = 31 * result + products.contentHashCode()
        return result
    }
}