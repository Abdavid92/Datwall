package com.smartsolutions.paquetes.ui.settings

import android.content.ClipData
import android.content.ClipboardManager
import android.os.Bundle
import android.text.Editable
import android.text.Html
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.AppCompatButton
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.text.HtmlCompat
import androidx.fragment.app.viewModels
import com.google.android.material.button.MaterialButton
import com.google.android.material.checkbox.MaterialCheckBox
import com.google.android.material.textview.MaterialTextView
import com.smartsolutions.paquetes.R
import dagger.hilt.android.AndroidEntryPoint
import org.xml.sax.XMLReader

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
    }

    private fun beginActivation() {

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