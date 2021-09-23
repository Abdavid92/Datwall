package com.smartsolutions.paquetes.ui.history

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.graphics.drawable.toDrawable
import androidx.fragment.app.viewModels
import com.google.android.material.tabs.TabLayoutMediator
import com.smartsolutions.paquetes.databinding.FragmentHistoryBinding
import com.smartsolutions.paquetes.repositories.models.Sim
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class HistoryFragment : Fragment() {

    companion object {
        fun newInstance() = HistoryFragment()
    }

    private val viewModel by viewModels<HistoryViewModel> ()
    private lateinit var binding: FragmentHistoryBinding

    private var adapter: HistoryPagerAdapter? = null


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.getInstalledSims().observe(viewLifecycleOwner){
            try {
                TabLayoutMediator(binding.tabs, binding.pager){ tab, position ->
                    it[position].icon?.let { bitmap ->
                        tab.icon = bitmap.toDrawable(resources)
                    }
                    tab.text = (it[position].slotIndex + 1).toString()
                }.also {
                    if (!it.isAttached){
                        it.attach()
                    }
                }
            }catch (e: Exception){ }

            setAdapter(it)
            if (it.size <= 1){
                binding.tabs.visibility = View.GONE
            }else {
                binding.tabs.visibility = View.VISIBLE
            }
        }
    }


    private fun setAdapter(sims: List<Sim>){
        if (adapter == null){
            adapter = HistoryPagerAdapter(this, sims)
            binding.pager.adapter = adapter
        }else {
            adapter?.sims = sims
            adapter?.notifyDataSetChanged()
        }
    }




}