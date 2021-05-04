package com.abdavid92.vpncore;

import android.app.PendingIntent;
import android.net.VpnService;
import android.os.ParcelFileDescriptor;

import static com.abdavid92.vpncore.DataConst.MAX_PACKET_LENGTH;

public abstract class BaseVpnConnection implements IVpnConnection {

    protected VpnService vpnService;
    protected ParcelFileDescriptor mInterface = null;
    protected String sessionName = "FirewallService";
    protected PendingIntent pendingIntent = null;

    @Override
    public boolean isConnected() {
        return mInterface != null && mInterface.getFileDescriptor() != null &&
                mInterface.getFileDescriptor().valid();
    }

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

        configureAllowApps(builder);

        return builder;
    }

    protected abstract void configureAllowApps(VpnService.Builder builder);
}
