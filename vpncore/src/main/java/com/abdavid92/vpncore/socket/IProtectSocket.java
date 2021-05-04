package com.abdavid92.vpncore.socket;

import androidx.annotation.NonNull;

import java.net.DatagramSocket;
import java.net.Socket;

public interface IProtectSocket {
	void protectSocket(@NonNull Socket socket);
	void protectSocket(int socket);
	void protectSocket(@NonNull DatagramSocket socket);
}
