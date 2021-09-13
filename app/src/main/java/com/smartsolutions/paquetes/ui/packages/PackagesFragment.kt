package com.smartsolutions.paquetes.ui.packages

import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.smartsolutions.paquetes.R

class PackagesFragment : Fragment() {

    companion object {
        fun newInstance() = PackagesFragment()
    }

    private lateinit var viewModel: PackagesViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_packages, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProvider(this).get(PackagesViewModel::class.java)
        // TODO: Use the ViewModel
    }

}