package com.smartsolutions.paquetes.ui.activation

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import com.smartsolutions.paquetes.R
import com.smartsolutions.paquetes.ui.AbstractActivity
import com.smartsolutions.paquetes.ui.addOpenActivityListener
import com.smartsolutions.paquetes.ui.next
import com.smartsolutions.paquetes.ui.setup.OnCompletedListener
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ActivationActivity : AbstractActivity(R.layout.activity_activation), OnCompletedListener {

    private val viewModel by viewModels<ActivationViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setSupportActionBar(findViewById(R.id.toolbar))

        supportFragmentManager.beginTransaction()
            .add(R.id.container, ApplicationStatusFragment())
            .commit()

        /*viewModel.nextActivity().observe(this) {
            startActivity(Intent(this, it))
            finish()
        }*/
        viewModel.addOpenActivityListener(this) {
            startActivity(Intent(this, it))
            finish()
        }
    }

    override fun onCompleted() {
        viewModel.next()
    }
}