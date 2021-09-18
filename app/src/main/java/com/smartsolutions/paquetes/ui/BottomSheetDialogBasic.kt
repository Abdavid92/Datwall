package com.smartsolutions.paquetes.ui

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Message
import android.provider.Settings
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.smartsolutions.paquetes.R
import com.smartsolutions.paquetes.databinding.FragmentBottomSheetDialogBasicBinding


private const val TYPE = "dialog_type"
private const val TITLE = "title"
private const val MESSAGE = "message"

class BottomSheetDialogBasic : BottomSheetDialogFragment() {

    private lateinit var binding: FragmentBottomSheetDialogBasicBinding
    private var title: String? = null
    private var message: String? = null
    private lateinit var type: DialogType


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments?.let {
            type = DialogType.valueOf(it.getString(TYPE) ?: throw IllegalArgumentException())
            title = it.getString(TITLE)
            message = it.getString(MESSAGE)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentBottomSheetDialogBasicBinding.inflate(inflater, container, false)
        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.buttonDone.setOnClickListener {
            dismiss()
        }

        when (type){
            DialogType.BASIC_INFORMATION -> {
                if (title == null || message == null){
                    dismiss()
                }
                binding.textTitle.text = title
                binding.textMessage.text = message

            }
            DialogType.SYNCHRONIZATION_FAILED -> {
                binding.textTitle.text = getString(R.string.synchronization_failed)
                binding.textMessage.text = message ?: getString(R.string.synchronization_failed_message)
            }
            DialogType.SYNCHRONIZATION_FAILED_NOT_DEFAULT_SIM -> {
                binding.textTitle.text = getString(R.string.synchronization_failed_not_default_sim)
                binding.textMessage.text = getString(R.string.synchronization_failed_not_default_sim_message)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    binding.buttonCancel.apply {
                        visibility = View.VISIBLE
                        text = "Abrir Ajustes"
                        setOnClickListener {
                            val intent = Intent(Settings.ACTION_WIRELESS_SETTINGS)
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            if (intent.resolveActivity(requireContext().packageManager) != null) {
                                startActivity(intent)
                            } else {
                                Toast.makeText(
                                    requireContext(),
                                    "No se pudo abrir",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                            this@BottomSheetDialogBasic.dismiss()
                        }
                    }
                }
            }
        }

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