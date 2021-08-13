package com.smartsolutions.paquetes.ui.applications

import androidx.lifecycle.ViewModel
import com.smartsolutions.paquetes.repositories.contracts.IAppRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.filter
import javax.inject.Inject

@HiltViewModel
class PlaceHolderViewModel @Inject constructor(
    private val appRepository: IAppRepository
) : ViewModel() {


}