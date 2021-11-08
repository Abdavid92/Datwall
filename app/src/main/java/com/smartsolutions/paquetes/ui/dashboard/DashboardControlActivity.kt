package com.smartsolutions.paquetes.ui.dashboard

import android.os.Bundle
import androidx.activity.viewModels
import com.smartsolutions.paquetes.R
import com.smartsolutions.paquetes.ui.TransparentActivity
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class DashboardControlActivity : TransparentActivity(R.layout.activity_dashboard_control) {

    val viewModel by viewModels<DashboardViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val className = intent.getStringExtra(EXTRA_CONTROLS_CLASS_NAME)
            ?: throw IllegalArgumentException("The class name extra must be provided")

        val fragment = supportFragmentManager.fragmentFactory.instantiate(
            classLoader,
            className
        )

        supportFragmentManager.beginTransaction()
            .add(R.id.container, fragment)
            .commit()
    }

    companion object {
        const val EXTRA_CONTROLS_CLASS_NAME = "com.smartsolutions.paquetes.ui.dashboard.extra.CONTROLS_CLASS_NAME"

        const val CARD_VIEW = "control:card_view"
        const val HEADER = "control:header"
        const val SWITCH = "control:switch"
        const val SUMMARY = "control:summary"
    }
}