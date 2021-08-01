package com.smartsolutions.paquetes.ui.activation

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.smartsolutions.paquetes.R
import com.smartsolutions.paquetes.serverApis.models.Result
import com.smartsolutions.paquetes.ui.settings.AbstractSettingsFragment
import dagger.hilt.android.AndroidEntryPoint
import java.net.ConnectException

@AndroidEntryPoint
class PurchasedFragment : Fragment(R.layout.fragment_purchased) {

    private val viewModel by viewModels<PurchasedViewModel>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<Button>(R.id.purchase)
            .setOnClickListener {

                if (view.findViewById<CheckBox>(R.id.accept_terms_conditions).isChecked) {
                    showPurchaseOptions()
                } else {
                    Toast.makeText(
                        context,
                        "Debe aceptar los términos y condiciones.",
                        Toast.LENGTH_SHORT).show()
                }

            }

        view.findViewById<Button>(R.id.btn_transfermovil_back)
            .setOnClickListener {
                layoutBack()
            }

        view.findViewById<Button>(R.id.btn_ussd_back)
            .setOnClickListener {
                layoutBack()
            }

        view.findViewById<Button>(R.id.btn_copy_to_clipboard)
            .setOnClickListener {
                viewModel.copyDebitCardToClipboard()
            }

        view.findViewById<Button>(R.id.btn_open_transfermovil)
            .setOnClickListener {
                viewModel.openTransfermovil()
            }

        view.findViewById<Button>(R.id.btn_ussd_tranfer)
            .setOnClickListener(::ussdTranfer)

        view.findViewById<Button>(R.id.btn_continue)
            .setOnClickListener {
                complete()
            }
        view.findViewById<Button>(R.id.btn_transfermovil_continue)
            .setOnClickListener {
                complete()
            }

        beginActivation()
        registerUssdResultObserver()
    }

    private fun registerUssdResultObserver() {
        viewModel.ussdTranferenceResult.observe(viewLifecycleOwner) {
            if (it.isSuccess) {

                complete()

            } else {
                //viewModel.handleUssdResultFailure(it as Result.Failure, childFragmentManager)
                it.getThrowableOrNull()?.let { throwable ->
                    throw throwable
                }
            }
        }
    }

    private fun beginActivation() {
        val view = layoutInflater.inflate(
            R.layout.begin_activation_layout,
            null,
            false
        )

        val dialog = AlertDialog.Builder(requireContext())
            .setView(view)
            .setTitle(R.string.begin_activation_title)
            .setCancelable(false)
            .create()

        val progressBar = view.findViewById<ProgressBar>(R.id.progressBar)
        val msgStatus = view.findViewById<TextView>(R.id.msg_status)

        val btnClose = view.findViewById<Button>(R.id.btn_close)
        btnClose.setOnClickListener {
            dialog.hide()
            complete()
        }

        val btnRetry = view.findViewById<Button>(R.id.btn_retry)
        btnRetry.setOnClickListener {
            progressBar.visibility = View.VISIBLE
            msgStatus.text = getString(R.string.begin_activation_summary)
            btnClose.visibility = View.INVISIBLE
            btnRetry.visibility = View.INVISIBLE

            viewModel.initDeviceAppAndActivation()
        }

        viewModel.beginActivationResult.observe(viewLifecycleOwner) {
            progressBar.visibility = View.INVISIBLE

            if (it.isSuccess) {
                dialog.hide()
                this.view?.findViewById<TextView>(R.id.transfermovil_instructions)
                    ?.text = getString(R.string.transfermovil_instruccions, viewModel.getPrice())

                this.view?.findViewById<TextView>(R.id.card_number)
                    ?.text = getString(R.string.debit_card_number, viewModel.getDebitCardNumber())
            } else {
                btnClose.visibility = View.VISIBLE
                btnRetry.visibility = View.VISIBLE

                when (val throwable = (it as Result.Failure).throwable) {
                    is ConnectException -> {
                        msgStatus.text = "Falló la conexión con el servidor"
                    }
                    else -> {
                        msgStatus.text = throwable.message
                    }
                }
            }
        }

        dialog.show()
    }

    private fun ussdTranfer(view: View) {
        val key = this.view?.findViewById<EditText>(R.id.tranfer_key)
            ?.text?.toString() ?: ""

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
        view?.findViewById<ConstraintLayout>(R.id.first_layout)
            ?.visibility = View.GONE

        view?.findViewById<ConstraintLayout>(R.id.transfermovil_layout)
            ?.visibility = View.VISIBLE
    }

    private fun showUssdLayout() {
        view?.findViewById<ConstraintLayout>(R.id.first_layout)
            ?.visibility = View.GONE

        view?.findViewById<ConstraintLayout>(R.id.ussd_layout)
            ?.visibility = View.VISIBLE
    }

    private fun layoutBack() {
        view?.findViewById<ConstraintLayout>(R.id.first_layout)
            ?.visibility = View.VISIBLE

        view?.findViewById<ConstraintLayout>(R.id.transfermovil_layout)
            ?.visibility = View.GONE

        view?.findViewById<ConstraintLayout>(R.id.ussd_layout)
            ?.visibility = View.GONE
    }

    fun complete() {
        activity?.let {
            if (it is OnCompletedListener)
                it.onCompleted()
        }
    }
}