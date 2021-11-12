package com.smartsolutions.paquetes.ui.activation

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.commit
import androidx.fragment.app.viewModels
import com.smartsolutions.paquetes.R
import com.smartsolutions.paquetes.databinding.FragmentBankaryTransferBinding
import com.smartsolutions.paquetes.ui.settings.AbstractSettingsFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class BankingTransferFragment : AbstractSettingsFragment() {

    private var _binding: FragmentBankaryTransferBinding? = null
    private val binding get() = _binding!!

    private val viewModel: Purchase2ViewModel by viewModels ()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
       _binding = FragmentBankaryTransferBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.loadLicence()

        viewModel.setWaitingPurchase()

        binding.apply {
            headerDescription.text = getString(R.string.transfermovil_instruccions, viewModel.getPrice())
            cardNumber.text = getString(R.string.debit_card_number, viewModel.getDebitCardNumber())

            btnCopyToClipboard.setOnClickListener {
                viewModel.copyDebitCardToClipboard()
            }

            btnOpenTransfermovil.setOnClickListener {
                viewModel.openTransfermovil(root)
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
        fun newInstance() = BankingTransferFragment()
    }
}