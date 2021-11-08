package com.smartsolutions.paquetes.ui.activation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import com.smartsolutions.paquetes.R
import com.smartsolutions.paquetes.databinding.FragmentPurchase2Binding
import com.smartsolutions.paquetes.ui.settings.AbstractSettingsFragment


class Purchase2Fragment : AbstractSettingsFragment() {

    private var _binding: FragmentPurchase2Binding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPurchase2Binding.inflate(layoutInflater, container, false)
        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        childFragmentManager.commit {
            add(R.id.container, TermsAndConditionsFragment.newInstance())
        }

    }


    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }


    companion object {
        fun newInstance() = Purchase2Fragment()
    }

}