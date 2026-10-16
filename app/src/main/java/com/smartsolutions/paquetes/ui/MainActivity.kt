package com.smartsolutions.paquetes.ui

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.view.menu.MenuBuilder
import androidx.appcompat.widget.PopupMenu
import androidx.core.view.GravityCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.navigation.NavController
import androidx.navigation.fragment.FragmentNavigator
import androidx.navigation.findNavController
import androidx.navigation.ui.NavigationUI
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.navigation.NavigationBarView
import com.smartsolutions.paquetes.services.BubbleFloatingService
import com.smartsolutions.paquetes.R
import com.smartsolutions.paquetes.databinding.ActivityMainBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AbstractActivity(), NavigationBarView.OnItemSelectedListener {

    private lateinit var navController: NavController


    private var _binding: ActivityMainBinding? = null
    private val binding: ActivityMainBinding
        get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        _binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        navController = findNavController(R.id.nav_host_fragment)

        binding.navView.setupWithNavController(navController)
        binding.navView.setOnItemSelectedListener(this)

        followDisplayedScreen()
    }

    /**
     * Mantiene resaltada en la barra la pestaña de la pantalla que se está mostrando.
     *
     * Con el retroceso predictivo (Android 13+) el contenido puede cambiar al volver atrás sin
     * que el destino de la navegación lo notifique a tiempo, y la barra se quedaba en la pestaña
     * anterior. Seguir a la pantalla que llega a primer plano no depende de ese aviso.
     * */
    private fun followDisplayedScreen() {
        val navHost = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) ?: return

        navHost.childFragmentManager.registerFragmentLifecycleCallbacks(
            object : FragmentManager.FragmentLifecycleCallbacks() {
                override fun onFragmentResumed(fm: FragmentManager, f: Fragment) {
                    val destination = navController.graph.firstOrNull {
                        it is FragmentNavigator.Destination && it.className == f.javaClass.name
                    } ?: return

                    val menu = binding.navView.menu

                    // Las pantallas del menú "Más" no tienen pestaña propia: se resalta "Más".
                    val item = menu.findItem(destination.id) ?: menu.findItem(R.id.navigation_more)

                    if (item != null && !item.isChecked)
                        item.isChecked = true
                }
            },
            false
        )
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

    override fun onResume() {
        super.onResume()

        LocalBroadcastManager.getInstance(this).sendBroadcast(
            Intent(BubbleFloatingService.ACTION_MAIN_IS_OPEN)
        )

    }

    override fun onDestroy() {
        _binding = null
        super.onDestroy()
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
    } catch (e: Exception) {
    }
}