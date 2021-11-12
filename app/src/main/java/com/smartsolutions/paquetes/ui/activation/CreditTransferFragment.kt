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
import com.smartsolutions.paquetes.databinding.FragmentCreditTransferBinding
import com.smartsolutions.paquetes.ui.settings.AbstractSettingsFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class CreditTransferFragment : AbstractSettingsFragment() {

    private var _binding: FragmentCreditTransferBinding? = null
    private val binding get() = _binding!!

    private val viewModel: Purchase2ViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentCreditTransferBinding.inflate(layoutInflater, container, false)

        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.loadLicence()

        viewModel.setWaitingPurchase()

        binding.apply {

            btnTransfer.setOnClickListener {
                if (editKeyTransfer.text == null || editKeyTransfer.text.toString().isEmpty() || editKeyTransfer.text.toString().isBlank()){
                    Toast.makeText(requireContext(), getString(R.string.must_have_input_key), Toast.LENGTH_SHORT).show()
                }else if (editKeyTransfer.text!!.length < 4){
                    Toast.makeText(requireContext(), getString(R.string.key_to_short), Toast.LENGTH_SHORT).show()
                }else {
                    viewModel.transferCreditByUSSD(editKeyTransfer.text.toString())
                }
            }

            btnCancel.setOnClickListener {
                complete()
            }

            btnBack.setOnClickListener {
                parentFragmentManager.commit {
                    replace(R.id.container, PurchaseModeFragment.newInstance())
                }
            }
        }

        viewModel.onConfirmPurchase().observe(viewLifecycleOwner) {
            if (it.isSuccess) {
                parentFragmentManager.commit {
                    replace(R.id.container, PurchaseSuccessfulFragment())
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        showWaiting(true)
    }

    private fun showWaiting(show: Boolean){
        if (show){
            binding.constraintWaitingPurchase.visibility = View.VISIBLE
        }else {
            binding.constraintWaitingPurchase.visibility = View.GONE
        }
    }


    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }

    companion object {
        fun newInstance() = CreditTransferFragment()
    }
}