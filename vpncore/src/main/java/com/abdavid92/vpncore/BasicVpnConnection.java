package com.abdavid92.vpncore;

import android.net.VpnService;


public class BasicVpnConnection extends LiteVpnConnection {

    public BasicVpnConnection(VpnService vpnService) {
        super(vpnService);
    }

    @Override
    public void run() {
        if (isConnected())
            return;

        mInterface = configure().establish();
    }
}
