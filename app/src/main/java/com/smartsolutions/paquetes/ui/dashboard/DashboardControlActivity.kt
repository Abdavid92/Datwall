package com.smartsolutions.paquetes.ui.dashboard

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.activity.viewModels
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class DashboardControlActivity : AppCompatActivity() {

    private lateinit var controls: IControls

    val viewModel by viewModels<DashboardViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val className = intent.getStringExtra(EXTRA_CONTROLS_CLASS_NAME)
            ?: throw IllegalArgumentException("The class name extra must be provided")

        controls = Class.forName(className)
            .getDeclaredConstructor(DashboardControlActivity::class.java)
            .newInstance(this) as IControls

        setContentView(controls.getRoot())

        controls.init()
    }

    override fun onBackPressed() {
        controls.onBackPressed()
        super.onBackPressed()
    }

    companion object {
        const val EXTRA_CONTROLS_CLASS_NAME = "com.smartsolutions.paquetes.ui.dashboard.extra.CONTROLS_CLASS_NAME"
    }
}