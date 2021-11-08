package com.smartsolutions.paquetes.ui.activation

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.view.View
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.*
import com.google.android.material.snackbar.Snackbar
import com.smartsolutions.paquetes.DatwallApplication
import com.smartsolutions.paquetes.exceptions.USSDRequestException
import com.smartsolutions.paquetes.helpers.USSDHelper
import com.smartsolutions.paquetes.managers.contracts.IActivationManager
import com.smartsolutions.paquetes.managers.contracts.IPermissionsManager
import com.smartsolutions.paquetes.serverApis.models.License
import com.smartsolutions.paquetes.serverApis.models.Result
import com.smartsolutions.paquetes.ui.permissions.SinglePermissionFragment
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PurchaseViewModel @Inject constructor(
    application: Application,
    private val activationManager: IActivationManager,
    private val permissionManager: IPermissionsManager
) : AndroidViewModel(application) {

    private var cardCopied = false

    private var _license: License? = null

    private val licenseLiveData = MutableLiveData<License?>()
    val license: LiveData<License?>
        get() {
            if (licenseLiveData.value == null) {
                viewModelScope.launch {

                    _license = activationManager.getLocalLicense()
                    licenseLiveData.postValue(_license)
                }
            }

            return licenseLiveData
        }

    /**
     * Esta propiedad se debe llamar antes de usar cualquier método de este viewmodel.
     *
     * Obtiene el deviceApp del servidor e inicia el proceso de activación.
     * */
    private val _beginActivationResult = MutableLiveData<Result<Unit>>()
    val beginActivationResult: LiveData<Result<Unit>>
        get() {
            if (_beginActivationResult.value == null) {
                initDeviceAppAndActivation()
            }
            return _beginActivationResult
        }

    /**
     * Resultado de la tranferencia por código ussd.
     * */
    val ussdTransferenceResult: LiveData<Result<Unit>> = MutableLiveData()

    fun initDeviceAppAndActivation() {
        viewModelScope.launch {

            val licenseResult = activationManager.getLicense()

            if (licenseResult.isFailure)
                _beginActivationResult.postValue(Result.Failure((licenseResult as Result.Failure).throwable))
            else {
                _license = licenseResult.getOrNull()
            }
        }
    }

    /**
     * Copia el número de tarjeta en el portapapeles.
     * */
    fun copyDebitCardToClipboard() {
        checkLicense()

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

    fun getDebitCardNumber(): String? {
        checkLicense()

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
                "No tiene Transfermovil instalado",
                Snackbar.LENGTH_LONG
            ).setAction("Instalar") {

                val transferIntent = Intent(Intent.ACTION_VIEW)
                    .setData(Uri.parse("https://apklis.cu/application/$transfermovilPackageName"))

                getApplication<DatwallApplication>().startActivity(transferIntent)
            }.show()
        }
    }

    fun transferCreditByUSSD(key: String) {
        checkLicense()

        _license?.let {
            viewModelScope.launch {
                (ussdTransferenceResult as MutableLiveData).postValue(
                    activationManager.transferCreditByUSSD(key, it)
                )
            }
        }
    }

    fun getPrice(): String {
        checkLicense()

        _license?.let {
            return "${it.androidApp.price}$"
        }
        return "30$"
    }

    fun handleUssdResultFailure(failure: Result.Failure<Unit>, fragmentManager: FragmentManager) {
        if (failure.throwable is USSDRequestException &&
            failure.throwable.errorCode == USSDHelper.DENIED_CALL_PERMISSION &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

            permissionManager.findPermission(IPermissionsManager.CALL_CODE)?.let {
                if (!it.checkPermission(it, getApplication())) {
                    SinglePermissionFragment.newInstance(IPermissionsManager.CALL_CODE)
                        .show(fragmentManager, null)
                }
            }
        } else {
            Toast.makeText(
                getApplication(),
                failure.throwable.message,
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun checkLicense() {
        if (_license == null)
            throw Exception("License is null. First call " +
                    "method initDeviceAppAndActivation()")
    }
}