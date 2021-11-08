package com.smartsolutions.paquetes.ui.activation

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.commit
import androidx.fragment.app.viewModels
import com.smartsolutions.paquetes.R
import com.smartsolutions.paquetes.databinding.FragmentPurchaseModeBinding
import com.smartsolutions.paquetes.ui.settings.AbstractSettingsFragment

class PurchaseModeFragment : AbstractSettingsFragment() {

    private var _binding: FragmentPurchaseModeBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPurchaseModeBinding.inflate(layoutInflater, container, false)
        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.apply {

            btnCancel.setOnClickListener {
                complete()
            }

            btnContinue.setOnClickListener {
                when (groupMode.checkedRadioButtonId){
                    R.id.banking_mode -> {
                       replaceFragment(BankingTransferFragment.newInstance())
                    }
                    R.id.credit_mode -> {
                        replaceFragment(CreditTransferFragment.newInstance())
                    }
                    else -> {
                        Toast.makeText(requireContext(), getString(R.string.must_have_select), Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

    }


    private fun replaceFragment(fragment: Fragment){
        parentFragmentManager.commit {
            replace(R.id.container, fragment)
        }
    }


    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }

    companion object {
        fun newInstance() = PurchaseModeFragment()
    }
}