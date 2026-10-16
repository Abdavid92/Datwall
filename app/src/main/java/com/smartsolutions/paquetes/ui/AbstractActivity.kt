package com.smartsolutions.paquetes.ui

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.annotation.LayoutRes
import androidx.appcompat.app.AppCompatActivity
import com.smartsolutions.paquetes.R
import com.smartsolutions.paquetes.repositories.contracts.preferencesRepository

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

        enableEdgeToEdge()
    }

    override fun onContentChanged() {
        super.onContentChanged()

        applySystemBarsPaddingToContent()
    }

    override fun onResume() {
        super.onResume()
        if (currentTheme != getThemeConfigured()){
            recreate()
        }
    }


    private fun getThemeConfigured(): Int =
        preferencesRepository().preferences.value.appTheme ?: R.style.Theme_Datwall
}