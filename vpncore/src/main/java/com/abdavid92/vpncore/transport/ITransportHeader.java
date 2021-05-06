package com.abdavid92.vpncore.transport;

/**
 * Created by Lipi on 2017. 3. 27..
 */
public interface ITransportHeader {
    int getSourcePort();
    int getDestinationPort();
    int getUid();
}
