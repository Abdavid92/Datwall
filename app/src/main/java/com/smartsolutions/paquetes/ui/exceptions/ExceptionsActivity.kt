package com.smartsolutions.paquetes.ui.exceptions

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Process
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import com.smartsolutions.paquetes.R
import com.smartsolutions.paquetes.exceptions.ExceptionsController
import com.smartsolutions.paquetes.exceptions.UnprocessableRequestException
import com.smartsolutions.paquetes.helpers.LocalFileHelper
import com.smartsolutions.paquetes.ui.AbstractActivity
import com.smartsolutions.paquetes.ui.SplashActivity
import com.smartsolutions.paquetes.ui.permissions.SinglePermissionFragment
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlin.system.exitProcess

@AndroidEntryPoint
class ExceptionsActivity : AbstractActivity() {

    @Inject
    lateinit var localFileHelper: LocalFileHelper


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_exceptions)

        val exceptionFile = localFileHelper.findFileTemporal(ExceptionsController.EXCEPTION_FILE_NAME, LocalFileHelper.TYPE_DIR_EXCEPTIONS)

        val editUserInformation = findViewById<EditText>(R.id.edit_user_describer)
        val sendReport = findViewById<Button>(R.id.button_send_report)
        val closeApp = findViewById<Button>(R.id.button_close_app)
        val restart = findViewById<Button>(R.id.button_restart)
        val image = findViewById<ImageView>(R.id.image_file_report)



        sendReport.setOnClickListener {
            localFileHelper.sendFileByEmail(
                "smartsolutions.apps.cuba@gmail.com",
                "Informe de Error Datwall",
                editUserInformation.text.toString(),
                exceptionFile?.second
            )
        }

        closeApp.setOnClickListener {
            close()
        }

        restart.setOnClickListener {
            startActivity(Intent(this, SplashActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            finish()
        }
    }

    override fun onBackPressed() {
        close()
    }

    private fun close() {
        finishAffinity()
        Process.killProcess(Process.myPid())
        exitProcess(10)
    }
}