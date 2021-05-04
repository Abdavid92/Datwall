package com.abdavid92.vpncore;

import androidx.annotation.NonNull;

public interface IObserverPacket {
    void observe(@NonNull Packet packet);
}
