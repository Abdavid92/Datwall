package com.smartsolutions.paquetes.ui

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.smartsolutions.paquetes.R
import com.smartsolutions.paquetes.annotations.Networks
import com.smartsolutions.paquetes.managers.MiCubacelManager
import com.smartsolutions.paquetes.managers.SynchronizationManager
import com.smartsolutions.paquetes.repositories.models.MiCubacelAccount
import com.smartsolutions.paquetes.repositories.models.Sim
import com.smartsolutions.paquetes.ui.firewall.LogActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity(R.layout.activity_main) {

    @Inject
    lateinit var synchronizationManager: SynchronizationManager

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
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_log -> {
                obtainPackages()
                startActivity(Intent(this, LogActivity::class.java))
            }
        }
        return true
    }

    private fun obtainPackages(){
        GlobalScope.launch(Dispatchers.IO) {
            synchronizationManager.synchronizeUserDataBytes(Sim("1", 0L, Networks.NETWORK_3G_4G))
        }
    }
}