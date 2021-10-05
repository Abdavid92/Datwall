package com.smartsolutions.paquetes.ui.settings.sim

import android.os.Bundle
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import com.smartsolutions.paquetes.databinding.FragmentDefaultSimsDialogBinding
import com.smartsolutions.paquetes.repositories.models.Sim
import dagger.hilt.android.AndroidEntryPoint

private const val FAILED_DEFAULT = "failed_default"

@AndroidEntryPoint
class DefaultSimsDialogFragment : BottomSheetDialogFragment() {

    private var failed: FailDefault? = null
    private lateinit var binding: FragmentDefaultSimsDialogBinding

    private val viewModel by viewModels<DefaultSimsViewModel> ()
    private var adapter: DefaultSimRecyclerAdapter? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let { bundle ->
            bundle.getString(FAILED_DEFAULT)?.let {
                failed = FailDefault.valueOf(it)
            }
        }
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentDefaultSimsDialogBinding.inflate(inflater, container, false)
        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        viewModel.getInstalledSims().observe(viewLifecycleOwner){
            setAdapter(it)
        }
    }



    private fun setAdapter(sims: List<Sim>){
        if (adapter == null){
            adapter = DefaultSimRecyclerAdapter(sims)
            binding.recycler.adapter = adapter
        }else {
            adapter?.sims = sims
            adapter?.notifyDataSetChanged()
        }
    }

    companion object {

        fun newInstance(failDefault: FailDefault?): DefaultSimsDialogFragment =
            DefaultSimsDialogFragment().apply {
                arguments = Bundle().apply {
                    failDefault?.let {
                        putString(FAILED_DEFAULT, it.name)
                    }
                }
            }

    }

    enum class FailDefault {
        DEFAULT_DATA,
        DEFAULT_VOICE
    }
}