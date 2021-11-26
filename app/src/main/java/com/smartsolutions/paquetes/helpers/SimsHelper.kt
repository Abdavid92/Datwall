package com.smartsolutions.paquetes.helpers

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
import androidx.fragment.app.FragmentManager
import com.abdavid92.alertbottomdialog.AlertBottomDialog
import com.smartsolutions.paquetes.R
import com.smartsolutions.paquetes.databinding.FragmentSimsDefaultDialogBinding
import com.smartsolutions.paquetes.managers.contracts.ISimManager2
import com.smartsolutions.paquetes.repositories.models.Sim
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject


class SimsHelper @Inject constructor(
    private val simManager: ISimManager2
) {

    private var dialog: AlertBottomDialog? = null

    suspend fun invokeOnDefault(
        context: Context,
        sim: Sim,
        simType: SimDelegate.SimType,
        fragmentManager: FragmentManager,
        onDefault: () -> Unit
    ) {
        val result = simManager.isSimDefaultSystem(simType, sim)
        when {
            //No se cual es la default y pregunto para confirmar
            result == null -> {
                dialog = AlertBottomDialog.Builder(context)
                    .setView(inflateView(context,true, simType, sim, onDefault))
                    .show(fragmentManager)
            }
            result -> {
                withContext(Dispatchers.Main) {
                    onDefault()
                }
            }
            //No es la default. Informo que no se puede realizar la acción
            else -> {
                dialog = AlertBottomDialog.Builder(context)
                    .setView(inflateView(context,false, simType, sim, onDefault))
                    .show(fragmentManager)
            }
        }
    }

    private fun inflateView(context: Context, ask: Boolean, type: SimDelegate.SimType, sim: Sim, onDefault: () -> Unit): View {
        val binding =
            FragmentSimsDefaultDialogBinding.inflate(LayoutInflater.from(context))

        binding.apply {

            sim.icon?.let {
                imageSim.setImageBitmap(it)
            }

            if (!ask){

                when (type) {
                    SimDelegate.SimType.VOICE -> {
                        title.text = context.getString(R.string.no_sim_default_voice, sim.name())
                        imageType.setImageResource(R.drawable.ic_call_24)
                    }
                    SimDelegate.SimType.DATA -> {
                        title.text = context.getString(R.string.no_sim_default_data, sim.name())
                        imageType.setImageResource(R.drawable.ic_data_24)
                    }
                }

                imageAction.setImageResource(R.drawable.ic_arrow_forward_24)

                description.text = context.getString(R.string.no_sim_default_summary)

                buttonCancel.visibility = View.GONE
                buttonDone.text = context.getString(R.string.btn_ok)
                buttonDone.setOnClickListener {
                    dialog?.dismiss()
                }

            }else {

                when (type) {
                    SimDelegate.SimType.VOICE -> {
                        title.text =
                            context.getString(R.string.is_default_sim_voice, sim.name())
                        imageType.setImageResource(R.drawable.ic_call_24)
                    }
                    SimDelegate.SimType.DATA -> {
                        title.text =
                            context.getString(R.string.is_default_sim_data, sim.name())
                        imageType.setImageResource(R.drawable.ic_data_24)
                    }
                }

                imageAction.setImageResource(R.drawable.ic_question_mark_24)

                description.text = context.getString(R.string.is_default_sim_summary)

                buttonCancel.setOnClickListener {
                    dialog?.dismiss()
                }

                buttonDone.setOnClickListener {
                    onDefault.invoke()
                }

            }
        }


        return binding.root
    }


}