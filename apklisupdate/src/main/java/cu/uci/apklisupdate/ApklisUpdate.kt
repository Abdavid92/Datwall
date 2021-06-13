package cu.uci.apklisupdate

import android.content.Context
import android.os.Build
import cu.uci.apklisupdate.model.ApiResponce
import cu.uci.apklisupdate.model.AppUpdateInfo
import cu.uci.apklisupdate.model.Result
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine


object ApklisUpdate {

    suspend fun getAppUpdate(packageName: String): Result<AppUpdateInfo> {
        return suspendCoroutine {
            hasAppUpdate(packageName = packageName, callback = object : UpdateCallback {
                override fun onNewUpdate(appUpdateInfo: AppUpdateInfo) {
                    it.resume(Result.Success(appUpdateInfo))
                }

                override fun onOldUpdate(appUpdateInfo: AppUpdateInfo) {
                    it.resume(Result.Success(appUpdateInfo))
                }

                override fun onError(e: Throwable) {
                    it.resume(Result.Failure(e))
                }

            })
        }
    }

    fun hasAppUpdate(context: Context, callback: UpdateCallback) {

        val manager = context.packageManager
        val info = manager.getPackageInfo(
            context.packageName, 0
        )

        val versionCode: Long = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            info.longVersionCode
        } else {
            info.versionCode.toLong()
        }

        hasAppUpdate(context.packageName, versionCode, callback)

    }

    fun hasAppUpdate(packageName: String, versionCode: Long = 0, callback: UpdateCallback) {

        LastReleaseClient()
            .lastRelease(packageName)
            .enqueue(object : Callback<ApiResponce> {
                override fun onResponse(call: Call<ApiResponce>, response: Response<ApiResponce>) {

                    val appUpdateInfo = response.body()
                        ?.results
                        ?.firstOrNull()

                    if (appUpdateInfo != null) {
                        if (versionCode < appUpdateInfo.last_release.version_code)
                            callback.onNewUpdate(appUpdateInfo)
                        else
                            callback.onOldUpdate(appUpdateInfo)
                    } else {
                        callback.onError(Exception("Not data"))
                    }
                }

                override fun onFailure(call: Call<ApiResponce>, e: Throwable) {
                    callback.onError(e)
                }

            })
    }
}