package com.smartsolutions.datwall.managers

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

@RequiresApi(Build.VERSION_CODES.LOLLIPOP_MR1)
class DataPackageManager @Inject constructor(
    @ApplicationContext
    private val context: Context
): IDataPackageManager {

    private val subscriptionManager = ContextCompat
        .getSystemService(context, SubscriptionManager::class.java) ?: throw NullPointerException()

    private val telephonyManager = ContextCompat
        .getSystemService(context, TelephonyManager::class.java) ?: throw NullPointerException()

    @SuppressLint("HardwareIds")
    fun foo() {
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_PHONE_STATE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return
        }

        val result = subscriptionManager.activeSubscriptionInfoList

        if (result.size == 0)
            throw Exception()

        val info = subscriptionManager.getActiveSubscriptionInfo(result[0].subscriptionId)

        try {
            val method = telephonyManager.javaClass.getDeclaredMethod("getDefaultSim")

            method.isAccessible = true

            val defaultSim = method.invoke(telephonyManager) as Int

            print(defaultSim)

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

}