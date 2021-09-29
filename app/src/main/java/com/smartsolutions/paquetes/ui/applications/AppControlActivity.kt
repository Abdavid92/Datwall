package com.smartsolutions.paquetes.ui.applications

import android.content.Intent
import android.os.Bundle
import android.transition.Transition
import android.view.View
import android.widget.CompoundButton
import androidx.core.view.ViewCompat
import com.smartsolutions.paquetes.R
import com.smartsolutions.paquetes.databinding.ActivityAppControlBinding
import com.smartsolutions.paquetes.helpers.UIHelper
import com.smartsolutions.paquetes.managers.contracts.IIconManager
import com.smartsolutions.paquetes.repositories.models.App
import com.smartsolutions.paquetes.repositories.models.TrafficType
import com.smartsolutions.paquetes.ui.TransparentActivity
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Nombres de las transiciones
 * */
const val VIEW_NAME_HEADER_IMAGE = "control:header:image"
const val VIEW_NAME_HEADER_NAME = "control:header:name"
const val VIEW_NAME_HEADER_PACKAGE_NAME = "control:header:package_name"
const val VIEW_NAME_HEADER_LAYOUT = "control:header:layout"

/**
 * Actividad que contiene los controles de una [App]
 * */
@AndroidEntryPoint
class AppControlActivity : TransparentActivity() {

    /**
     * Enlace a la vista.
     * */
    private lateinit var binding: ActivityAppControlBinding

    /**
     * Aplicación.
     * */
    private var app: App? = null

    /**
     * Indica si hubo cambios en la aplicación.
     * */
    private var wasChanges = false

    @Inject
    lateinit var iconManager: IIconManager

    private var uiHelper = UIHelper(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityAppControlBinding.inflate(layoutInflater)
        setContentView(binding.root)

        /*
         * Se establece los nombres de las transiciones para activar las animaciones.
         * */
        ViewCompat.setTransitionName(binding.appInfo, VIEW_NAME_HEADER_LAYOUT)
        ViewCompat.setTransitionName(binding.icon, VIEW_NAME_HEADER_IMAGE)
        ViewCompat.setTransitionName(binding.name, VIEW_NAME_HEADER_NAME)
        ViewCompat.setTransitionName(binding.packageName, VIEW_NAME_HEADER_PACKAGE_NAME)

        app = intent.getParcelableExtra(EXTRA_APP)

        savedInstanceState?.let {
            wasChanges = it.getBoolean(EXTRA_WAS_CHANGES, false)
            showControlPanel()
        }

        loadData()

        //Evento click del fondo para cerrar la actvidad.
        binding.backgroundLayout.setOnClickListener { onBackPressed() }
        binding.appInfo.setOnClickListener {
            //Empty para evitar el onCLick del background
        }
        binding.appControl.setOnClickListener {
            //Empty para evitar el onCLick del background
        }

        /*Selecciono el RadioButton correspondiente al trafficType de la app.*/
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
            //Indico que hubo cambios
            wasChanges = true
        }

        app?.let {
            //Asigno el evento del checkBox del vpn
            uiHelper.setVpnAccessCheckBoxListener(it, binding.vpnAccess) {
                wasChanges = true
            }
        }
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
                    /*Cuando la transición termina muestro el panel de control*/
                    showControlPanel()
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
            /*Si hubo cambios establezco el resultado en ok e
            * inserto la app con los cambios.*/
            setResult(
                RESULT_OK,
                Intent().putExtra(EXTRA_APP, app)
            )
        } else {
            //Sino establezco el resultado en canceled
            setResult(RESULT_CANCELED)
        }
        onBackPressed()
    }

    private fun showControlPanel() {
        binding.appControl.animate()
            .alpha(1F)
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

    companion object {
        const val EXTRA_APP = "com.smartsolutions.paquetes.ui.applications.extra.APP"
        const val EXTRA_WAS_CHANGES = "com.smartsolutions.paquetes.ui.applications.extra.WAS_CHANGES"
    }
}