package com.smartsolutions.paquetes.ui.setup

import androidx.lifecycle.ViewModel
import com.smartsolutions.paquetes.managers.contracts.ISimManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SetupViewModel @Inject constructor(
    private val simManager: ISimManager
) : ViewModel() {

}