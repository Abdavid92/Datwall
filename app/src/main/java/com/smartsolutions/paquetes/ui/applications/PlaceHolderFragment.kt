package com.smartsolutions.paquetes.ui.applications

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.smartsolutions.paquetes.R
import com.smartsolutions.paquetes.databinding.FragmentApplicationsPlaceholderBinding

class PlaceHolderFragment private constructor(

) : Fragment() {

    private lateinit var binding: FragmentApplicationsPlaceholderBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentApplicationsPlaceholderBinding.inflate(
            inflater,
            container,
            false
        )
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


    }

    companion object {

        fun newInstance(): PlaceHolderFragment {
            val fragment = PlaceHolderFragment()

            return fragment
        }
    }
}