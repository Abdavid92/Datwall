package com.smartsolutions.paquetes.ui.permissions

import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.smartsolutions.paquetes.R
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/**
 * Hoja que lleva al usuario a activar el servicio de accesibilidad. Se abre con
 * [showAccessibilitySheet] y devuelve su resultado a [onAccessibilityResult].
 * */
@AndroidEntryPoint
class StartAccessibilityServiceFragment : BottomSheetDialogFragment() {

    private val viewModel by viewModels<StartAccessibilityServiceViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(
            R.layout.fragment_start_accessibility_service,
            container,
            false
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<Button>(R.id.btn_open_settings)
            .setOnClickListener {
                viewModel.onOpenSettingsClicked()
            }

        view.findViewById<Button>(R.id.btn_jump)
            .setOnClickListener {
                viewModel.onSkipClicked()
            }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.events.collect { event ->
                    when (event) {
                        is StartAccessibilityEvent.Finish -> {
                            sendAccessibilityResult(event.granted)
                            dismiss()
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.onResumed()
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)

        // DialogFragment también llama a onDismiss cuando la pantalla se recrea (por ejemplo, al
        // girar): eso no es una respuesta del usuario.
        if (activity?.isChangingConfigurations != false)
            return

        // Descartada sin responder (gesto de volver o tocar fuera).
        if (viewModel.onDismissed() && isAdded)
            sendAccessibilityResult(false)
    }
}
