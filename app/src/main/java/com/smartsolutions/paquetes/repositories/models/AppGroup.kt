package com.smartsolutions.paquetes.repositories.models

import kotlinx.android.parcel.Parcelize

@Parcelize
class AppGroup(
    override val uid: Int,
    override val name: String,
    private val apps: List<App>,
    override val allowAnnotations: String?,
    override val blockedAnnotations: String?
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