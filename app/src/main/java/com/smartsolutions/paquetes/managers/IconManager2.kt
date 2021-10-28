package com.smartsolutions.paquetes.managers

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.util.TypedValue
import androidx.core.content.ContextCompat
import com.smartsolutions.paquetes.managers.contracts.IIconManager2
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

@Suppress("BlockingMethodInNonBlockingContext", "DEPRECATION")
class IconManager2 @Inject constructor(
    @ApplicationContext
    private val context: Context
) : CoroutineScope, IIconManager2 {

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Default

    /**
     * Directorio cache de la aplicación
     * */
    private lateinit var cacheDir: File

    /**
     * Servicio que se usa para obtener los íconos de las aplicaciones
     * */
    private val packageManager = context.packageManager

    /**
     * Nombre base de los íconos
     * */
    private val baseIconName = "icon_"

    init {
        launch {
            cacheDir = File(context.cacheDir, "icon_cache")

            withContext(Dispatchers.IO) {
                if (!cacheDir.exists()) {
                    //Si el directorio de cache de íconos no existe lo creo
                    cacheDir.mkdir()
                }
            }
        }
    }


    override suspend fun synchronizeIcons(size: Int) {
        packageManager.getInstalledPackages(0).forEach {
            getIcon(it, size)
        }
    }

    override suspend fun synchronizeIcons(infos: List<PackageInfo>, size: Int) {
       infos.forEach {
            getIcon(it, size)
        }
    }


    override fun getIcon(
        packageName: String,
        versionCode: Long,
        size: Int,
        onResult: (icon: Bitmap?) -> Unit
    ) {
        launch {
            val icon = getIcon(packageName, versionCode, size)
            withContext(Dispatchers.Main) {
                onResult(icon)
            }
        }
    }


    override fun getIcon(packageName: String, size: Int, onResult: (icon: Bitmap?) -> Unit) {
        launch {
            val icon = try {
                val info = packageManager.getPackageInfo(packageName, 0)
                getIcon(info, size)
            } catch (e: Exception) {
                getDefaultBitmap(size)
            }
            withContext(Dispatchers.Main) {
                onResult(icon)
            }
        }
    }

    override suspend fun deleteAll() {
        withContext(Dispatchers.IO) {
            cacheDir.listFiles()?.forEach {
                it.delete()
            }
        }
    }


    private suspend fun getIcon(info: PackageInfo, size: Int = defaultIconSize): Bitmap? {
        val version = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            info.longVersionCode
        } else {
            info.versionCode.toLong()
        }

        return getIcon(info.packageName, version, size)
    }


    private suspend fun getIcon(packageName: String, versionCode: Long, size: Int): Bitmap? {
        getIconFile(packageName, versionCode, size)?.let {
            return BitmapFactory.decodeFile(it.path)
        }

        return null
    }

    private suspend fun getIconFile(packageName: String, versionCode: Long, size: Int): File? {
        //Instancio un file
        val iconFile = File(this.cacheDir, makeIconName(packageName, versionCode, size))

        //Si el file no existe es porque o no se ha creado el ícono o tiene una versión diferente
        if (withContext(Dispatchers.IO) {
                !iconFile.exists()
            }) {
            try {
                //Creo o actualizo el ícono
                createOrUpdate(packageName, versionCode, size)
            } catch (e: PackageManager.NameNotFoundException) {
                return null
            }
        }

        return iconFile
    }

    private suspend fun createOrUpdate(packageName: String, versionCode: Long, size: Int) {
        //Obtengo la lista de íconos en cache
        withContext(Dispatchers.IO) { cacheDir.list() }?.forEach { name ->

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

    private suspend fun create(packageName: String, versionCode: Long, size: Int) {
        //File que contendrá el ícono
        val file = File(cacheDir, makeIconName(packageName, versionCode, size))

        if (withContext(Dispatchers.IO) {
                file.createNewFile()
            }) {
            /*Si logré crear el file, construyo el ícono de la aplicación o uno predeterminado de android
              en caso de que la aplicación no tenga ícono*/
            val icon = try {
                getResizedBitmap(
                    drawableToBitmap(packageManager.getApplicationIcon(packageName)),
                    size
                )
            } catch (e: Exception) {
                getDefaultBitmap(size)
            }

            withContext(Dispatchers.IO) {
                //Guardo el ícono en el file
                FileOutputStream(file).use {
                    icon?.compress(Bitmap.CompressFormat.PNG, 100, it)
                }
            }
        }
    }

    /**
     * Actualiza el ícono de una aplicación
     * */
    private suspend fun update(packageName: String, versionCode: Long, size: Int, oldIcon: String) {
        val oldIconFile = File(cacheDir, oldIcon)

        withContext(Dispatchers.IO) {
            if (oldIconFile.exists())
                oldIconFile.delete()
        }

        create(packageName, versionCode, size)
    }

    private fun getDefaultBitmap(size: Int): Bitmap? {
        ContextCompat.getDrawable(
            context,
            android.R.drawable.sym_def_app_icon
        )?.let {
            return getResizedBitmap(
                drawableToBitmap(
                    it
                ), size
            )
        }
        return null
    }

    /**
     * Convierte un Drawable a Bitmap
     *
     * @param drawable - Drawable a convertir
     * @return Bitmap
     * */
    private fun drawableToBitmap(drawable: Drawable): Bitmap {
        if (drawable is BitmapDrawable) {
            if (drawable.bitmap != null) {
                return drawable.bitmap
            }
        }
        val bitmap = if (drawable.intrinsicWidth <= 0 || drawable.intrinsicHeight <= 0) {
            Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
        } else {
            Bitmap.createBitmap(
                drawable.intrinsicWidth,
                drawable.intrinsicHeight,
                Bitmap.Config.ARGB_8888
            )
        }
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }

    /**
     * Redimenciona un Bitmap
     *
     * @param image - Imagen a redimencionar
     * @param size - Tamaño que se le asignará a la imagen
     * @return Imagen redimencionada
     * */
    private fun getResizedBitmap(image: Bitmap, size: Int): Bitmap {
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

    private fun fixSize(size: Int): Int {
        val displayMetrics = context.resources.displayMetrics
        return TypedValue
            .applyDimension(TypedValue.COMPLEX_UNIT_DIP, size.toFloat(), displayMetrics)
            .toInt()
    }

    /**
     * Contruye el nombre del ícono basado en el nombre de paquete y la versión.
     * */
    private fun makeIconName(packageName: String, versionCode: Long, size: Int) =
        "${this.baseIconName}${packageName}_${versionCode}_${size}"

}