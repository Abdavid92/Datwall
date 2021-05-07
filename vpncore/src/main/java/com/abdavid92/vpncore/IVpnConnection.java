package com.abdavid92.vpncore;

import android.app.PendingIntent;
import androidx.annotation.NonNull;

/**
 * Conexión vpn. Esta clase implementa la interfaz {@link Runnable}
 * para iniciar la conexión en un hilo de fondo.
 * */
public interface IVpnConnection extends Runnable {

    /**
     * Establece el nombre de la sesión vpn.
     *
     * @param sessionName - Nombre de la sesion.
     * */
    @NonNull
    IVpnConnection setSessionName(@NonNull String sessionName);

    /**
     * Configura un PendingIntent en el vpn.
     *
     * @param pendingIntent - {@link PendingIntent} - que se configura en el vpn.
     * */
    @NonNull
    IVpnConnection setPendingIntent(PendingIntent pendingIntent);

    /**
     * Establece las aplicaciones permitidas. (Aplicaciones que no se le
     * bloqueara la conexión a internet).
     * */
    @NonNull
    IVpnConnection setAllowedPackageNames(@NonNull String[] packageNames);

    /**
     * Subscribe un observador que recibirá todos los paquetes
     * entarntes y salientes.
     * */
    void subscribe(IObserverPacket observer);

    /**
     * Elimina una subscripción.
     * */
    void unsubscribe(IObserverPacket observer);

    /**
     * Indica si el vpn está conectado.
     * */
    boolean isConnected();

    /**
     * Apaga y cierra la conexión.
     * */
    void shutdown();
}
