package com.smartsolutions.paquetes.ui

import android.os.Bundle
import androidx.annotation.LayoutRes
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.asLiveData
import com.smartsolutions.paquetes.PreferencesKeys
import com.smartsolutions.paquetes.R
import com.smartsolutions.paquetes.dataStore
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.runBlocking

abstract class AbstractActivity : AppCompatActivity {

    constructor(): super()

    constructor(@LayoutRes contentLayoutRedId: Int): super(contentLayoutRedId)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val currentTheme = runBlocking {
            dataStore.data.firstOrNull()
                ?.get(PreferencesKeys.APP_THEME) ?: R.style.Theme_Datwall
        }

        if (currentTheme != R.style.Theme_Datwall)
            setTheme(currentTheme)

        /*dataStore.data.asLiveData().observe(this) {
            val newTheme = it[PreferencesKeys.APP_THEME] ?: currentTheme

            if (currentTheme != newTheme) {
                setTheme(newTheme)

                currentTheme = newTheme

                recreate()
            }
        }*/
    }
}