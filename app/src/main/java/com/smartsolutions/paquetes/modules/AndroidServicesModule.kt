package com.smartsolutions.paquetes.modules

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.app.usage.NetworkStatsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.preference.PreferenceManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
class AndroidServicesModule {

    @Provides
    fun providePackageManager(@ApplicationContext context: Context): PackageManager = context.packageManager

    @Provides
    @SuppressLint("InlinedApi")
    fun provideUsageStatsManager(@ApplicationContext context: Context): UsageStatsManager {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1)
            ContextCompat.getSystemService(context, UsageStatsManager::class.java) ?: throw NullPointerException()
        else
            context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
    }

    @Provides
    fun provideActivityManager(@ApplicationContext context: Context): ActivityManager =
        ContextCompat.getSystemService(context, ActivityManager::class.java) ?: throw NullPointerException()

    @Provides
    @RequiresApi(Build.VERSION_CODES.M)
    fun provideNetworkStatsManager(@ApplicationContext context: Context): NetworkStatsManager =
        ContextCompat.getSystemService(context, NetworkStatsManager::class.java) ?: throw NullPointerException()

    @Provides
    fun provideTelephonyManager(@ApplicationContext context: Context): TelephonyManager =
        ContextCompat.getSystemService(context, TelephonyManager::class.java) ?: throw NullPointerException()

    @Provides
    fun provideLocalBroadcastManager(@ApplicationContext context: Context) =
        LocalBroadcastManager.getInstance(context)

    @Provides
    @RequiresApi(Build.VERSION_CODES.LOLLIPOP_MR1)
    fun provideSubscriptionManager(@ApplicationContext context: Context) =
        ContextCompat.getSystemService(context, SubscriptionManager::class.java) ?: throw NullPointerException()

    @Provides
    fun providePreferences(@ApplicationContext context: Context): SharedPreferences =
        PreferenceManager.getDefaultSharedPreferences(context)
}