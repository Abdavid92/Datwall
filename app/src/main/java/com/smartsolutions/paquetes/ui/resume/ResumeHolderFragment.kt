package com.smartsolutions.paquetes.ui.resume

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import com.smartsolutions.paquetes.databinding.FragmentResumeHolderBinding
import com.smartsolutions.paquetes.repositories.models.DataBytes
import com.smartsolutions.paquetes.repositories.models.UserDataBytes
import dagger.hilt.android.AndroidEntryPoint


private const val SIM_ID = "sim_id"

@AndroidEntryPoint
class ResumeHolderFragment : Fragment() {

    private lateinit var simID: String

    private var _binding: FragmentResumeHolderBinding? = null
    private val binding: FragmentResumeHolderBinding
        get() = _binding!!

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
        _binding = FragmentResumeHolderBinding.inflate(inflater, container, false)
        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.getUserDataBytes(simID).observe(viewLifecycleOwner) {
            if (it.isEmpty()) {
                binding.linNoData.visibility = View.VISIBLE
                binding.recycler.visibility = View.GONE
            } else {
                binding.linNoData.visibility = View.GONE
                binding.recycler.visibility = View.VISIBLE
                setAdapter(it)
            }
        }

    }

    fun showChartUsageGeneral(dataType: DataBytes.DataType) {
        UsageGeneralFragment.newInstance(
            simID,
            dataType.name
        ).show(childFragmentManager, null)
    }

    override fun onPause() {
        super.onPause()
        adapter = null
    }

    private fun setAdapter(userData: List<UserDataBytes>) {
        if (adapter == null) {
            adapter = UserDataBytesRecyclerAdapter(this, userData)
            binding.recycler.adapter = adapter
        } else {
            adapter?.update(userData)
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
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