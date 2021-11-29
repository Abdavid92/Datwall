package com.smartsolutions.paquetes.ui.resume

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.viewModels
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.smartsolutions.paquetes.R
import com.smartsolutions.paquetes.databinding.FragmentEditAddUserDataBytesBinding
import com.smartsolutions.paquetes.helpers.DateCalendarUtils
import com.smartsolutions.paquetes.helpers.getDataTypeName
import com.smartsolutions.paquetes.managers.models.DataUnitBytes
import com.smartsolutions.paquetes.repositories.models.DataBytes
import com.smartsolutions.paquetes.repositories.models.UserDataBytes
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.*

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
    private var isLaunched = false
    private var dataType: DataBytes.DataType? = null

    private var expireDate: Long? = null


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

        viewModel.getUserDataBytes(simID!!).observe(viewLifecycleOwner) {

            if (isLaunched){
                return@observe
            }

            isLaunched = true

            val list = if (isEdit == true) {
                binding.title.text = getString(
                    R.string.edit_user_data_bytes,
                    getDataTypeName(dataType!!, requireContext())
                )
                binding.spinnerDataType.visibility = View.GONE
                listOf(it.first { it.type == dataType })
            } else {
                binding.title.text = getString(R.string.add_user_data_bytes)
                it.filter { !it.exists() || it.isExpired() }
            }

            if(list.isEmpty()){
                dismiss()
                Toast.makeText(requireContext(), "No hay más megas que agregar", Toast.LENGTH_SHORT).show()
                return@observe
            }

            setSpinnerDataType(list)

            binding.apply {

                spinnerDataType.onItemSelectedListener =
                    object : AdapterView.OnItemSelectedListener {
                        override fun onItemSelected(
                            parent: AdapterView<*>?,
                            view: View?,
                            position: Int,
                            id: Long
                        ) {
                            fillValuesUserDataBytes(list[position])
                        }

                        override fun onNothingSelected(parent: AdapterView<*>?) {}
                    }

                spinnerDataType.setSelection(0)

                fillValuesUserDataBytes(list[0])

                btnDate.setOnClickListener {
                    val calendar = Calendar.getInstance()
                    DatePickerDialog(
                        requireContext(),
                        { _, year, month, dayOfMonth ->
                            val cal = Calendar.getInstance()
                            cal.set(Calendar.YEAR, year)
                            cal.set(Calendar.MONTH, month)
                            cal.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                            expireDate = DateCalendarUtils.getLastHour(Date(cal.timeInMillis)).time
                            binding.textDate.text = getExpiredTime(expireDate!!)
                            cal.clear()
                        },
                        calendar.get(Calendar.YEAR),
                        calendar.get(Calendar.MONTH),
                        calendar.get(Calendar.DAY_OF_MONTH)
                    ).show()
                    calendar.clear()
                }

                buttonAction.setOnClickListener {
                    viewModel.updateUserDataBytes(
                        list[binding.spinnerDataType.selectedItemPosition].apply {

                            var restBytes = binding.editValueRest.text?.toString()

                            if (restBytes.isNullOrEmpty())
                                restBytes = "0"

                            bytes = DataUnitBytes.DataValue(
                                restBytes.toDouble(),
                                DataUnitBytes.DataUnit.values()[binding.spinnerDataUnitRest.selectedItemPosition]
                            ).toBytes()

                            var originBytes = binding.editValueInitial.text?.toString()

                            if (originBytes.isNullOrEmpty())
                                originBytes = "0"

                            initialBytes = DataUnitBytes.DataValue(
                                originBytes.toDouble(),
                                DataUnitBytes.DataUnit.values()[binding.spinnerDataUnitInitial.selectedItemPosition]
                            ).toBytes()

                            expiredTime = expireDate ?: expiredTime

                            if (initialBytes < bytes) {
                                initialBytes = bytes
                            }
                        }
                    )
                    dismiss()
                }
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

    private fun fillValuesUserDataBytes(userDataBytes: UserDataBytes) {
        binding.apply {
            if (isEdit == true) {
                val initial = DataUnitBytes(userDataBytes.initialBytes).getValue()
                val rest = DataUnitBytes(userDataBytes.bytes).getValue()

                editValueInitial.setText(initial.value.toString(), TextView.BufferType.EDITABLE)
                spinnerDataUnitInitial.setSelection(initial.dataUnit.ordinal)

                editValueRest.setText(rest.value.toString(), TextView.BufferType.EDITABLE)
                spinnerDataUnitRest.setSelection(rest.dataUnit.ordinal)

                textDate.text = getExpiredTime(userDataBytes.expiredTime)
            }
        }
    }


    private fun getExpiredTime(time: Long): String {
        return if (time > 0){
            SimpleDateFormat("dd/MM/yyyy hh:mm aa", Locale.getDefault()).format(Date(time))
        }else {
            "Desconocido"
        }
    }


    companion object {
        fun newInstance(
            simID: String,
            isEdit: Boolean,
            dataType: DataBytes.DataType?
        ): EditAddUserDataBytesFragment =
            EditAddUserDataBytesFragment().apply {
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