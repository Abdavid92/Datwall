package com.smartsolutions.paquetes.ui.setup

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.activity.viewModels
import com.smartsolutions.paquetes.R
import com.smartsolutions.paquetes.ui.activation.PurchasedFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SetupActivity : AppCompatActivity(R.layout.activity_setup), OnCompletedListener {

    private val viewModel by viewModels<SetupViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel.configurations.observe(this) {
            nextOrComplete()
        }
    }

    override fun onCompleted() {
        nextOrComplete()
    }

    private fun nextOrComplete() {
        if (viewModel.hasNextConfiguration()) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.setup_container, viewModel.nextConfiguration()!!.fragment.get())
                .commit()
        } else {
            viewModel.continueWithRun(this)
        }
    }
}