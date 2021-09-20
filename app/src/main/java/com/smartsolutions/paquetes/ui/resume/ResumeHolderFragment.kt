package com.smartsolutions.paquetes.ui.resume

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import com.smartsolutions.paquetes.databinding.FragmentResumeHolderBinding
import com.smartsolutions.paquetes.repositories.models.UserDataBytes
import com.smartsolutions.paquetes.ui.usage.UsageAppDetailsRecyclerAdapter
import dagger.hilt.android.AndroidEntryPoint


private const val SIM_ID = "sim_id"
@AndroidEntryPoint
class ResumeHolderFragment : Fragment() {

    private lateinit var simID: String
    private lateinit var binding: FragmentResumeHolderBinding
    private val viewModel by viewModels<ResumeViewModel>()
    private var adapter: UserDataBytesRecyclerAdapter? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
           simID = it.getString(SIM_ID, null) ?: throw NullPointerException()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentResumeHolderBinding.inflate(inflater, container, false)
        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.getUserDataBytes(simID).observe(viewLifecycleOwner) {
            if (it.isEmpty()){
                binding.linNoData.visibility = View.VISIBLE
                binding.recycler.visibility = View.GONE
            }else {
                binding.linNoData.visibility = View.GONE
                binding.recycler.visibility = View.VISIBLE
                setAdapter(it)
            }
        }

    }


    private fun setAdapter(userData: List<UserDataBytes>) {
        if (adapter == null){
            adapter = UserDataBytesRecyclerAdapter(requireContext(), userData)
            binding.recycler.adapter = adapter
        }else {
            adapter!!.update(userData)
        }
    }

    companion object {

        @JvmStatic
        fun newInstance(simID: String) =
            ResumeHolderFragment().apply {
                arguments = Bundle().apply {
                    putString(SIM_ID, simID)
                }
            }
    }
}