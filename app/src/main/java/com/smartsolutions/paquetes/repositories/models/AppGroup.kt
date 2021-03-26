package com.smartsolutions.paquetes.repositories.models

import kotlinx.parcelize.Parcelize

@Parcelize
class AppGroup(
    override var uid: Int,
    override var name: String,
    private val apps: List<App>,
    override var allowAnnotations: String?,
    override var blockedAnnotations: String?
) : IApp, Collection<App> {

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

    override fun contains(element: App) = apps.contains(element)

    override fun containsAll(elements: Collection<App>) = apps.containsAll(elements)

    override fun isEmpty() = apps.isEmpty()

    override fun iterator() = apps.iterator()

    operator fun get(i: Int): App {
        return apps[i]
    }

    fun toList() = apps
}