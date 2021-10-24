package com.smartsolutions.paquetes.ui.exceptions

import android.content.Intent
import android.os.Bundle
import android.os.Process
import com.smartsolutions.paquetes.databinding.ActivityExceptionsBinding
import com.smartsolutions.paquetes.exceptions.ExceptionsController
import com.smartsolutions.paquetes.helpers.LocalFileHelper
import com.smartsolutions.paquetes.services.BubbleFloatingService
import com.smartsolutions.paquetes.services.DatwallService
import com.smartsolutions.paquetes.ui.AbstractActivity
import dagger.hilt.android.AndroidEntryPoint
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