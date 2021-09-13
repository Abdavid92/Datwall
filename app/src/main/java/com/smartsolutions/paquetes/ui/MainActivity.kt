package com.smartsolutions.paquetes.ui

import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import androidx.core.view.GravityCompat
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.NavOptionsBuilder
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.navigation.NavigationBarView
import com.smartsolutions.paquetes.R
import com.smartsolutions.paquetes.annotations.ApplicationStatus
import com.smartsolutions.paquetes.managers.SynchronizationManager
import com.smartsolutions.paquetes.managers.contracts.IUpdateManager
import com.smartsolutions.paquetes.serverApis.models.AndroidApp
import com.smartsolutions.paquetes.services.BubbleFloatingService
import com.smartsolutions.paquetes.ui.settings.UpdateFragment
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity(R.layout.activity_main), NavigationBarView.OnItemSelectedListener {

    private lateinit var navView: BottomNavigationView
    private lateinit var navController: NavController

    private var ignoreNavigate = false

    @Inject
    lateinit var synchronizationManager: SynchronizationManager

    @Inject
    lateinit var updateManager: IUpdateManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //setSupportActionBar(findViewById(R.id.toolbar))

        navView = findViewById(R.id.nav_view)

        navController = findNavController(R.id.nav_host_fragment)
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        /*val appBarConfiguration = AppBarConfiguration(setOf(
            R.id.navigation_home, R.id.navigation_dashboard, R.id.navigation_notifications
        ))*/

        //setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

        handleIntent()

        navView.setOnItemSelectedListener(this)

        //startService(Intent(this, BubbleFloatingService::class.java))
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

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        val id = item.itemId

        var changeIgnoredNavigation = true

        if (id == R.id.navigation_more) {

            val popup = PopupMenu(this, navView, GravityCompat.END)
            popup.inflate(R.menu.more_nav_menu)
            popup.setOnDismissListener {
                navController.currentDestination?.let {
                    if (changeIgnoredNavigation)
                        ignoreNavigate = true

                    navView.selectedItemId = it.id
                }
            }
            popup.setOnMenuItemClickListener { menuItem ->
                changeIgnoredNavigation = false
                navController.navigate(menuItem.itemId)
                return@setOnMenuItemClickListener true
            }
            popup.show()
        } else {
            if (!ignoreNavigate)
                navController.navigate(id)

            ignoreNavigate = false
        }
        return true
    }

    companion object {
        const val ACTION_OPEN_FRAGMENT = "action_open_fragment"
        const val EXTRA_FRAGMENT = "extra_fragment"
        const val FRAGMENT_UPDATE_DIALOG = "fragment_update_dialog"
    }
}