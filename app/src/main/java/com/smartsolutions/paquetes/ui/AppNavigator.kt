package com.smartsolutions.paquetes.ui

import android.content.Context
import android.content.Intent
import com.smartsolutions.paquetes.managers.contracts.IAppNavigator
import com.smartsolutions.paquetes.ui.exceptions.ExceptionsActivity
import com.smartsolutions.paquetes.ui.settings.SimsConfigurationFragment
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class AppNavigator @Inject constructor(
    @ApplicationContext
    private val context: Context
) : IAppNavigator {

    override fun splash() = Intent(context, SplashActivity::class.java)

    override fun simsConfiguration() = Intent(context, FragmentContainerActivity::class.java)
        .putExtra(FragmentContainerActivity.EXTRA_FRAGMENT, SimsConfigurationFragment::class.java.name)

    override fun updates() = Intent(context, MainActivity::class.java)
        .setAction(MainActivity.ACTION_OPEN_FRAGMENT)
        .putExtra(MainActivity.EXTRA_FRAGMENT, MainActivity.FRAGMENT_UPDATE_DIALOG)

    override fun exceptions() = Intent(context, ExceptionsActivity::class.java)
}
