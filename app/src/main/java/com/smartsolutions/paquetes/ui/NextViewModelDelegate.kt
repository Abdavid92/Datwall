package com.smartsolutions.paquetes.ui

import android.app.Activity
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartsolutions.paquetes.DatwallKernel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.lastOrNull
import kotlinx.coroutines.launch

class NextViewModelDelegate(
    private val kernel: DatwallKernel,
    private val scope: CoroutineScope
) {

    private val _nextActivity = MutableLiveData<Class<out Activity>>()

    private var kernelRunning = false

    fun nextActivity(): LiveData<Class<out Activity>> {
        return _nextActivity
    }

    fun next() {
        if (!kernelRunning) {
            kernelRunning = true

            scope.launch(Dispatchers.Default) {
                kernel.main()
                _nextActivity.postValue(kernel.nextActivity.value)
            }
        }
    }
}