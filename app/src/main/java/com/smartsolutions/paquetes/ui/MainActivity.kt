package com.smartsolutions.paquetes.ui

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.menu.MenuBuilder
import androidx.appcompat.widget.PopupMenu
import androidx.core.view.GravityCompat
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.ui.NavigationUI
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.navigation.NavigationBarView
import com.smartsolutions.paquetes.R
import com.smartsolutions.paquetes.databinding.ActivityMainBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity(), NavigationBarView.OnItemSelectedListener {

    private lateinit var navController: NavController

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setTheme(R.style.Theme_Datwall_Blue)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        navController = findNavController(R.id.nav_host_fragment)


                binding.navView.setupWithNavController(navController)
        binding.navView.setOnItemSelectedListener(this)

        handleIntent()
    }



    private fun handleIntent(){
        intent.action?.let { action ->
            if (action == ACTION_OPEN_FRAGMENT) {
                intent.getStringExtra(EXTRA_FRAGMENT)?.let { extra ->
                    when(extra) {
                        FRAGMENT_UPDATE_DIALOG -> {
                            //TODO: Crear un método que saque el diálogo de las actualizaciones
                        }
                        else -> {

                        }
                    }
                }
            }
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        val id = item.itemId

        if (id == R.id.navigation_more) {

            val popup = PopupMenu(this, binding.navView, GravityCompat.END)
            popup.inflate(R.menu.more_nav_menu)
            popup.menu.showIcons(true)
            popup.setOnDismissListener {
                navController.currentDestination?.let {
                    binding.navView.menu.findItem(it.id)
                        ?.isChecked = true
                }
            }
            popup.setOnMenuItemClickListener { menuItem ->
                if (navController.currentDestination?.id != menuItem.itemId)
                    return@setOnMenuItemClickListener NavigationUI
                        .onNavDestinationSelected(menuItem, navController)

                return@setOnMenuItemClickListener false
            }

            popup.show()
        } else {
            if (navController.currentDestination?.id != id)
                return NavigationUI.onNavDestinationSelected(item, navController)
        }

        return true
    }

    companion object {
        const val ACTION_OPEN_FRAGMENT = "action_open_fragment"
        const val EXTRA_FRAGMENT = "extra_fragment"
        const val FRAGMENT_UPDATE_DIALOG = "fragment_update_dialog"
    }
}

@SuppressLint("RestrictedApi")
fun Menu.showIcons(show: Boolean) {
    try {
        (this as MenuBuilder).setOptionalIconsVisible(show)
    } catch (e: Exception) { }
}