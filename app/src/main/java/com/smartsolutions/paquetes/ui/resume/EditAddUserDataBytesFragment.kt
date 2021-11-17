package com.smartsolutions.paquetes.ui.resume

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.viewModels
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.smartsolutions.paquetes.databinding.FragmentEditAddUserDataBytesBinding
import com.smartsolutions.paquetes.databinding.FragmentEditUserDataBytesBinding
import com.smartsolutions.paquetes.repositories.models.DataBytes
import com.smartsolutions.paquetes.repositories.models.UserDataBytes
import dagger.hilt.android.AndroidEntryPoint

private const val ARG_SIM_ID = "arg_sim_id"
private const val ARG_IS_EDIT = "arg_is_edit"
private const val ARG_DATA_TYPE = "arg_data_type"

@AndroidEntryPoint
class EditAddUserDataBytesFragment : BottomSheetDialogFragment() {

    private val viewModel by viewModels<EditUserDataBytesViewModel>()

    private var _binding: FragmentEditAddUserDataBytesBinding? = null
    private val binding get() = _binding!!

    private var simID: String? = null
    private var isEdit: Boolean? = null
    private var dataType: DataBytes.DataType? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        simID = arguments?.getString(ARG_SIM_ID) ?: throw  IllegalArgumentException()
        isEdit = arguments?.getBoolean(ARG_IS_EDIT) ?: throw IllegalArgumentException()
        val typeName = arguments?.getString(ARG_DATA_TYPE)
        if (typeName == null && isEdit == true) {
            throw IllegalArgumentException()
        }
        typeName?.let {
            dataType = DataBytes.DataType.valueOf(it)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEditAddUserDataBytesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.getUserDataBytes(simID!!).observe(viewLifecycleOwner){



        }


    }


    private fun setSpinnerDataType(list: List<UserDataBytes>) {
        binding.apply {
            val types = mutableListOf<String>()
            list.forEach {
                types.add(it.getName(requireContext()))
            }
            val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, types)
            spinnerDataType.adapter = adapter
        }
    }


    companion object {
        fun newInstance(
            simID: String,
            isEdit: Boolean,
            dataType: DataBytes.DataType?
        ): EditUserDataBytesFragment =
            EditUserDataBytesFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_SIM_ID, simID)
                    putBoolean(ARG_IS_EDIT, isEdit)
                    putString(ARG_DATA_TYPE, dataType?.name)
                }
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}