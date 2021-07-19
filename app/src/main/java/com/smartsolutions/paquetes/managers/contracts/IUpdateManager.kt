package com.smartsolutions.paquetes.managers.contracts

import android.net.Uri
import com.smartsolutions.paquetes.serverApis.models.AndroidApp

/**
 * Busca y descarga las actualizaciones.
 * */
interface IUpdateManager {

    /**
     * Busca una actualización en el servidor. Si la encuentra,
     * la guarda en el dataStore.
     *
     * @return [AndroidApp] si encontró una actualización.
     * */
    suspend fun findUpdate(): AndroidApp?

    /**
     * Programa la búsqueda de actualizaciones periodica.
     *
     * @param intervalInHours - Intervalo en horas.
     * */
    fun scheduleFindUpdate(intervalInHours: Long)

    /**
     * Descarga directa usando la url dada.
     *
     * @param url - Url de descarga.
     *
     * @return id de la descarga.
     * */
    fun downloadUpdate(url: Uri): Long

    /**
     * Construye la url dinámica para descarga.
     * */
    fun buildDynamicUrl(baseUrl: String, version: Int): String
}