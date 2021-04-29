package com.smartsolutions.paquetes.ui.home

import android.app.Activity
import android.content.Intent
import android.net.VpnService
import android.os.Bundle
import android.view.*
import android.widget.Button
import android.widget.EditText
import android.widget.Switch
import android.widget.TextView
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.smartsolutions.paquetes.ui.ApplicationFragment
import com.smartsolutions.paquetes.ui.MainActivity
import com.smartsolutions.paquetes.R
import com.smartsolutions.paquetes.firewall.VpnConnectionUtils
import com.smartsolutions.paquetes.services.FirewallService

class HomeFragment : ApplicationFragment() {

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
                VpnConnectionUtils.startVpn(requireContext())?.let {
                    startActivityForResult(it, REQUEST_VPN_PERMISSION)
                }
            } else {
                VpnConnectionUtils.stopVpn(requireContext())
            }
        }

        val edit = view.findViewById<EditText>(R.id.editTextNumberSigned)

        val button = view.findViewById<Button>(R.id.button)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_VPN_PERMISSION && resultCode == Activity.RESULT_OK)
            context?.startService(Intent(context, FirewallService::class.java))
    }
}