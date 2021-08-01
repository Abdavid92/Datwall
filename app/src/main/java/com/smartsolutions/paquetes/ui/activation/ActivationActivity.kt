package com.smartsolutions.paquetes.ui.activation

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.smartsolutions.paquetes.DatwallKernel
import com.smartsolutions.paquetes.R
import dagger.Lazy
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class ActivationActivity : AppCompatActivity(R.layout.activity_activation) {

    @Inject
    lateinit var kernel: Lazy<DatwallKernel>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setSupportActionBar(findViewById(R.id.toolbar))

        supportFragmentManager.beginTransaction()
            .add(
                R.id.container,
                ApplicationStatusFragment()
                    .setOnCompletedListener {
                        GlobalScope.launch {
                            kernel.get().mainInForeground(this@ActivationActivity)
                        }
                    }
            ).commit()
    }
}