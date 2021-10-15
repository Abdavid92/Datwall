package com.smartsolutions.paquetes.ui

import android.os.Bundle
import androidx.annotation.LayoutRes
import androidx.appcompat.app.AppCompatActivity
import com.smartsolutions.paquetes.PreferencesKeys
import com.smartsolutions.paquetes.R
import com.smartsolutions.paquetes.settingsDataStore
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.runBlocking

abstract class AbstractActivity : AppCompatActivity {

    private var currentTheme: Int = R.style.Theme_Datwall

    constructor(): super()

    constructor(@LayoutRes contentLayoutRedId: Int): super(contentLayoutRedId)

    override fun onCreate(savedInstanceState: Bundle?) {

        currentTheme = getThemeConfigured()

        if (currentTheme != R.style.Theme_Datwall) {
            setTheme(currentTheme)
        }

        super.onCreate(savedInstanceState)
    }

    override fun onResume() {
        super.onResume()
        if (currentTheme != getThemeConfigured()){
            recreate()
        }
    }


    private fun getThemeConfigured(): Int{
        return runBlocking {
            settingsDataStore.data.firstOrNull()
                ?.get(PreferencesKeys.APP_THEME) ?: R.style.Theme_Datwall
        }
    }
}