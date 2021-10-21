package com.smartsolutions.paquetes.managers

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.TypedValue
import androidx.core.content.ContextCompat
import com.smartsolutions.paquetes.managers.contracts.IIconManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

/**
 * Clase administradora de los íconos de las aplicaciones
 * */
class IconManager @Inject constructor(
    @ApplicationContext
    private val context: Context
) : IIconManager, CoroutineScope {

    /**
     * Servicio que se usa para obtener los íconos de las aplicaciones
     * */
    private val packageManager = context.packageManager

    /**
     * Directorio cache de la aplicación
     * */
    private val cacheDir = File(context.cacheDir, "icon_cache")

    /**
     * Nombre base de los íconos
     * */
    private val baseIconName = "icon_"

    private val handler = Handler(Looper.getMainLooper())

    init {
        if (!cacheDir.exists()) {
            //Si el directorio de cache de íconos no existe lo creo
            cacheDir.mkdir()
        }
    }

    @Suppress("DEPRECATION")
    override fun get(packageName: String, size: Int): Bitmap? {
        return try {
            val info = packageManager.getPackageInfo(packageName, 0)
            val version = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                info.longVersionCode
            } else {
                info.versionCode.toLong()
            }

            get(packageName, version, size)
        } catch (e: PackageManager.NameNotFoundException) {
            null
        }
    }

    override fun getAsync(packageName: String, size: Int, callback: (img: Bitmap) -> Unit) {
        launch {
            get(packageName, size)?.let {
                withContext(Dispatchers.Main){
                    callback(it)
                }
            }
        }
    }

    /**
     * Obtiene el ícono de la aplicación actualizado a la versión pasada como argumento
     *
     * @param packageName - Nombre de paquete de la aplicación
     * @param versionCode - Versión de la aplicación que se usará para determinar si
     * se debe actualizar el ícono.
     *
     * @return Ícono de la aplicación
     * */
    override fun get(packageName: String, versionCode: Long, size: Int): Bitmap? {
        getImageFile(packageName, versionCode, size)?.let {
            //Si obtengo el file, construyo el bitmap
            return BitmapFactory.decodeFile(it.path)
        }
        //Sino retorno null
        return null
    }

    override fun getAsync(
        packageName: String,
        versionCode: Long,
        size: Int,
        callback: (img: Bitmap) -> Unit
    ) {
        launch {
            get(packageName, versionCode, size)?.let {
                withContext(Dispatchers.Main){
                    callback(it)
                }
            }
        }
    }

    override fun getImageFile(packageName: String, versionCode: Long, size: Int): File? {
        //Instancio un file
        val iconFile = File(this.cacheDir, makeIconName(packageName, versionCode, size))

        //Si el file no existe es porque o no se ha creado el ícono o tiene una versión diferente
        if (!iconFile.exists()) {
            try {
                //Creo o actualizo el ícono
                saveOrUpdate(packageName, versionCode, size)
            } catch (e: PackageManager.NameNotFoundException) {
                return null
            }
        }

        return iconFile
    }

    override fun getImageFileAsync(
        packageName: String,
        versionCode: Long,
        size: Int,
        callback: (img: File) -> Unit
    ) {
        launch {
            getImageFile(packageName, versionCode, size)?.let {
                withContext(Dispatchers.Main) {
                    callback(it)
                }
            }
        }
    }

    override fun delete(packageName: String, versionCode: Long) {
        val iconName = makeIconName(packageName, versionCode, null)

        cacheDir.listFiles()
            ?.filter { it.name.contains(iconName) }
            ?.forEach {
                it.delete()
            }
    }

    /**
     * Elimina la cache de íconos completa.
     * */
    override fun deleteAll() {
        cacheDir.listFiles()?.forEach {
            it.delete()
        }
    }

    /**
     * Obtiene y crea un ícono de una aplicación si no existe
     *
     * @param packageName - Nombre de paquete de la aplicación
     * @param versionCode - Versión de la aplicación
     * */
    private fun create(packageName: String, versionCode: Long, size: Int) {
        //File que contendrá el ícono
        val file = File(cacheDir, makeIconName(packageName, versionCode, size))

        if (file.createNewFile()) {
            /*Si logré crear el file, construyo el ícono de la aplicación o uno predeterminado de android
              en caso de que la aplicación no tenga ícono*/
            val icon = try {
                getResizedBitmap(drawableToBitmap(packageManager.getApplicationIcon(packageName)), size)
            } catch (e: Exception) {
                getResizedBitmap(drawableToBitmap(ContextCompat.getDrawable(context, android.R.drawable.sym_def_app_icon)), size)
            }

            //Guardo el ícono en el file
            FileOutputStream(file).use {
                icon?.compress(Bitmap.CompressFormat.PNG, 100, it)
            }
        }
    }

    /**
     * Actualiza el ícono de una aplicación
     * */
    private fun update(packageName: String, versionCode: Long, size: Int, oldIcon: String) {
        val oldIconFile = File(cacheDir, oldIcon)

        if (oldIconFile.exists())
            oldIconFile.delete()

        create(packageName, versionCode, size)
    }

    /**
     * Guarda o actualiza un ícono.
     * */
    private fun saveOrUpdate(packageName: String, versionCode: Long, size: Int) {
        //Obtengo la lista de íconos en cache
        cacheDir.list()?.forEach { name ->

            if (name.contains("${this.baseIconName}$packageName")) {
                //Si contiene el nombre de paquete es porque existe pero tiene una versión diferente.
                //Entonces actualizo el ícono
                update(packageName, versionCode, size, name)
                //Y termino
                return
            }
        }
        //Sino encontré nada, creo el ícono
        create(packageName, versionCode, size)
    }

    /**
     * Contruye el nombre del ícono basado en el nombre de paquete y la versión.
     * */
    private fun makeIconName(packageName: String, versionCode: Long, size: Int?) =
        "${this.baseIconName}${packageName}_${versionCode}_${size ?: ""}"

    /**
     * Redimenciona un Bitmap
     *
     * @param image - Imagen a redimencionar
     * @param size - Tamaño que se le asignará a la imagen
     * @return Imagen redimencionada
     * */
    private fun getResizedBitmap(image: Bitmap?, size: Int): Bitmap? {
        image?.let {
            var width = image.width
            var height = image.height
            val bitmapRatio = width.toFloat() / height
            if (bitmapRatio > 1) {
                width = fixSize(size)
                height = (width / bitmapRatio).toInt()
            } else {
                height = fixSize(size)
                width = (height * bitmapRatio).toInt()
            }
            return Bitmap.createScaledBitmap(image, width, height, true)
        }
        return null
    }

    private fun fixSize(size: Int): Int {
        val displayMetrics = context.resources.displayMetrics
        return TypedValue
            .applyDimension(TypedValue.COMPLEX_UNIT_DIP, size.toFloat(), displayMetrics)
            .toInt()
    }

    /**
     * Convierte un Drawable a Bitmap
     *
     * @param drawable - Drawable a convertir
     * @return Bitmap
     * */
    private fun drawableToBitmap(drawable: Drawable?): Bitmap? {
        drawable?.let {
            if (drawable is BitmapDrawable) {
                if (drawable.bitmap != null) {
                    return drawable.bitmap
                }
            }
            val bitmap = if (drawable.intrinsicWidth <= 0 || drawable.intrinsicHeight <= 0) {
                Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
            } else {
                Bitmap.createBitmap(drawable.intrinsicWidth, drawable.intrinsicHeight, Bitmap.Config.ARGB_8888)
            }
            val canvas = Canvas(bitmap)
            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)
            return bitmap
        }
        return null
    }

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Default
}