package com.smartsolutions.paquetes.ui.firewall

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.smartsolutions.paquetes.repositories.models.App
import com.smartsolutions.paquetes.R
import com.smartsolutions.paquetes.databinding.ActivityAskBinding
import com.smartsolutions.paquetes.ui.AbstractActivity
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AskActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAskBinding

    private val viewModel: AskViewModel by viewModels()

    private var app: App? = null

    private var allowClose = false

    private val restoreReceiver = object : BroadcastReceiver() {

        override fun onReceive(context: Context?, intent: Intent?) {

            if (intent?.action == ACTION_RESTORE_ACTIVITY) {

                Handler(Looper.getMainLooper())
                    .postDelayed({
                        val app = intent.extras?.getParcelable<App>(EXTRA_FOREGROUND_APP) ?:
                        return@postDelayed

                        val currentForegroundApp = viewModel.getForegroundApp()

                        if (app.packageName == currentForegroundApp) {
                            startActivity(Intent(context, AskActivity::class.java)
                                .putExtra(EXTRA_FOREGROUND_APP, app)
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                        }

                    }, 500)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = DataBindingUtil.setContentView(this, R.layout.activity_ask)

        app = intent.getParcelableExtra(EXTRA_FOREGROUND_APP)

        if (app != null) {
            init(app!!)
        } else {
            finish()
        }

        registerReceiver(restoreReceiver, IntentFilter(ACTION_RESTORE_ACTIVITY))
    }

    private fun init(app: App) {
        binding.app = app

        viewModel.fillIcon(binding.icon, app)

        binding.btnAllow.setOnClickListener {

            app.tempAccess = true

            if (binding.rememberElection.isChecked) {
                app.foregroundAccess = true
            }

            viewModel.updateApp(app) {
                finish()
            }
        }

        binding.btnBlock.setOnClickListener {

            if (binding.rememberElection.isChecked) {
                app.ask = false

                viewModel.updateApp(app) {
                    finish()
                }
            } else {
                finish()
            }
        }

        binding.cardView.setOnClickListener {
            //Empty
        }

        binding.root.setOnClickListener {
            finish()
        }
    }

    override fun finish() {
        allowClose = true
        super.finish()
    }

    override fun onPause() {
        super.onPause()

        if (!allowClose)
            sendBroadcast(Intent(ACTION_RESTORE_ACTIVITY)
                .putExtra(EXTRA_FOREGROUND_APP, app))
    }

    override fun onDestroy() {
        super.onDestroy()

        if (allowClose) {
            unregisterReceiver(restoreReceiver)
        }
    }

    companion object {
        const val EXTRA_FOREGROUND_APP = "com.smartsolutions.paquetes.extra.FOREGROUND_APP"
        private const val ACTION_RESTORE_ACTIVITY = "com.smartsolutions.paquetes.action.RESTORE_ACTIVITY"
    }
}