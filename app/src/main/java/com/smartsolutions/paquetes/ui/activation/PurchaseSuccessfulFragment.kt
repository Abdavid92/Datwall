package com.smartsolutions.paquetes.ui.activation

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.smartsolutions.paquetes.R
import com.smartsolutions.paquetes.ui.settings.AbstractSettingsFragment

class PurchaseSuccessfulFragment : AbstractSettingsFragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(
            R.layout.fragment_purchase_successful,
            container,
            false).apply {

        }
    }
}