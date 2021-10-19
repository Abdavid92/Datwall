package com.smartsolutions.paquetes.ui.exceptions

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Process
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import com.smartsolutions.paquetes.BuildConfig
import com.smartsolutions.paquetes.DatwallKernel
import com.smartsolutions.paquetes.R
import com.smartsolutions.paquetes.databinding.ActivityExceptionsBinding
import com.smartsolutions.paquetes.exceptions.ExceptionsController
import com.smartsolutions.paquetes.exceptions.UnprocessableRequestException
import com.smartsolutions.paquetes.helpers.LocalFileHelper
import com.smartsolutions.paquetes.services.BubbleFloatingService
import com.smartsolutions.paquetes.services.DatwallService
import com.smartsolutions.paquetes.ui.AbstractActivity
import com.smartsolutions.paquetes.ui.SplashActivity
import com.smartsolutions.paquetes.ui.permissions.SinglePermissionFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.runBlocking
import javax.inject.Inject
import kotlin.system.exitProcess

@AndroidEntryPoint
class ExceptionsActivity : AbstractActivity() {

    @Inject
    lateinit var localFileHelper: LocalFileHelper

    private lateinit var binding: ActivityExceptionsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityExceptionsBinding.inflate(layoutInflater)

        setContentView(binding.root)

        val exceptionFile = localFileHelper.findFileTemporal(
            ExceptionsController.EXCEPTION_FILE_NAME,
            LocalFileHelper.TYPE_DIR_EXCEPTIONS
        )

        binding.buttonSendReport.setOnClickListener {
            localFileHelper.sendFileByEmail(
                "smartsolutions.apps.cuba@gmail.com",
                "Informe de Error Datwall",
                binding.editMoreInfo.text.toString(),
                if (binding.checkboxInclude.isChecked) {
                    exceptionFile?.second
                } else {
                    null
                }
            )
        }

        binding.buttonCloseApp.setOnClickListener {
            close()
        }

    }

    override fun onBackPressed() {
        close()
    }

    private fun close() {
        kotlin.runCatching {
            stopService(Intent(this, BubbleFloatingService::class.java))
        }
        kotlin.runCatching {
            stopService(Intent(this, DatwallService::class.java))
        }
        finishAffinity()
        Process.killProcess(Process.myPid())
        exitProcess(0)
    }
}