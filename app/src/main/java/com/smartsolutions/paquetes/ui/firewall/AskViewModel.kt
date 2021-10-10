package com.smartsolutions.paquetes.ui.firewall

import android.widget.ImageView
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartsolutions.paquetes.managers.contracts.IIconManager
import com.smartsolutions.paquetes.repositories.contracts.IAppRepository
import com.smartsolutions.paquetes.repositories.models.App
import com.smartsolutions.paquetes.watcher.WatcherUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class AskViewModel @Inject constructor(
    private val appRepository: IAppRepository,
    private val iconManager: IIconManager,
    private val watcherUtils: WatcherUtils
) : ViewModel() {

    fun fillIcon(img: ImageView, app: App) {
        iconManager.getAsync(
            packageName = app.packageName,
            versionCode = app.version
        ) {
            img.setImageBitmap(it)
        }
    }

    fun updateApp(app: App, task: (() -> Unit)? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            appRepository.update(app)

            withContext(Dispatchers.Main) {
                task?.invoke()
            }
        }
    }

    fun getForegroundApp() = watcherUtils.getLastApp()
}