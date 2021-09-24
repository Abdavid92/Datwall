package com.smartsolutions.paquetes.ui.activation

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
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

    @Inject
    lateinit var kernel: Lazy<DatwallKernel>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setSupportActionBar(findViewById(R.id.toolbar))

        supportFragmentManager.beginTransaction()
            .add(R.id.container, ApplicationStatusFragment())
            .commit()
    }

    override fun onCompleted() {
        kernel.get().main(this)
    }
}