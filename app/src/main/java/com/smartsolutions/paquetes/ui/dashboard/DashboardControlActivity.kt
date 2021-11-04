package com.smartsolutions.paquetes.ui.dashboard

import android.os.Bundle
import androidx.activity.viewModels
import com.smartsolutions.paquetes.R
import com.smartsolutions.paquetes.ui.TransparentActivity
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class DashboardControlActivity : TransparentActivity(R.layout.activity_dashboard_control) {

    //private lateinit var controls: IControls

    val viewModel by viewModels<DashboardViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val className = intent.getStringExtra(EXTRA_CONTROLS_CLASS_NAME)
            ?: throw IllegalArgumentException("The class name extra must be provided")

        /*controls = Class.forName(className)
            .getDeclaredConstructor(DashboardControlActivity::class.java)
            .newInstance(this) as IControls

        setContentView(controls.getRoot())

        controls.init()*/

        val fragment = supportFragmentManager.fragmentFactory.instantiate(
            classLoader,
            className
        )

        supportFragmentManager.beginTransaction()
            .add(R.id.container, fragment)
            .commit()
    }

    override fun onBackPressed() {
        //controls.onBackPressed()
        super.onBackPressed()
    }

    override fun onDestroy() {
        //controls.onDestroy()
        super.onDestroy()
    }

    companion object {
        const val EXTRA_CONTROLS_CLASS_NAME = "com.smartsolutions.paquetes.ui.dashboard.extra.CONTROLS_CLASS_NAME"
    }
}