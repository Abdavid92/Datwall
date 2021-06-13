package cu.uci.apklisupdate

import cu.uci.apklisupdate.base.RestClient
import cu.uci.apklisupdate.model.ApiResponce
import retrofit2.Call

/**
 * Created by Adrian Arencibia Herrera on 5/29/18.
 * Email: adrian011494@gmail.com
 */

class LastReleaseClient : RestClient<LastReleaseApi>(LastReleaseApi::class.java) {

    fun lastRelease(appPackage: String) = mApi.lastRelease(appPackage)
}