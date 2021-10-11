package com.smartsolutions.paquetes.ui.activation

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.activity.viewModels
import com.smartsolutions.paquetes.DatwallKernel
import com.smartsolutions.paquetes.R
import com.smartsolutions.paquetes.ui.AbstractActivity
import com.smartsolutions.paquetes.ui.setup.OnCompletedListener
import dagger.Lazy
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class ActivationActivity : AbstractActivity(R.layout.activity_activation), OnCompletedListener {

    private val viewModel by viewModels<ActivationViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setSupportActionBar(findViewById(R.id.toolbar))

        supportFragmentManager.beginTransaction()
            .add(R.id.container, ApplicationStatusFragment())
            .commit()

        viewModel.nextActivity().observe(this) {
            startActivity(Intent(this, it))
            finish()
        }
    }

    override fun onCompleted() {
        viewModel.next()
    }
}