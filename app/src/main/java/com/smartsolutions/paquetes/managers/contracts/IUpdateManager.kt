package com.smartsolutions.paquetes.managers.contracts

import android.app.Activity
import android.net.Uri
import android.os.Build
import androidx.annotation.RequiresApi
import com.smartsolutions.paquetes.serverApis.models.AndroidApp
import kotlinx.coroutines.Job

/**
 * Busca y descarga las actualizaciones.
 * */
interface IUpdateManager {

    val BASE_URL_APKLIS: String
        get() = "https://archive.apklis.cu/application/apk/"

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
    fun scheduleUpdateApplicationStatusWorker(intervalInHours: Long)

    /**
     * Indica si ya fué registrado el worker de actualización de estado.
     * */
    fun wasScheduleUpdateApplicationStatusWorker(): Boolean

    /**
     * Descarga directa usando la url dada.
     *
     * @param url - Url de descarga.
     *
     * @return id de la descarga o null si el archivo ya ha sido descargado
     * */
    fun downloadUpdate(url: Uri): Long


    /**
     * Permite obtener el estado de cualquier descarga previa hecha con el DownloadManager
     * @param id - ID unico de la descarga que provee el DownloadManager
     * @param callback - Interface por donde se informan los eventos segun el estado de la descarga
     * @author EJV96
     */
    fun getStatusDownload(id: Long, callback: DownloadStatus): Job


    /**
     * Encuentra el Uri del archivo descargado y se lo entrega a [installDownloadedPackage]
     * @param id - ID unico de la descarga que se completo
     * @author EJV96
     */
    fun installDownloadedPackage(idDownload: Long): Boolean

    /**
     * Envía un Intent con el Uri al instalador de paquetes
     * @return true si el resultado de la operación se completa correctamente o falso si se produce una excepción
     **/
    fun installDownloadedPackage(uri: Uri): Boolean

    /**
     * Busca el archivo en la carpeta de Updates de la app
     * @return El uri del archivo si fue descargado previamente o null si no existe
     */
    fun foundIfDownloaded(url: Uri): Uri?

    /**
     * Verifica el estado con el DownloadManager
     */
    fun isDownloaded(id: Long): Boolean

    /**
     * Construye la url dinámica para descarga.
     * */
    fun buildDynamicUrl(baseUrl: String, version: Int): String

    /**
     * Construye la url dinamicamente con los parametros recibidos
     */
    fun buildDynamicUrl(baseUrl: String, packageName: String,  version: Int): String

    /**
     * Construye la url dinamicamente con el androidApp
     */
    fun buildDynamicUrl(baseUrl: String, androidApp: AndroidApp): String

    /**
     * Verifica si el permiso de instalar apps está concedido para esta app
     * @author EJV96
     */
    fun isInstallPermissionGranted(): Boolean

    /**
     * Lanza un Intent solicitando el permiso de instalar apps para este paquete
     * @param requestCode: Entero utilizado para realizar el Intent
     * @param activity - Actividad donde se recibirá el onActivityResult
     * @author EJV96
     */
    @RequiresApi(Build.VERSION_CODES.O)
    fun requestInstallPermission(requestCode: Int, activity: Activity)

    /**
     * Obtiene del DataStore el id de la descarga que proporciona el DownloadManager
     * @return null si no se ha guardado ningun id o no hay ninguna descarga pendiente
     */
    fun getSavedDownloadId(): Long?

    /**
     * Guarda el id de la descarga proporcionado por el DownloadManager
     */
    fun saveIdDownload(id: Long?)

    /**
     *Detiene la descarga y la borra de el almacenamiento
     */
    fun cancelDownload(id: Long)

    /**
     * Interfaz que se utiliza para conocer el estado de las descargas realizadas con Download Manager
     */
    interface DownloadStatus {
        fun onRunning(downloaded: Long, total: Long)
        fun onFailed(reason: String)
        fun onPending()
        fun onPaused(reason: String)
        fun onSuccessful()
        fun onCanceled()
    }
}