package com.smartsolutions.paquetes.ui.settings

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.AppCompatButton
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.viewModels
import com.google.android.material.button.MaterialButton
import com.google.android.material.checkbox.MaterialCheckBox
import com.smartsolutions.paquetes.R
import com.smartsolutions.paquetes.exceptions.MissingPermissionException
import com.smartsolutions.paquetes.serverApis.models.Result
import com.smartsolutions.paquetes.ui.permissions.PermissionsFragment
import com.smartsolutions.paquetes.ui.permissions.SinglePermissionFragment
import dagger.hilt.android.AndroidEntryPoint
import java.net.ConnectException

@AndroidEntryPoint
class PurchasedFragment : AbstractSettingsFragment(R.layout.fragment_purchased) {

    private val viewModel by viewModels<PurchasedViewModel>()

    override fun isRequired() = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<MaterialButton>(R.id.purchase)
            .setOnClickListener {

                if (view.findViewById<MaterialCheckBox>(R.id.accept_terms_conditions).isChecked) {
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

        beginActivation()
        registerUssdResultObserver(view)
    }

    private fun registerUssdResultObserver(view: View) {
        viewModel.ussdTranferenceResult.observe(viewLifecycleOwner) {
            if (it.isSuccess) {

                listener?.invoke(null)

            } else {

                //TODO: Pedir permiso si lo tengo denegado
                Toast.makeText(
                    requireContext(),
                    (it as Result.Failure).throwable.message,
                    Toast.LENGTH_SHORT).show()
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
            .create()

        val progressBar = view.findViewById<ProgressBar>(R.id.progressBar)
        val msgStatus = view.findViewById<TextView>(R.id.msg_status)

        val btnClose = view.findViewById<Button>(R.id.btn_close)
        btnClose.setOnClickListener {
            dialog.hide()
            //TODO: Temp
            listener?.invoke(null)
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

        view.findViewById<AppCompatButton>(R.id.btn_transfermovil)
            .setOnClickListener {
                dialog.hide()
                showTransfermovilLayout()
            }

        view.findViewById<AppCompatButton>(R.id.btn_ussd)
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
}