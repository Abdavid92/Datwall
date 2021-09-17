package com.smartsolutions.paquetes.ui

import android.os.Bundle
import android.os.Message
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.smartsolutions.paquetes.databinding.FragmentBottomSheetDialogBasicBinding


private const val TYPE = "dialog_type"
private const val TITLE = "title"
private const val MESSAGE = "message"

class BottomSheetDialogBasic : BottomSheetDialogFragment() {

    private lateinit var binding: FragmentBottomSheetDialogBasicBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentBottomSheetDialogBasicBinding.inflate(inflater, container, false)
        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


    }


    companion object {

        fun newInstance(dialogType: DialogType, title: String? = null, message: String? = null): BottomSheetDialogBasic =
            BottomSheetDialogBasic().apply {
                arguments = Bundle().apply {
                    putString(TYPE, dialogType.name)
                    putString(TITLE, title)
                    putString(MESSAGE, message)
                }
            }

    }


    enum class DialogType {
        BASIC_INFORMATION,
        SYNCHRONIZATION_FAILED,
        SYNCHRONIZATION_FAILED_NOT_DEFAULT_SIM
    }
}