package com.smartsolutions.paquetes.managers

import com.abdavid92.vpncore.IObserverPacket
import com.abdavid92.vpncore.Packet

class PacketManager private constructor() {

    private val subscriptions = mutableListOf<IObserverPacket>()

    fun subscribe(observer: IObserverPacket) {
        if (!subscriptions.contains(observer))
            subscriptions.add(observer)
    }

    fun unsubscribe(observer: IObserverPacket) {
        subscriptions.remove(observer)
    }

    fun sendPacket(packet: Packet) {
        subscriptions.forEach {
            it.observe(packet)
        }
    }

    companion object {

        fun getInstance(): PacketManager {
            if (packetManager == null)
                packetManager = PacketManager()
            return packetManager!!
        }

        private var packetManager: PacketManager? = null
    }
}