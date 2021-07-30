package com.smartsolutions.paquetes.ui.settings

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.os.Build
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.*
import com.smartsolutions.paquetes.DatwallKernel
import com.smartsolutions.paquetes.exceptions.MissingPermissionException
import com.smartsolutions.paquetes.exceptions.USSDRequestException
import com.smartsolutions.paquetes.managers.contracts.IActivationManager
import com.smartsolutions.paquetes.managers.contracts.IPermissionsManager
import com.smartsolutions.paquetes.serverApis.models.DeviceApp
import com.smartsolutions.paquetes.serverApis.models.Result
import com.smartsolutions.paquetes.ui.permissions.SinglePermissionFragment
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PurchasedViewModel @Inject constructor(
    application: Application,
    private val activationManager: IActivationManager,
    private val permissionManager: IPermissionsManager
) : AndroidViewModel(application) {

    private var cardCopied = false

    private var deviceApp: DeviceApp? = null

    /**
     * Esta propiedad se debe llamar antes de usar cualquier método de este viewmodel.
     *
     * Obtiene el deviceApp del servidor e inicia el proceso de activación.
     * */
    private val _beginActivationResult = MutableLiveData<Result<Unit>>()
    val beginActivationResult: LiveData<Result<Unit>>
        get() {
            initDeviceAppAndActivation()
            return _beginActivationResult
        }

    /**
     * Resultado de la tranferencia por código ussd.
     * */
    val ussdTranferenceResult: LiveData<Result<Unit>> = MutableLiveData()

    fun initDeviceAppAndActivation() {
        viewModelScope.launch(Dispatchers.IO) {
            val deviceAppResult = activationManager.getDeviceApp()

            if (deviceAppResult.isFailure)
                _beginActivationResult.postValue(Result.Failure((deviceAppResult as Result.Failure).throwable))
            else {
                deviceApp = deviceAppResult.getOrNull()

                deviceApp?.let {
                    _beginActivationResult.postValue(
                        activationManager.beginActivation(it)
                    )
                }
            }
        }
    }

    /**
     * Copia el número de tarjeta en el portapapeles.
     * */
    fun copyDebitCardToClipboard() {
        checkDeviceApp()

        deviceApp?.let {

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
        checkDeviceApp()

        deviceApp?.let {
            return it.androidApp.debitCard
        }
        return null
    }

    fun openTransfermovil() {
        val transfermovilPackageName = "cu.etecsa.cubacel.tr.tm"

        val packageManager = getApplication<Application>().packageManager

        val intent = packageManager.getLaunchIntentForPackage(transfermovilPackageName)

        if (intent != null) {
            if (!cardCopied)
                copyDebitCardToClipboard()

            getApplication<Application>().startActivity(intent)
        } else {
            Toast.makeText(
                getApplication(),
                "No tiene Transfermovil instalado.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    fun transferCreditByUSSD(key: String) {
        checkDeviceApp()

        deviceApp?.let {
            viewModelScope.launch(Dispatchers.IO) {
                (ussdTranferenceResult as MutableLiveData).postValue(
                    activationManager.transferCreditByUSSD(key, it)
                )
            }
        }
    }

    fun getPrice(): String {
        checkDeviceApp()

        deviceApp?.let {
            return "${it.androidApp.price}$"
        }
        return "30$"
    }

    fun handleUssdResultFailure(failure: Result.Failure<Unit>, fragmentManager: FragmentManager) {
        if (failure.throwable is USSDRequestException && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

            permissionManager.findPermission(IPermissionsManager.CALL_CODE)?.let {
                if (!it.checkPermission(it, getApplication())) {
                    SinglePermissionFragment.newInstance(IPermissionsManager.CALL_CODE)
                        .show(fragmentManager, null)
                }
            }
        }
        
        Toast.makeText(
            getApplication(),
            failure.throwable.message,
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun checkDeviceApp() {
        if (deviceApp == null)
            throw Exception("DeviceApp is null. First call property beginActivationResult " +
                    "or method initDeviceAppAndActivation()")
    }
}