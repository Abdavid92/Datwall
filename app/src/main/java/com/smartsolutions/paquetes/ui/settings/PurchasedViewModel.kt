package com.smartsolutions.paquetes.ui.settings

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.lifecycle.*
import com.smartsolutions.paquetes.managers.contracts.IActivationManager
import com.smartsolutions.paquetes.serverApis.models.DeviceApp
import com.smartsolutions.paquetes.serverApis.models.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class PurchasedViewModel @Inject constructor(
    application: Application,
    private val activationManager: IActivationManager
) : AndroidViewModel(application) {

    private var cardCopied = false

    private var deviceApp: DeviceApp? = null

    /**
     * Esta propiedad se deba llamar antes de usar cualquier método de este viewmodel.
     *
     * Obtiene el deviceApp del servidor e inicia el proceso de activación.
     * */
    private val _beginActivationResult = MutableLiveData<Result<Unit>>()
    val beginActivationResult: LiveData<Result<Unit>>
        get() {
            viewModelScope.launch {
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
            return _beginActivationResult
        }

    /**
     * Copia el número de tarjeta en el portapapeles.
     * */
    fun copyDebitCardToClipboard() {
        viewModelScope.launch {
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

                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        getApplication(),
                        "Número de tarjeta copiado al portapapeles.",
                        Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    fun getDebitCardNumber(): String? {
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

    }
}