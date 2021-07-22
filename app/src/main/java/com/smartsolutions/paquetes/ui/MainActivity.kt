package com.smartsolutions.paquetes.ui

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.smartsolutions.paquetes.R
import com.smartsolutions.paquetes.annotations.ApplicationStatus
import com.smartsolutions.paquetes.managers.SynchronizationManager
import com.smartsolutions.paquetes.managers.contracts.IUpdateManager
import com.smartsolutions.paquetes.serverApis.models.AndroidApp
import com.smartsolutions.paquetes.ui.settings.UpdateFragment
import com.smartsolutions.paquetes.ui.setup.SetupActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity(R.layout.activity_main) {

    @Inject
    lateinit var synchronizationManager: SynchronizationManager

    @Inject
    lateinit var updateManager: IUpdateManager

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

        searchUpdate()

    }


    fun searchUpdate(){
        /*GlobalScope.launch(Dispatchers.IO) {
            updateManager.findUpdate()?.let {
                UpdateFragment().show(supportFragmentManager, "UpdateDialog")
            }
        }*/

        val androidApp = AndroidApp(
            12,
            "Covid",
            "cu.xetid.apk.enzona",
            1,
            17100,
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

        val st = "https://archive.apklis.cu/application/apk/Jefferson.covid19.world.data-v4.apk"

       val dialog = UpdateFragment(androidApp)

        //dialog.isCancelable = false

        dialog.show(supportFragmentManager, "UpdateDialog")



    }
}