package com.abdavid92.vpncore;

import android.content.Context;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.VpnService;

import androidx.annotation.NonNull;

import com.abdavid92.vpncore.exceptions.PacketHeaderException;
import com.abdavid92.vpncore.socket.IProtectSocket;
import com.abdavid92.vpncore.socket.SocketNIODataService;
import com.abdavid92.vpncore.socket.SocketProtector;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

import static com.abdavid92.vpncore.DataConst.MAX_PACKET_LENGTH;

/**
 * Implementación de la Interfaz {@link IVpnConnection}.
 *
 * Funciona en android 4 en adelante. Implementa su propio sistema para el
 * manejo de todas las conexiones de red del dispositivo y decide bloquearlas o no
 * basado en la lista de aplicaciones permitidas que se le configura.
 *
 * Realiza un rastreo de los paquetes tcp y udp salientes y se los provee
 * a los observadores subscritos.
 *
 * La conexión no se reinicia para aplicar los cambios en las reglas de acceso.
 *
 * No soporta el protocolo ip versión 6. Las conexiones con esta versión ip serán bloqueadas
 * automáticamente.
 *
 * Esta implementación no debe iniciarse en el hilo principal porque se lanzará una
 * {@link IllegalThreadStateException}. Esto es debido a que la conexión vpn debe trabajar
 * en un hilo de fondo para poder capturar todos los paquetes de red sin bloquear el hilo de la ui.
 * */
public class TrackerVpnConnection extends BaseVpnConnection {

    private int[] allowedUid = new int[0];
    private SessionHandler handler = null;
    private boolean allowUnknownUid = false;

    /**
     * Crea una instancia de {@link TrackerVpnConnection}.
     *
     * @param vpnService - Servicio vpn. Este servicio debe implementar la interfaz
     * {@link IProtectSocket}, de lo contrario se lanzará una excepción.
     *
     * @throws IllegalArgumentException si el servicio vpn no implementa la interfaz
     * {@link IProtectSocket}
     * */
    public TrackerVpnConnection(@NonNull VpnService vpnService) {
        this.vpnService = vpnService;

        if (!(vpnService instanceof IProtectSocket))
            throw new IllegalArgumentException("VpnService must be instance of IProtectSocket");
    }

    public void allowUnknownUid(boolean allow) {
        this.allowUnknownUid = allow;
        if (handler != null)
            handler.allowUnknownUid(this.allowUnknownUid);
    }

    @NonNull
    @Override
    public IVpnConnection setAllowedPackageNames(@NonNull String[] packageNames) {
        PackageManager pm = vpnService.getPackageManager();

        int[] uids = new int[packageNames.length];

        for (int i = 0; i < uids.length; i++) {
            try {
                uids[i] = pm.getPackageInfo(packageNames[i], PackageManager.GET_META_DATA)
                        .applicationInfo
                        .uid;
            } catch (PackageManager.NameNotFoundException e) {
                uids[i] = 0;
            }
        }
        this.allowedUid = uids;
        if (handler != null)
            handler.setAllowedUids(this.allowedUid);
        return this;
    }

    @Override
    public void run() {
        assertMainThread();
        if (isConnected())
            return;

        mInterface = configure().establish();
        if (isConnected()) {

            SocketProtector.getInstance()
                    .setProtector((IProtectSocket) this.vpnService);

            FileInputStream clientReader = new FileInputStream(mInterface.getFileDescriptor());
            FileOutputStream clientWriter = new FileOutputStream(mInterface.getFileDescriptor());

            ByteBuffer packet = ByteBuffer.allocate(MAX_PACKET_LENGTH);
            IClientPacketWriter clientPacketWriter = new ClientPacketWriterImpl(clientWriter);

            handler = SessionHandler.getInstance(
                    clientPacketWriter,
                    (ConnectivityManager) vpnService.getSystemService(Context.CONNECTIVITY_SERVICE)
            );
            handler.setAllowedUids(this.allowedUid);
            handler.allowUnknownUid(this.allowUnknownUid);

            SocketNIODataService dataService = new SocketNIODataService(clientPacketWriter);
            Thread dataServiceThread = new Thread(dataService);
            dataServiceThread.start();

            byte[] data;
            int length;
            running = true;

            while (running && !Thread.interrupted()) {

                try {
                    data = packet.array();
                    length = clientReader.read(data);

                    if (length > 0) {
                        try {
                            packet.limit(length);

                            observePackets(handler.handlePacket(packet));
                        } catch (PacketHeaderException ignored) {

                        }
                        packet.clear();
                    } else {
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException ignored) {

                        }
                    }
                } catch (Exception ignored) {

                }

            }

            dataService.setShutdown(true);
            try {
                clientReader.close();
                clientWriter.close();
                mInterface.close();
            } catch (IOException ignored) {

            }
            mInterface = null;
        }
    }

    @Override
    public void subscribe(@NonNull IObserverPacket observer) {
        if (!observers.contains(observer))
            observers.add(observer);
    }


    @Override
    public void unsubscribe(@NonNull IObserverPacket observer) {
        observers.remove(observer);
    }

    @Override
    public void shutdown() {
        this.running = false;
    }
}
