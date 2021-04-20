package com.smartsolutions.paquetes

import android.os.Bundle
import android.util.Log
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.smartsolutions.datwall.managers.MiCubacelClientManager
import com.smartsolutions.datwall.repositories.models.UserDataPackage
import com.smartsolutions.micubacel_client.exceptions.UnprocessableRequestException
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity(R.layout.activity_main) {
    val client = MiCubacelClientManager()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val navView: BottomNavigationView = findViewById(R.id.nav_view)

        val navController = findNavController(R.id.nav_host_fragment)
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        val appBarConfiguration = AppBarConfiguration(setOf(
                R.id.navigation_home, R.id.navigation_dashboard, R.id.navigation_notifications))
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

      singIn()
    }

    fun singIn(){
        client.signIn("52379969", "Geaelf*1736#", object : MiCubacelClientManager.Callback<Any>{
            override fun onSuccess(response: Any) {
                Log.i("EJV", "LOGEADO")
                client.getUserDataPackagesInfo(null, object : MiCubacelClientManager.Callback<UserDataPackage> {
                    override fun onSuccess(response: UserDataPackage) {

                    }

                    override fun onFail(throwable: Throwable) {

                    }

                })
            }

            override fun onFail(throwable: Throwable) {
                if (throwable is UnprocessableRequestException){
                    Log.i("EJV", "Nombre de usuario o contraseña incorrecto")
                }else {
                    Log.i("EJV", "NO SE PUDO INICIAR SESION")
                }
            }

        })
    }

    fun loadHome(){
        client.loadHomePage(object : MiCubacelClientManager.Callback<Map<String, String>>{
            override fun onSuccess(response: Map<String, String>) {
                response.forEach {
                    Log.i("EJV", "onSuccess: key -> ${it.key}, value -> ${it.value} ")
                }
            }

            override fun onFail(throwable: Throwable) {
                if (throwable is UnprocessableRequestException){
                    Log.i("EJV", "Pagina vacia, NO se ha iniciado sesion")
                }else{
                    Log.i("EJV", "No se pudo obtener los datos")
                }
            }

        }, false)
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
        client.verifyCode(code, object :MiCubacelClientManager.Callback<Any>{
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
}