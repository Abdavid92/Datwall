package com.smartsolutions.paquetes.micubacel.models

class ProductGroup(
    val type: GroupType,
    products: Array<Product>
): List<Product> {

    private val list = listOf(*products)

    override val size: Int
        get() = list.size

    override fun contains(element: Product) = list.contains(element)

    override fun containsAll(elements: Collection<Product>) = list.containsAll(elements)

    override fun get(index: Int) = list[index]

    override fun indexOf(element: Product) = list.indexOf(element)

    override fun isEmpty() = list.isEmpty()

    override fun iterator() = list.iterator()

    override fun lastIndexOf(element: Product) = list.lastIndexOf(element)

    override fun listIterator() = list.listIterator()

    override fun listIterator(index: Int) = list.listIterator(index)

    override fun subList(fromIndex: Int, toIndex: Int) = list.subList(fromIndex, toIndex)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ProductGroup

        if (type != other.type) return false
        if (list != other.list) return false

        return true
    }

    override fun hashCode(): Int {
        var result = type.hashCode()
        result = 31 * result + list.hashCode()
        return result
    }

    enum class GroupType {
        Packages,
        PackagesLTE,
        Bag
    }
}