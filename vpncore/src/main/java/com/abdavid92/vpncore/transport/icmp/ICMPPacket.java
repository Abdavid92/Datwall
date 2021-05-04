package com.abdavid92.vpncore.transport.icmp;

import androidx.annotation.NonNull;

import com.abdavid92.vpncore.exceptions.PacketHeaderException;

public class ICMPPacket {
    public static final byte ECHO_REQUEST_TYPE = 8;
    public static final byte ECHO_SUCCESS_TYPE = 0;

    final byte type;
    final byte code; // 0 for request, 0 for success, 0 - 15 for error subtypes

    final int checksum;
    final int identifier;
    final int sequenceNumber;

    final byte[] data;

    ICMPPacket(
        int type,
        int code,
        int checksum,
        int identifier,
        int sequenceNumber,
        byte[] data
    ) throws PacketHeaderException {
        if (type != ECHO_REQUEST_TYPE && type != ECHO_SUCCESS_TYPE) {
            throw new PacketHeaderException("ICMP packet with id must be request or response");
        }

        this.type = (byte) type;
        this.code = (byte) code;
        this.checksum = checksum;
        this.identifier = identifier;
        this.sequenceNumber = sequenceNumber;
        this.data = data;
    }

    @NonNull
    public String toString() {
        return "ICMP packet type " + type + "/" + code + " id:" + identifier +
                " seq:" + sequenceNumber + " and " + data.length + " bytes of data";
    }
}
