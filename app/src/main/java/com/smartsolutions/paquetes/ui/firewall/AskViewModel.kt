package com.smartsolutions.paquetes.ui.firewall

import androidx.lifecycle.ViewModel
import com.smartsolutions.paquetes.repositories.contracts.IAppRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class AskViewModel @Inject constructor(
    private val appRepository: IAppRepository
) : ViewModel() {
}