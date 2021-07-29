package com.smartsolutions.paquetes.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.smartsolutions.paquetes.DatwallKernel
import com.smartsolutions.paquetes.R
import com.smartsolutions.paquetes.annotations.ApplicationStatus
import com.smartsolutions.paquetes.managers.SynchronizationManager
import com.smartsolutions.paquetes.managers.contracts.IUpdateManager
import com.smartsolutions.paquetes.serverApis.models.AndroidApp
import com.smartsolutions.paquetes.services.BubbleFloatingService
import com.smartsolutions.paquetes.ui.permissions.SinglePermissionFragment
import com.smartsolutions.paquetes.ui.permissions.StartAccessibilityServiceFragment
import com.smartsolutions.paquetes.ui.settings.UpdateFragment
import com.smartsolutions.paquetes.ui.setup.SetupActivity
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

const val ACTION_OPEN_FRAGMENT = "action_open_fragment"
const val EXTRA_FRAGMENT = "extra_fragment"
const val FRAGMENT_UPDATE_DIALOG = "fragment_update_dialog"

@AndroidEntryPoint
class MainActivity : AppCompatActivity(R.layout.activity_main) {

    @Inject
    lateinit var synchronizationManager: SynchronizationManager

    @Inject
    lateinit var updateManager: IUpdateManager

    @Inject
    lateinit var kernel: DatwallKernel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setSupportActionBar(findViewById(R.id.toolbar))

        val navView: BottomNavigationView = findViewById(R.id.nav_view)

        val navController = findNavController(R.id.nav_host_fragment)
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        val appBarConfiguration = AppBarConfiguration(setOf(
            R.id.navigation_home, R.id.navigation_dashboard, R.id.navigation_notifications
        ))
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

        //startActivity(Intent(this, SetupActivity::class.java))

        kernel.synchronizeDatabaseAndStartWatcher()

        startService(Intent(this, BubbleFloatingService::class.java))

        //handleIntent()
    }


    private fun handleIntent(){
        intent.action?.let { action ->
            if (action == ACTION_OPEN_FRAGMENT) {
                intent.getStringExtra(EXTRA_FRAGMENT)?.let { extra ->
                    when(extra) {
                        FRAGMENT_UPDATE_DIALOG -> {
                            testDownload()
                        }
                        else -> {

                        }
                    }
                }
            }
        }
    }

    fun testDownload() {

        val androidApp = AndroidApp(
            12,
            "Covid",
            "Jefferson.covid19.world.data",
            1,
            4,
            "4.2.0",
            AndroidApp.UpdatePriority.High,
            "Se mejoro mucho esto de las actualizaciones\nAhora todo funciona mucho mejor\nClaro por lo menos en las pruebas que hemos realizado",
            ApplicationStatus.DISCONTINUED,
            false,
            trialPeriod = 12,
            12,
            "",
            "",
            "",
            ""
        )
        //archive.apklis.cu/application/apk/Jefferson.covid19.world.data-v4.apk

        val dialog = UpdateFragment(androidApp)

        dialog.show(supportFragmentManager, "UpdateDialog")

    }


}