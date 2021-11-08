package com.smartsolutions.paquetes.ui.activation

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.commit
import com.smartsolutions.paquetes.R
import com.smartsolutions.paquetes.databinding.FragmentTermsAndConditionsBinding
import com.smartsolutions.paquetes.ui.settings.AbstractSettingsFragment


class TermsAndConditionsFragment : AbstractSettingsFragment() {

    private var _binding: FragmentTermsAndConditionsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTermsAndConditionsBinding.inflate(layoutInflater, container, false)
        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.apply {

            btnContinue.setOnClickListener {
                if (acceptTermsConditions.isChecked) {
                    parentFragmentManager.commit {
                        replace(R.id.container, PurchaseModeFragment.newInstance())
                    }
                }else {
                    Toast.makeText(requireContext(), getString(R.string.must_have_agree), Toast.LENGTH_SHORT).show()
                }
            }

            btnCancel.setOnClickListener {
                complete()
            }

        }

    }


    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }

    companion object {
        fun newInstance() = TermsAndConditionsFragment()
    }
}