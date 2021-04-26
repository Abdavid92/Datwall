package com.smartsolutions.paquetes.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.Menu
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.smartsolutions.paquetes.R
import com.smartsolutions.paquetes.exceptions.UnprocessableRequestException
import com.smartsolutions.paquetes.managers.MiCubacelClientManager
import com.smartsolutions.paquetes.micubacel.models.ProductGroup
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity(R.layout.activity_main) {
    val client = MiCubacelClientManager()

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


    fun singUp(){
        client.signUp("Alexis", "Leon", "53814765", object : MiCubacelClientManager.Callback<Any>{
            override fun onSuccess(response: Any) {
                Log.i("EJV", "Creacion de cuenta iniciado")
            }

            override fun onFail(throwable: Throwable) {
                if (throwable is UnprocessableRequestException){
                    Log.i("EJV", "No se pudo cargar la creacion de la cuenta ${throwable.message}")
                }else {
                    Log.i("EJV", "No se pudo conectar para crear")
                }
            }
        })
    }

    fun verifyCode(code :String){
        client.verifyCode(code, object : MiCubacelClientManager.Callback<Any>{
            override fun onSuccess(response: Any) {
                Log.i("EJV", "Verificado correctamente")
                createPassword()
            }

            override fun onFail(throwable: Throwable) {
                if (throwable is UnprocessableRequestException){
                    Log.i("EJV", "No se pudo verificar ${throwable.message}")
                }else {
                    Log.i("EJV", "No se pudo conectar para verificar el codigo")
                }
            }
        })
    }

    fun createPassword(){
        client.createPassword("Alexis2021", object : MiCubacelClientManager.Callback<Any>{
            override fun onSuccess(response: Any) {
                Log.i("EJV", "Cuenta creada")
            }

            override fun onFail(throwable: Throwable) {
                if (throwable is UnprocessableRequestException){
                    Log.i("EJV", "No se pudo crear la cuenta ${throwable.message}")
                }else {
                    Log.i("EJV", "No se pudo conectar para crear la cuenta")
                }
            }
        })
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }
}