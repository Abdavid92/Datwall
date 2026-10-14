package com.smartsolutions.paquetes.ui.permissions

import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.smartsolutions.paquetes.databinding.LayoutPermissionNeededBinding
import com.smartsolutions.paquetes.managers.PermissionAction
import com.smartsolutions.paquetes.managers.contracts.IPermissionsManager
import kotlinx.coroutines.launch

/**
 * Muestra en lugar de [content] la explicación del permiso [requestCode] y un botón para
 * concederlo, si no está concedido. Devuelve si el permiso está concedido.
 *
 * Se llama en `onResume`: al volver de los ajustes o del diálogo del sistema se vuelve a
 * comprobar el permiso.
 * */
fun Fragment.bindPermissionNeeded(
    binding: LayoutPermissionNeededBinding,
    content: View,
    permissionsManager: IPermissionsManager,
    requestCode: Int
): Boolean {
    val permission = permissionsManager.findPermission(requestCode) ?: return true
    val granted = permission.checkPermission(permission, requireContext())

    content.visibility = if (granted) View.VISIBLE else View.GONE
    binding.root.visibility = if (granted) View.GONE else View.VISIBLE

    if (!granted) {
        binding.permissionNeededName.text = permission.name
        binding.permissionNeededDescription.text = permission.description
        binding.btnPermissionNeeded.setOnClickListener {
            viewLifecycleOwner.lifecycleScope.launch {
                when (permissionsManager.actionFor(permission, requireActivity())) {
                    PermissionAction.Nothing -> Unit
                    PermissionAction.OpenAppSettings -> requireContext().openAppSettings()
                    PermissionAction.Request -> {
                        permissionsManager.markRequested(requestCode)
                        permission.requestPermissionFragment(permission, this@bindPermissionNeeded)
                    }
                }
            }
        }
    }

    return granted
}
