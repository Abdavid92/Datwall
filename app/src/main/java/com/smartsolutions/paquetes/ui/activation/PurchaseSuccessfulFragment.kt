package com.smartsolutions.paquetes.ui.activation

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.appcompat.widget.AppCompatButton
import com.smartsolutions.paquetes.R
import com.smartsolutions.paquetes.ui.settings.AbstractSettingsFragment

class PurchaseSuccessfulFragment : AbstractSettingsFragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        return inflater.inflate(
            R.layout.fragment_purchase_successful,
            container,
            false).apply {

        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<Button>(R.id.btn_continue).setOnClickListener {
            complete()
        }
    }
}