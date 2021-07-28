package com.smartsolutions.paquetes.ui.setup

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.activity.viewModels
import com.smartsolutions.paquetes.R
import com.smartsolutions.paquetes.ui.settings.ApplicationStatusFragment
import com.smartsolutions.paquetes.ui.settings.PackagesConfigurationFragment
import com.smartsolutions.paquetes.ui.settings.PurchasedFragment
import com.smartsolutions.paquetes.ui.settings.SimsConfigurationFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SetupActivity : AppCompatActivity(R.layout.activity_setup) {

    private val viewModel by viewModels<SetupViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportFragmentManager.beginTransaction()
            .add(R.id.setup_container, PurchasedFragment())
            .commit()
    }
}