package com.smartsolutions.paquetes.ui.setup

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.smartsolutions.paquetes.R
import com.smartsolutions.paquetes.ui.settings.ApplicationStatusFragment
import com.smartsolutions.paquetes.ui.settings.PurchasedFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SetupActivity : AppCompatActivity(R.layout.activity_setup) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportFragmentManager.beginTransaction()
            .add(R.id.setup_container, PurchasedFragment())
            .commit()
    }
}