package com.abdavid92.vpncore;

import android.app.PendingIntent;
import android.net.VpnService;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelFileDescriptor;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

import static com.abdavid92.vpncore.DataConst.MAX_PACKET_LENGTH;

public abstract class BaseVpnConnection implements IVpnConnection {

    protected VpnService vpnService;
    protected ParcelFileDescriptor mInterface = null;
    protected String sessionName = "FirewallService";
    protected PendingIntent pendingIntent = null;
    protected final List<IObserverPacket> observers = new ArrayList<>();
    protected final Handler observerHandler = new Handler(Looper.getMainLooper());
    protected boolean running = false;

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

    @Override
    public boolean isConnected() {
        return mInterface != null && mInterface.getFileDescriptor() != null &&
                mInterface.getFileDescriptor().valid();
    }

    protected void observePackets(@Nullable Packet packet) {
        if (packet != null) {
            observerHandler.post(() -> {
                for (IObserverPacket observer : observers) {
                    observer.observe(packet);
                }
            });
        }
    }

    @NonNull
    protected VpnService.Builder configure() {
        VpnService.Builder builder = vpnService.new Builder()
                .addAddress("192.168.0.32", 32)
                .addAddress("fd00:1:fd00:1:fd00:1:fd00:1", 128)
                .addRoute("0.0.0.0", 0)
                .addRoute("0:0:0:0:0:0:0:0", 0)
                .setMtu(MAX_PACKET_LENGTH)
                .setSession(sessionName);

        if (pendingIntent != null)
            builder.setConfigureIntent(pendingIntent);

        return builder;
    }

    protected void assertMainThread() {
        if (Thread.currentThread().getName().equals("main"))
            throw new IllegalThreadStateException("Vpn not running in main thread");
    }
}
