package com.smartsolutions.paquetes.ui.events

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.smartsolutions.paquetes.R
import com.smartsolutions.paquetes.databinding.FragmentEventsBinding

class EventsFragment : Fragment() {

    private var _binding: FragmentEventsBinding? = null

    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEventsBinding.inflate(layoutInflater, container, false)
        return binding.root
    }


    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }

    companion object {

        @JvmStatic
        fun newInstance() = EventsFragment()

    }
}