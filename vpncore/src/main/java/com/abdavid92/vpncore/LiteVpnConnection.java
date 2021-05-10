package com.abdavid92.vpncore;

import android.app.PendingIntent;
import android.content.pm.PackageManager;
import android.net.VpnService;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import com.abdavid92.vpncore.exceptions.PacketHeaderException;
import com.abdavid92.vpncore.transport.ITransportHeader;
import com.abdavid92.vpncore.transport.ip.IPPacketFactory;
import com.abdavid92.vpncore.transport.ip.IPv4Header;
import com.abdavid92.vpncore.transport.tcp.TCPPacketFactory;
import com.abdavid92.vpncore.transport.udp.UDPPacketFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

import static com.abdavid92.vpncore.DataConst.MAX_PACKET_LENGTH;
import static com.abdavid92.vpncore.DataConst.PROTOCOL_TCP;
import static com.abdavid92.vpncore.DataConst.PROTOCOL_UDP;

/**
 * Implementación ligera del {@link IVpnConnection}.
 * Funciona en android 5 en adelante.
 * No puede iniciarse en el hilo principal.
 *
 * Realiza un rastreo de los paquetes tcp y udp salientes y se los provee
 * a los observadores subscritos.
 *
 * Soporta el protocolo ip version 4 y 6.
 *
 * La conexión se reinicia para aplicar los cambios de reglas de acceso, lo que puede provocar
 * pequeñas fugas de datos.
 * */
@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
public class LiteVpnConnection extends BaseVpnConnection {

    private String[] allowedPackageNames = null;

    public LiteVpnConnection(VpnService vpnService) {
        this.vpnService = vpnService;
    }

    @NonNull
    @Override
    public IVpnConnection setSessionName(@NonNull String sessionName) {
        this.sessionName = sessionName;
        return this;
    }

    @NonNull
    @Override
    public IVpnConnection setPendingIntent(PendingIntent pendingIntent) {
        this.pendingIntent = pendingIntent;
        return this;
    }

    @NonNull
    @Override
    public IVpnConnection setAllowedPackageNames(@NonNull String[] packageNames) {
        this.allowedPackageNames = packageNames;
        if (isConnected()) {
            VpnService.Builder builder = configure();
            shutdown();
            mInterface = builder.establish();
        }
        return this;
    }

    @Override
    public void subscribe(IObserverPacket observer) {
        if (!this.observers.contains(observer))
            this.observers.add(observer);
    }

    @Override
    public void unsubscribe(IObserverPacket observer) {
        this.observers.remove(observer);
    }

    @Override
    public void run() {
        assertMainThread();

        if (isConnected())
            return;

        mInterface = configure().establish();

        if (isConnected()) {
            FileInputStream clientRead = new FileInputStream(mInterface.getFileDescriptor());

            this.running = true;
            ByteBuffer packet = ByteBuffer.allocate(MAX_PACKET_LENGTH);
            byte[] data;
            int length;

            while (this.running && !Thread.interrupted()) {
                try {
                    data = packet.array();
                    length = clientRead.read(data);

                    if (length > 0) {
                        try {
                            packet.limit(length);

                            handlePacket(packet);
                        } catch (PacketHeaderException ignored) {
                        }
                        packet.clear();
                    }
                } catch (Exception ignored) {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    clientRead = new FileInputStream(mInterface.getFileDescriptor());
                }
            }

            try {
                clientRead.close();
                mInterface.close();
            } catch (IOException ignored) {

            }
            mInterface = null;
        }
    }

    private void handlePacket(ByteBuffer packet) throws PacketHeaderException {
        final IPv4Header ipHeader = IPPacketFactory.createIPv4Header(packet);

        ITransportHeader transportHeader;
        switch (ipHeader.getProtocol()) {
            case PROTOCOL_TCP:
                transportHeader = TCPPacketFactory.createTCPHeader(packet);
                break;
            case PROTOCOL_UDP:
                transportHeader = UDPPacketFactory.createUDPHeader(packet);
                break;
            default:
                return;
        }
        observePackets(new Packet(ipHeader, transportHeader, packet.array()));
    }

    @Override
    public void shutdown() {
        this.running = false;
        try {
            mInterface.close();
        } catch (IOException ignored) {

        }
        mInterface = null;
    }

    @Override
    protected VpnService.Builder configure() {
        VpnService.Builder builder = super.configure();
        configureAllowApps(builder);
        return builder;
    }

    private void configureAllowApps(VpnService.Builder builder) {
        try {
            builder.addDisallowedApplication(vpnService.getPackageName());
        } catch (PackageManager.NameNotFoundException ignored) { }
        if (allowedPackageNames != null) {
            for (String packageName: allowedPackageNames) {
                try {
                    builder.addDisallowedApplication(packageName);
                } catch (PackageManager.NameNotFoundException ignored) { }
            }
        }
    }
}
