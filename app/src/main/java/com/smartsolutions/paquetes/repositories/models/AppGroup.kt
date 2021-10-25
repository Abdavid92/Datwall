package com.smartsolutions.paquetes.repositories.models

import kotlinx.parcelize.Parcelize

/**
 * Grupo de aplicaciones que tiene el mismo uid.
 * */
@Parcelize
class AppGroup(
    /**
     * Nombre de paquete
     * */
    override var packageName: String,
    /**
     * Identificador único (uid)
     * */
    override var uid: Int,
    /**
     * Nombre del grupo
     * */
    override var name: String,
    /**
     * Lista de aplicaciones
     * */
    private var apps: MutableList<App>,
    /**
     * Anotación de advertencia que se muestra cuando se intenta conceder
     * el acceso permanente al grupo completo.
     * */
    override var allowAnnotations: String?,
    /**
     * Anotación de advertencia que se muestra cuando se intenta bloquear
     * el acceso permanente al grupo completo.
     * */
    override var blockedAnnotations: String?
) : IApp, MutableList<App> {

    /**
     * Indica si el grupo de aplicaciones tiene acceso permanente.
     * Cuando esta propiedad se asigna, cambia el acceso a toads las aplicaciones
     * del grupo. Si una sola aplicación no tiene acceso, esta propiedad estará en `falso`.
     * */
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

    override var system: Boolean
        get() = getMasterApp().system
        set(value) {
            getMasterApp().system = value
        }

    override val size: Int
        get() = apps.size

    /**
     * Obtiene la aplicación maestra con la cual se creó el grupo.
     *
     * @return [App]
     * */
    fun getMasterApp(): App {
        return first { it.packageName == this.packageName }
    }

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

    /**
     * Obtiene un número construido basandose en el acceso permanente y temporal
     * de las aplicaciones del grupo. Este número se usa en el firewall para identificar diferencias
     * de acceso entre dos listas de aplicaciones y grupos.
     * */
    override fun accessHashToken(): String {
        var token = ""
        apps.forEach {
            token += it.accessHashToken()
        }
        return token
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AppGroup

        if (packageName != other.packageName) return false
        if (uid != other.uid) return false
        if (name != other.name) return false
        if (apps != other.apps) return false
        if (allowAnnotations != other.allowAnnotations) return false
        if (blockedAnnotations != other.blockedAnnotations) return false
        if (access != other.access) return false
        if (system != other.system) return false
        if (size != other.size) return false

        return true
    }

    override fun hashCode(): Int {
        var result = packageName.hashCode()
        result = 31 * result + uid
        result = 31 * result + name.hashCode()
        result = 31 * result + apps.hashCode()
        result = 31 * result + (allowAnnotations?.hashCode() ?: 0)
        result = 31 * result + (blockedAnnotations?.hashCode() ?: 0)
        result = 31 * result + access.hashCode()
        result = 31 * result + system.hashCode()
        result = 31 * result + size
        return result
    }
}