package com.smartsolutions.paquetes.ui.applications

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.transition.Transition
import android.view.View
import android.widget.CompoundButton
import androidx.appcompat.app.AlertDialog
import androidx.core.view.ViewCompat
import com.smartsolutions.paquetes.R
import com.smartsolutions.paquetes.databinding.ActivityAppControlBinding
import com.smartsolutions.paquetes.managers.contracts.IIconManager
import com.smartsolutions.paquetes.repositories.models.App
import com.smartsolutions.paquetes.repositories.models.IApp
import com.smartsolutions.paquetes.repositories.models.TrafficType
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

const val VIEW_NAME_HEADER_IMAGE = "control:header:image"
const val VIEW_NAME_HEADER_NAME = "control:header:name"
const val VIEW_NAME_HEADER_PACKAGE_NAME = "control:header:package_name"
const val VIEW_NAME_HEADER_LAYOUT = "control:header:layout"

@AndroidEntryPoint
class AppControlActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAppControlBinding

    private var app: App? = null

    private var wasChanges = false

    @Inject
    lateinit var iconManager: IIconManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityAppControlBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setTransitionName(binding.appInfo, VIEW_NAME_HEADER_LAYOUT)
        ViewCompat.setTransitionName(binding.icon, VIEW_NAME_HEADER_IMAGE)
        ViewCompat.setTransitionName(binding.name, VIEW_NAME_HEADER_NAME)
        ViewCompat.setTransitionName(binding.packageName, VIEW_NAME_HEADER_PACKAGE_NAME)

        app = intent.getParcelableExtra(EXTRA_APP)

        wasChanges = savedInstanceState?.getBoolean(EXTRA_WAS_CHANGES) ?: false

        loadData()

        binding.backgroundLayout.setOnClickListener { onBackPressed() }
        binding.appInfo.setOnClickListener {
            //Empty para evitar el onCLick del background
        }
        binding.appControl.setOnClickListener {
            //Empty para evitar el onCLick del background
        }

        when (app?.trafficType) {
            TrafficType.International -> binding.trafficInternational.isChecked = true
            TrafficType.National -> binding.trafficNational.isChecked = true
            TrafficType.Free -> binding.trafficFree.isChecked = true
        }

        binding.trafficTypeGroup.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.traffic_international -> app?.trafficType = TrafficType.International
                R.id.traffic_national -> app?.trafficType = TrafficType.National
                R.id.traffic_free -> app?.trafficType = TrafficType.Free
            }
            wasChanges = true
        }

        setVpnAccessCheckBoxListener(binding.vpnAccess)
        setAskCheckboxListener(binding.ask)

        addTransitionListener()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(EXTRA_WAS_CHANGES, wasChanges)
    }

    private fun addTransitionListener() {
        window.sharedElementEnterTransition?.let {
            it.addListener(object : Transition.TransitionListener {
                override fun onTransitionStart(transition: Transition?) {

                }

                override fun onTransitionEnd(transition: Transition?) {
                    binding.appControl.animate()
                        .alpha(1F)
                    it.removeListener(this)
                }

                override fun onTransitionCancel(transition: Transition?) {
                    it.removeListener(this)
                }

                override fun onTransitionPause(transition: Transition?) {

                }

                override fun onTransitionResume(transition: Transition?) {

                }
            })
        }
    }

    fun onSave(view: View) {
        if (wasChanges) {
            setResult(
                RESULT_OK,
                Intent().putExtra(EXTRA_APP, app)
            )
        } else {
            setResult(RESULT_CANCELED)
        }
        onBackPressed()
    }

    private fun loadData() {
        app?.let { app ->
            binding.name.text = app.name
            binding.packageName.text = app.packageName
            binding.icon.setImageBitmap(iconManager.get(app.packageName, app.version))
        }
    }

    private fun setAskCheckboxListener(checkBox: CompoundButton) {
        checkBox.setOnCheckedChangeListener(null)
        checkBox.isChecked = app?.ask ?: false
        checkBox.setOnCheckedChangeListener { _, isChecked ->
            app?.ask = isChecked
            wasChanges = true
        }
    }

    /**
     * Asigna el evento onCheckedChange al checkBox y establece la propiedad
     * [CompoundButton.isChecked] de manera segura sin lanzar el evento
     * accidentalmente.
     * */
    private fun setVpnAccessCheckBoxListener(checkBox: CompoundButton) {
        checkBox.setOnCheckedChangeListener(null)
        checkBox.isChecked = app?.access ?: false
        checkBox.setOnCheckedChangeListener { _,_ ->
            handleWarningMessages(app, checkBox)
        }
    }

    /**
     * Maneja los mensajes de advertencia si los hay y cambia el acceso a la app.
     * */
    private fun handleWarningMessages(app: IApp?, checkBox: CompoundButton) {

        //Diálogo que se mostrará cuando exista un mensaje de advertencia
        val dialog = AlertDialog.Builder(this)
            .setTitle(R.string.warning_title)
            .setNegativeButton(R.string.btn_cancel
            ) { _,_ ->
                /*Si se oprime el botón cancelar se llama al método
                * setCheckBoxListener para restablecer el estado anterior del checkBox si
                * lanzar el evento de este. Este método utiliza la propiedad access de la app,
                * que no se ha cambiado todavía.*/
                setVpnAccessCheckBoxListener(checkBox)
            }
            .setPositiveButton(R.string.btn_continue) { _,_ ->
                app?.access = checkBox.isChecked
                wasChanges = true
            }

        if (checkBox.isChecked && app?.allowAnnotations != null) {
            dialog.setMessage(app.allowAnnotations)
                .show()
        } else if (!checkBox.isChecked && app?.blockedAnnotations != null) {
            dialog.setMessage(app.blockedAnnotations)
                .show()
        } else {
            /*Si no hay ningún mensaje de advertencia cambio la propiedad access y
            * notifico que hubo cambios.*/
            app?.access = checkBox.isChecked
            wasChanges = true
        }
    }

    companion object {
        const val EXTRA_APP = "com.smartsolutions.paquetes.ui.applications.extra.APP"
        const val EXTRA_WAS_CHANGES = "com.smartsolutions.paquetes.ui.applications.extra.WAS_CHANGES"
    }
}