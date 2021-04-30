package com.smartsolutions.paquetes.ui.dashboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.viewModels
import com.google.android.material.button.MaterialButton
import com.smartsolutions.paquetes.ui.ApplicationFragment
import com.smartsolutions.paquetes.R
import com.smartsolutions.paquetes.helpers.USSDHelper
import com.smartsolutions.paquetes.helpers.string
import com.smartsolutions.paquetes.managers.IDataPackageManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import javax.inject.Inject
import kotlin.coroutines.coroutineContext

@AndroidEntryPoint
class DashboardFragment : ApplicationFragment() {

    private val dashboardViewModel by viewModels<DashboardViewModel>()

    @Inject
    lateinit var ussdHelper: USSDHelper

    @Inject
    lateinit var dataPackageManager: IDataPackageManager

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_dashboard, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val textView: TextView = view.findViewById(R.id.text_dashboard)
        /*dashboardViewModel.text.observe(viewLifecycleOwner, Observer {
            textView.text = it
        })*/

        view.findViewById<MaterialButton>(R.id.delete_one).setOnClickListener(::deleteOne)

        dashboardViewModel.apps.observe(viewLifecycleOwner, {
            textView.text = "Cantidad de aplicaciones: ${it.size}"
        })
    }
    
    private fun deleteOne(view: View) {
        //dashboardViewModel.deleteOne()
        GlobalScope.launch {
            dataPackageManager.configureDataPackages()
        }
    }
}