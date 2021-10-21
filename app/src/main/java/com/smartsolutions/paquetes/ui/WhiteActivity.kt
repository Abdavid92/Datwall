package com.smartsolutions.paquetes.ui

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.smartsolutions.paquetes.R

class WhiteActivity : AppCompatActivity(R.layout.activity_white) {
    override fun onStart() {
        super.onStart()
        finish()
    }
}