package com.smartsolutions.paquetes.ui.permissions

import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatButton
import androidx.core.app.ActivityCompat
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.textview.MaterialTextView
import com.smartsolutions.paquetes.R
import com.smartsolutions.paquetes.managers.PermissionAction
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/**
 * Hoja inferior que explica un permiso y lo pide. Se abre con [showPermissionSheet] y devuelve su
 * resultado a [onPermissionResult]; la pantalla de origen no le pasa ningún objeto de respuesta.
 *
 * Los permisos del sistema se piden al diálogo propio de Android; los especiales (acceso de uso,
 * superposición, VPN) abren su pantalla de ajustes. Si el permiso fue rechazado de forma permanente
 * el botón lleva a los ajustes de la app.
 */
@AndroidEntryPoint
class SinglePermissionFragment : BottomSheetDialogFragment() {

    private val viewModel by viewModels<SinglePermissionViewModel>()

    private val permission get() = viewModel.state.value.permission

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_single_permission, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        view.findViewById<MaterialTextView>(R.id.permission_name)
            .setText(PermissionTexts.name(permission))

        view.findViewById<MaterialTextView>(R.id.description)
            .setText(PermissionTexts.description(permission))

        view.findViewById<AppCompatButton>(R.id.btn_jump).setOnClickListener {
            viewModel.onSkipClicked()
        }

        val grantButton = view.findViewById<AppCompatButton>(R.id.btn_grant)

        grantButton.setOnClickListener {
            viewModel.onGrantClicked()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.state.collect { state ->
                        if (state.action == PermissionAction.OpenAppSettings)
                            grantButton.setText(R.string.btn_open_settings)
                    }
                }
                launch { viewModel.events.collect(::onEvent) }
            }
        }
    }

    override fun onResume() {
        super.onResume()

        viewModel.onResumed()
        viewModel.onShown(canAskAgain())
    }

    /**
     * El sistema todavía deja volver a preguntar por el permiso. Se calcula aquí porque necesita la
     * actividad, y se entrega a la lógica como un dato.
     * */
    private fun canAskAgain() = permission.keys.any {
        ActivityCompat.shouldShowRequestPermissionRationale(requireActivity(), it)
    }

    private fun onEvent(event: SinglePermissionEvent) {
        when (event) {
            SinglePermissionEvent.RequestFromSystem -> PermissionLauncher.request(permission, this)

            SinglePermissionEvent.OpenAppSettings -> requireContext().openAppSettings()

            is SinglePermissionEvent.Finish -> {
                sendPermissionResult(permission.requestCode, event.granted)
                dismiss()
            }

            SinglePermissionEvent.SimSetupPending -> {
                requireActivity().openStartScreen()
                dismiss()
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        // Una petición interrumpida llega sin resultados: no es ni un sí ni un no.
        if (requestCode == permission.requestCode && grantResults.isNotEmpty()) {
            viewModel.onRequestPermissionsResult(
                grantResults.all { it == PackageManager.PERMISSION_GRANTED }
            )
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == permission.requestCode)
            viewModel.onReturnedFromSystem()
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)

        // DialogFragment también llama a onDismiss cuando la pantalla se recrea (por ejemplo, al
        // girar): eso no es una respuesta del usuario.
        if (activity?.isChangingConfigurations != false)
            return

        // Descartada sin responder (gesto de volver o tocar fuera).
        if (viewModel.onDismissed() && isAdded)
            sendPermissionResult(permission.requestCode, false)
    }

    companion object {

        fun newInstance(requestCode: Int): SinglePermissionFragment =
            SinglePermissionFragment().apply {
                arguments = Bundle().apply {
                    putInt(SinglePermissionViewModel.ARG_REQUEST_CODE, requestCode)
                }
            }
    }
}
