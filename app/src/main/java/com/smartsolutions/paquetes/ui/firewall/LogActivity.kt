package com.smartsolutions.paquetes.ui.firewall

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Html
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.abdavid92.vpncore.IObserverPacket
import com.abdavid92.vpncore.Packet
import com.abdavid92.vpncore.util.PacketUtil
import com.google.android.material.textview.MaterialTextView
import com.smartsolutions.paquetes.R
import com.smartsolutions.paquetes.databinding.ItemLogBinding
import com.smartsolutions.paquetes.managers.PacketManager
import com.smartsolutions.paquetes.repositories.contracts.IAppRepository
import com.smartsolutions.paquetes.repositories.models.App
import com.smartsolutions.paquetes.repositories.models.AppGroup
import com.smartsolutions.paquetes.repositories.models.IApp
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@AndroidEntryPoint
class LogActivity : AppCompatActivity(R.layout.activity_log), IObserverPacket {

    private lateinit var log: RecyclerView
    private lateinit var adapter: LogAdapter

    @Inject
    lateinit var appRepository: IAppRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setSupportActionBar(findViewById(R.id.toolbar))

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        log = findViewById(R.id.log)
        log.layoutManager = LinearLayoutManager(this)
        log.addItemDecoration(DividerItemDecoration(this, DividerItemDecoration.VERTICAL))
        adapter = LogAdapter(appRepository)
    }

    override fun onStart() {
        super.onStart()

        log.adapter = adapter

        PacketManager.getInstance()
            .subscribe(this)
    }

    override fun observe(packet: Packet) {
        adapter.addPacket(packet)
    }

    override fun onDestroy() {
        super.onDestroy()

        PacketManager.getInstance()
            .unsubscribe(this)
    }

    internal class LogAdapter(
        appRepository: IAppRepository
    ): RecyclerView.Adapter<LogAdapter.ViewHolder>() {

        private var _list = mutableListOf<Packet>()
        private var apps: List<IApp>? = null

        init {
            GlobalScope.launch {
                apps = appRepository.getAllByGroup()
            }
        }

        fun addPacket(packet: Packet) {
            _list.add(packet)
            notifyItemChanged(_list.size - 1)
        }

        override fun onCreateViewHolder(p0: ViewGroup, p1: Int): ViewHolder =
            ViewHolder(
                DataBindingUtil.inflate(
                    LayoutInflater.from(p0.context),
                    R.layout.item_log,
                    p0,
                    false),
                apps
            )

        override fun onBindViewHolder(p0: ViewHolder, p1: Int) {
            p0.bind(_list[p1])
        }

        override fun getItemCount(): Int = _list.size

        class ViewHolder(
            private val binding: ItemLogBinding,
            private val apps: List<IApp>?
            ): RecyclerView.ViewHolder(binding.root) {

            private val context = itemView.context

            fun bind(packet: Packet) {
                binding.srcIp.text = Html.fromHtml(
                    context.getString(
                        R.string.src_ip_template,
                        PacketUtil.intToIPAddress(packet.ipHeader.sourceIP),
                        packet.transportHeader.sourcePort
                    )
                )
                binding.destIp.text = Html.fromHtml(
                    context.getString(
                        R.string.dest_ip_template,
                        PacketUtil.intToIPAddress(packet.ipHeader.destinationIP),
                        packet.transportHeader.destinationPort
                    )
                )
                binding.uid.text = packet.transportHeader.uid.toString()
                computePackageNames(packet.transportHeader.uid)
            }

            //TODO: Mala práctica de programación
            private fun computePackageNames(uid: Int) {
                val iapp = apps?.firstOrNull { it.uid == uid }
                if (iapp != null) {
                    if (iapp is AppGroup) {
                        binding.packageName.text = iapp.name
                    } else if (iapp is App){
                        binding.packageName.text = iapp.packageName
                    }
                } else {
                    binding.packageName.text = "unknown"
                }
            }

        }
    }
}