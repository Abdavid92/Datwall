package com.smartsolutions.datwall.repositories.models

import kotlinx.parcelize.Parcelize

@Parcelize
class AppGroup(
    override var uid: Int,
    override var name: String,
    private var apps: MutableList<App>,
    override var allowAnnotations: String?,
    override var blockedAnnotations: String?
) : IApp, MutableList<App> {

    override var access: Boolean
        get() {
            apps.forEach {
                if (!it.access)
                    return false
            }
            return true
        }
        set(value) {
            apps.forEach { it.access = value }
        }

    override val size: Int
        get() = apps.size

    override fun contains(element: App): Boolean = apps.contains(element)

    override fun containsAll(elements: Collection<App>): Boolean = apps.containsAll(elements)

    override fun get(index: Int): App = apps[index]

    override fun indexOf(element: App): Int = apps.indexOf(element)

    override fun isEmpty(): Boolean = apps.isEmpty()

    override fun iterator(): MutableIterator<App> = apps.iterator()

    override fun lastIndexOf(element: App): Int = apps.lastIndexOf(element)

    override fun add(element: App): Boolean = apps.add(element)

    override fun add(index: Int, element: App) = apps.add(index, element)

    override fun addAll(index: Int, elements: Collection<App>): Boolean = apps.addAll(index, elements)

    override fun addAll(elements: Collection<App>): Boolean = apps.addAll(elements)

    override fun clear() = apps.clear()

    override fun listIterator(): MutableListIterator<App> = apps.listIterator()

    override fun listIterator(index: Int): MutableListIterator<App> = apps.listIterator(index)

    override fun remove(element: App): Boolean = apps.remove(element)

    override fun removeAll(elements: Collection<App>): Boolean = apps.removeAll(elements)

    override fun removeAt(index: Int): App = apps.removeAt(index)

    override fun retainAll(elements: Collection<App>): Boolean = apps.retainAll(elements)

    override fun set(index: Int, element: App): App = apps.set(index, element)

    override fun subList(fromIndex: Int, toIndex: Int): MutableList<App> = apps.subList(fromIndex, toIndex)

    override fun accessHashCode(): Int {
        var code = ""
        apps.forEach {
            code += it.accessHashCode().toString()
        }
        return code.toInt()
    }
}