package com.abdavid92.vpncore;

import android.app.PendingIntent;
import android.content.pm.PackageManager;
import android.net.VpnService;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import java.io.IOException;

/**
 * Implementación ligera del {@link IVpnConnection}.
 * Funciona en android 5 en adelante y no hace una rastreo de los
 * paquetes de red. Puede iniciarse en el hilo principal. Los observadores que se subscriban
 * no recibirán ningún paquete de red.
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
    public IVpnConnection setBlockedAddress(@NonNull String[] address) {
        return this;
    }

    @Override
    public void subscribe(IObserverPacket observer) {

    }

    @Override
    public void unsubscribe(IObserverPacket observer) {

    }

    @Override
    public void run() {
        if (isConnected())
            return;

        mInterface = configure().establish();
    }

    @Override
    public void shutdown() {
        try {
            mInterface.close();
        } catch (IOException ignored) {

        }
        mInterface = null;
    }

    @Override
    protected void configureAllowApps(VpnService.Builder builder) {
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
