package com.smartsolutions.paquetes.ui.activation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.viewModels
import com.smartsolutions.paquetes.R
import com.smartsolutions.paquetes.databinding.FragmentPurchaseBinding
import com.smartsolutions.paquetes.serverApis.models.Result
import com.smartsolutions.paquetes.ui.settings.AbstractSettingsFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class PurchaseFragment : AbstractSettingsFragment() {

    private val viewModel by viewModels<PurchaseViewModel>()

    private var _binding: FragmentPurchaseBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentPurchaseBinding.inflate(
            inflater,
            container,
            false
        )

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.apply {

            purchase.setOnClickListener {

                if (acceptTermsConditions.isChecked) {
                    showPurchaseOptions()
                } else {
                    Toast.makeText(
                        context,
                        "Debe aceptar los términos y condiciones.",
                        Toast.LENGTH_SHORT).show()
                }
            }

            btnTransfermovilBack.setOnClickListener(::layoutBack)
            btnUssdBack.setOnClickListener(::layoutBack)

            btnCopyToClipboard.setOnClickListener {
                viewModel.copyDebitCardToClipboard()
            }

            btnOpenTransfermovil.setOnClickListener {
                viewModel.openTransfermovil(it)
            }

            btnUssdTranfer.setOnClickListener(::ussdTransfer)

            btnCancel.setOnClickListener {
                complete()
            }

            btnTransfermovilContinue.setOnClickListener {
                complete()
            }
        }

        registerUssdResultObserver()
        initLicense()
    }

    private fun initLicense() {

        viewModel.license.observe(viewLifecycleOwner) { license ->

            if (license != null) {

                binding.transfermovilInstructions
                    .text = getString(
                    R.string.transfermovil_instruccions,
                    "${license.androidApp.price}$")

                binding.cardNumber
                    .text = getString(
                        R.string.debit_card_number,
                        license.androidApp.debitCard)
            }
        }
    }

    private fun registerUssdResultObserver() {
        viewModel.ussdTransferenceResult.observe(viewLifecycleOwner) {
            if (it.isSuccess) {

                complete()

            } else {
                viewModel.handleUssdResultFailure(it as Result.Failure, childFragmentManager)
            }
        }
    }

    private fun ussdTransfer(view: View) {
        val key = binding.tranferKey.text?.toString() ?: ""

        if (key.isNotEmpty() && key.length == 4) {
            viewModel.transferCreditByUSSD(key)
        } else {
            Toast.makeText(
                context,
                "No ha insertado la clave correctamente.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun showPurchaseOptions() {

        val view = layoutInflater.inflate(
            R.layout.purchase_options_layout,
            null,
            false
        )

        val dialog = AlertDialog.Builder(requireContext())
            .setView(view)
            .setTitle("Elija el método de pago")
            .setNegativeButton("Cerrar", null)
            .create()

        dialog.show()

        view.findViewById<Button>(R.id.btn_transfermovil)
            .setOnClickListener {
                dialog.hide()
                showTransfermovilLayout()
            }

        view.findViewById<Button>(R.id.btn_ussd)
            .setOnClickListener {
                dialog.hide()
                showUssdLayout()
            }
    }

    private fun showTransfermovilLayout() {

        binding.firstLayout.visibility = View.GONE
        binding.transfermovilLayout.visibility = View.VISIBLE
    }

    private fun showUssdLayout() {

        binding.firstLayout.visibility = View.GONE
        binding.ussdLayout.visibility = View.VISIBLE
    }

    private fun layoutBack(view: View) {

        binding.firstLayout.visibility = View.VISIBLE
        binding.transfermovilLayout.visibility = View.GONE
        binding.ussdLayout.visibility = View.GONE
    }
}