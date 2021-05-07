package com.smartsolutions.paquetes.ui.firewall

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.smartsolutions.paquetes.R
import com.smartsolutions.paquetes.databinding.FirewallFragmentBinding
import com.smartsolutions.paquetes.managers.IconManager
import com.smartsolutions.paquetes.repositories.models.IApp
import com.smartsolutions.paquetes.ui.ApplicationFragment
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class FirewallFragment : ApplicationFragment() {

    private val viewModel by viewModels<FirewallViewModel>()

    private lateinit var binding: FirewallFragmentBinding

    @Inject
    lateinit var iconManager: IconManager

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.firewall_fragment, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.appsList.layoutManager = LinearLayoutManager(requireContext())

        viewModel.getApps().observe(viewLifecycleOwner, {
            val adapter = AppsListAdapter(it, iconManager)
            adapter.onAccessChange = ::onAccessChange
            binding.appsList.adapter = adapter
        })
    }

    private fun onAccessChange(app: IApp) {
        viewModel.updateApp(app)
    }
}