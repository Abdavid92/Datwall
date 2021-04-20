package com.smartsolutions.paquetes.ui.firewall

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.activity.viewModels
import com.smartsolutions.paquetes.managers.IconManager
import com.smartsolutions.paquetes.repositories.models.App
import com.smartsolutions.paquetes.watcher.Watcher
import com.smartsolutions.paquetes.R
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class AskActivity : AppCompatActivity(R.layout.activity_ask) {

    private val viewModel: AskViewModel by viewModels()

    private var app: App? = null

    @Inject
    lateinit var iconManager: IconManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        app = intent.getParcelableExtra(Watcher.EXTRA_FOREGROUND_APP)

        if (app == null)
            finish()
    }
}