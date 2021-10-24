package com.smartsolutions.paquetes.ui.setup

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.activity.viewModels
import com.smartsolutions.paquetes.R
import com.smartsolutions.paquetes.ui.AbstractActivity
import com.smartsolutions.paquetes.ui.activation.PurchasedFragment
import com.smartsolutions.paquetes.ui.addOpenActivityListener
import com.smartsolutions.paquetes.ui.next
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SetupActivity : AbstractActivity(R.layout.activity_setup), OnCompletedListener {

    private val viewModel by viewModels<SetupViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel.configurations.observe(this) {
            nextOrComplete()
        }

        viewModel.addOpenActivityListener(this) {
            startActivity(Intent(this, it))
            finish()
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
            viewModel.next()
        }
    }
}