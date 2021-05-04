package com.abdavid92.vpncore.socket;

import androidx.annotation.NonNull;

public interface IReceivePacket {
	void receive(@NonNull byte[] packet);
}
