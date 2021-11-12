package com.smartsolutions.paquetes.ui.activation

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.net.Uri
import android.view.View
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.lifecycle.*
import com.google.android.material.snackbar.Snackbar
import com.smartsolutions.paquetes.DatwallApplication
import com.smartsolutions.paquetes.managers.ActivationManager
import com.smartsolutions.paquetes.managers.contracts.IActivationManager
import com.smartsolutions.paquetes.serverApis.models.License
import com.smartsolutions.paquetes.serverApis.models.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@HiltViewModel
class Purchase2ViewModel @Inject constructor(
    application: Application,
    private val activationManager: IActivationManager
): AndroidViewModel(application) {

    private var _license: License? = null
    private var cardCopied = false

    fun onConfirmPurchase() = activationManager.onConfirmPurchase

    fun loadLicence() {
        runBlocking {
            _license = activationManager.getLocalLicense()
        }
    }

    fun getPrice(): String {
        _license?.let {
            return "${it.androidApp.price}.00 MN"
        }
        return "30.00 MN"
    }

    fun getDebitCardNumber(): String? {
        _license?.let {
            return it.androidApp.debitCard
        }
        return null
    }

    fun openTransfermovil(view: View) {
        val transfermovilPackageName = "cu.etecsa.cubacel.tr.tm"

        val packageManager = getApplication<Application>().packageManager

        val intent = packageManager.getLaunchIntentForPackage(transfermovilPackageName)

        if (intent != null) {
            if (!cardCopied)
                copyDebitCardToClipboard()

            getApplication<Application>().startActivity(intent)
        } else {
            Snackbar.make(
                view,
                "No tiene Transfermóvil instalado",
                Snackbar.LENGTH_LONG
            ).setAction("Instalar") {

                val transferIntent = Intent(Intent.ACTION_VIEW)
                    .setData(Uri.parse("https://apklis.cu/application/$transfermovilPackageName"))

                getApplication<DatwallApplication>().startActivity(transferIntent)
            }.show()
        }
    }

    fun copyDebitCardToClipboard() {

        _license?.let {

            val clipboardManager = ContextCompat.getSystemService(
                getApplication(),
                ClipboardManager::class.java
            ) ?: throw NullPointerException()

            val clipData = ClipData.newPlainText(
                "Tarjeta de débito de Mis Datos",
                it.androidApp.debitCard
            )

            clipboardManager.setPrimaryClip(clipData)

            cardCopied = true

            Toast.makeText(
                getApplication(),
                "Número de tarjeta copiado al portapapeles.",
                Toast.LENGTH_SHORT).show()
        }
    }

    fun transferCreditByUSSD(key: String) {

        _license?.let {
            viewModelScope.launch {
               activationManager.transferCreditByUSSD(key, it)
            }
        }
    }

    fun setWaitingPurchase(){
        viewModelScope.launch {
            activationManager.setWaitingPurchase(true)
        }
    }

}