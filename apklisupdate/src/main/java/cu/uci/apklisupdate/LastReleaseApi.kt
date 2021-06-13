package cu.uci.apklisupdate

import cu.uci.apklisupdate.model.ApiResponce
import cu.uci.apklisupdate.model.AppUpdateInfo
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Created by Adrian Arencibia Herrera on 5/29/18.
 * Email: adrian011494@gmail.com
 */
interface LastReleaseApi {
    @GET("v1/application/")
    fun lastRelease(@Query("package_name") appPackage: String): Call<ApiResponce>

}