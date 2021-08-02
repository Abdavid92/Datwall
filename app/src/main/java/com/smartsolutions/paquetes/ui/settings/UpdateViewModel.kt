package com.smartsolutions.paquetes.ui.settings

import androidx.lifecycle.ViewModel
import com.smartsolutions.paquetes.managers.contracts.IUpdateManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class UpdateViewModel @Inject constructor(
    private val updateManager: IUpdateManager
) : ViewModel() {
}