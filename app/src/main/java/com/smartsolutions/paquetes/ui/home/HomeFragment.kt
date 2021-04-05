package com.smartsolutions.paquetes.ui.home

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Switch
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.smartsolutions.paquetes.MainActivity
import com.smartsolutions.paquetes.R
import com.smartsolutions.paquetes.services.FirewallService

class HomeFragment : Fragment() {

    private val REQUEST_VPN_PERMISSION = 932
    private lateinit var homeViewModel: HomeViewModel

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        homeViewModel =
                ViewModelProvider(this).get(HomeViewModel::class.java)
        val root = inflater.inflate(R.layout.fragment_home, container, false)
        val textView: TextView = root.findViewById(R.id.text_home)
        homeViewModel.text.observe(viewLifecycleOwner, Observer {
            textView.text = it
        })
        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val switch = view.findViewById<Switch>(R.id.switch1)

        switch.setOnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked) {
                val intent = VpnService.prepare(context)

                if (intent != null)
                    startActivityForResult(intent, REQUEST_VPN_PERMISSION)
                else
                    context?.startService(Intent(context, FirewallService::class.java))
            } else {
                val intent = Intent(context, FirewallService::class.java).apply {
                    action = FirewallService.ACTION_STOP_FIREWALL_SERVICE
                }
                context?.startService(intent)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_VPN_PERMISSION && resultCode == Activity.RESULT_OK)
            context?.startService(Intent(context, FirewallService::class.java))
    }
}