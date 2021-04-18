package com.smartsolutions.datwall.webApis

import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.GET

/**
 * Interfaz que se usa con Retrofit para conectarse con la
 * web api de Datwall.
 * */
interface DatwallWebApi {

    /**
     * Obtiene todos los paquetes.
     *
     * @return Call que se usa para ejecutar la petición http y obtener
     * una lista de paquetes.
     * */
    @GET("dataPackage")
    fun getPackages(): Call<ResponseBody>
}