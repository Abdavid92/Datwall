package com.smartsolutions.paquetes.ui.activation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartsolutions.paquetes.DatwallKernel
import com.smartsolutions.paquetes.ui.NextViewModelDelegate
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class ActivationViewModel @Inject constructor(
    kernel: DatwallKernel
) : ViewModel() {

    private val nextViewModelDelegate = NextViewModelDelegate(kernel, viewModelScope)

    fun nextActivity() = nextViewModelDelegate.nextActivity()

    fun next() = nextViewModelDelegate.next()
}