package com.smartsolutions.paquetes.helpers

import android.R.id.message
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import javax.inject.Inject


class LocalFileHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {

    /**
     * Lee el archivo proporcionado por el URI como una cadena de texto. Debe tener permisos de URI
     * de lectura.
     * @param uri - La Uri del archivo a Leer
     * @return La cadena del archivo analizado o null si no se pudo analizar el uri
     */
    fun readFile(uri: Uri): String? {
        return try {
            var text = ""
            context.contentResolver.openInputStream(uri)?.use {inputStream ->
                BufferedReader(InputStreamReader(inputStream, "windows-1252")).use { reader ->
                    text = reader.readText()
                    reader.close()
                }
            }
            text
        }catch (e: Exception){
            null
        }
    }

    /**
     * Guarda el json proporcionado en un archivo en el directorio local de la app. No se necesitan
     * permisos de almacenamiento para realizar esta acción
     * @param json El texto a guardar en el archivo
     * @param fileName El nombre del archivo que se guardara
     * @param typeDir La subcarpeta del alamcenamiento local que se utilizara para almacenar el archivo
     * @return La uri del archivo guardado mediante el FileProvider o null si no se pudo guardar el archivo
     */
    fun saveToFileTemporal(json: String, fileName: String, typeDir: String): Uri? {
        try {
            val file = File(context.getExternalFilesDir(typeDir), fileName)

            if (!file.exists()) {
                file.createNewFile()
            }

            file.outputStream().also {
                it.write(json.toByteArray())
            }

            return FileProvider.getUriForFile(
                context,
                AUTHORITY_PROVIDER, file
            )
        }catch (e: Exception){
            return null
        }
    }

    /**
     * Busca el archivo si existe en el almacenamiento local de la app segun la subcarpeta de
     * almacenamiento proporcionada
     * @param fileName Nombre del archivo a buscar
     * @param typeDir Subcarpeta del almacenamiento local donde buscar
     * @return Un pair que contiene el uri obtenido del FileProvider o null si no se encuentra el archivo
     * o no se puede leer
     */
    fun findFileTemporal(fileName: String, typeDir: String): Pair<Uri, String>? {

        try {
            val file = File(context.getExternalFilesDir(typeDir), fileName)

            if (file.exists()) {
                val uri = FileProvider.getUriForFile(
                    context,
                    AUTHORITY_PROVIDER, file
                )
                return Pair(uri, readFile(uri) ?: "")
            }
        }catch (e: Exception){

        }

        return null
    }


    /**
     * Envia mediante un chooser del sistema el archivo proporcionado mediante un uri. Este metodo es
     * util para compartir archivos mediante otras apps
     * @param uri La uri del archivo que se esta tratando de compartir
     * @param mimeType El tipo de archivo que se va a compartir. Esto limita la cantidad de apps que pueden
     * recibirlo a las que solamente hayan especificado este tipo de archivos
     * @param title Titulo que se mostrara en la ventana de compartir del sistema
     * @param description Descripcion breve del archivo que se compartira
     */
    fun sendFile(uri: Uri, mimeType: String, title: String, description: String) {
        val shareIntent: Intent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TITLE, description)
            type = mimeType
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(Intent.createChooser(shareIntent, title))
    }


    /**
     * Envia un correo con un asunto, cuerpo y un archivo adjunto si se le proporciona
     * @param emailAddress Direccion de correo a la cual se enviara el mensaje
     * @param subject Asunto del correo
     * @param body Cuerpo o texto del correo
     * @param attachment Archivo adjunto que se incluira en el correo. Es null por defecto. Si se deja
     * o se pasa en null se ignora
     */
    fun sendFileByEmail(emailAddress: String, subject: String, body: String, attachment: Uri? = null){
        val email = Intent(Intent.ACTION_SEND).apply {
            type = "*/*"
            putExtra(Intent.EXTRA_EMAIL, emailAddress)
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, body)
            attachment?.let {
                putExtra(Intent.EXTRA_STREAM, attachment)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        if (email.resolveActivity(context.packageManager) != null) {
            context.startActivity(email)
        }
    }


    companion object {
        const val AUTHORITY_PROVIDER = "com.smartsolutions.paquetes.provider"
        const val TYPE_DIR_UPDATES = "Updates"
        const val TYPE_DIR_EXCEPTIONS = "Exceptions"
    }

}