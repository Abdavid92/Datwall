package com.smartsolutions.paquetes.ui.resume

import android.content.Context
import android.database.DataSetObserver
import android.os.Bundle
import android.renderscript.Element
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.BaseAdapter
import android.widget.SpinnerAdapter
import android.widget.TextView
import androidx.fragment.app.viewModels
import com.smartsolutions.paquetes.R
import com.smartsolutions.paquetes.databinding.FragmentEditUserDataBytesBinding
import com.smartsolutions.paquetes.helpers.getDataTypeName
import com.smartsolutions.paquetes.managers.models.DataUnitBytes
import com.smartsolutions.paquetes.repositories.models.DataBytes
import com.smartsolutions.paquetes.repositories.models.UserDataBytes
import dagger.hilt.android.AndroidEntryPoint
import java.lang.NullPointerException
import kotlin.properties.Delegates

private const val ARG_SIM_ID = "arg_sim_id"
private const val ARG_IS_EDIT = "arg_is_edit"
private const val ARG_DATA_TYPE = "arg_data_type"

@AndroidEntryPoint
class EditUserDataBytesFragment : BottomSheetDialogFragment() {

    private val viewModel by viewModels<EditUserDataBytesViewModel>()

    private var _binding: FragmentEditUserDataBytesBinding? = null
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
        dataType = DataBytes.DataType.valueOf(typeName!!)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEditUserDataBytesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        viewModel.getUserDataBytes(simID!!).observe(viewLifecycleOwner) {

            val list = if (isEdit == true) {
                binding.title.text = getString(
                    R.string.edit_user_data_bytes,
                    getDataTypeName(dataType!!, requireContext())
                )
                listOf(it.first { it.type == dataType })
            } else {
                binding.title.text = getString(R.string.add_user_data_bytes)
                it.filter { !it.exists() }
            }

            setSpinnerDataType(list)

            binding.apply {
                val value =
                    DataUnitBytes(list[spinnerDataType.selectedItemPosition].bytes).getValue()

                editValue.setText(value.value.toString(), TextView.BufferType.EDITABLE)
                spinnerDataUnit.setSelection(value.dataUnit.ordinal)
            }

            binding.buttonAction.setOnClickListener {
                viewModel.updateUserDataBytes(
                    list[binding.spinnerDataType.selectedItemPosition].apply {
                        bytes =  DataUnitBytes.DataValue(
                            binding.editValue.text?.toString()?.toDouble() ?: 0.0,
                            DataUnitBytes.DataUnit.values()[binding.spinnerDataUnit.selectedItemPosition]
                        ).toBytes()

                        if (initialBytes < bytes){
                            initialBytes = bytes
                        }
                    }
                )
                dismiss()
            }
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