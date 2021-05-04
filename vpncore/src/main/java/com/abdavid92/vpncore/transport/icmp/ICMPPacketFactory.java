package com.abdavid92.vpncore.transport.icmp;

import androidx.annotation.NonNull;

import com.abdavid92.vpncore.exceptions.PacketHeaderException;
import com.abdavid92.vpncore.transport.ip.IPPacketFactory;
import com.abdavid92.vpncore.transport.ip.IPv4Header;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;

import static com.abdavid92.vpncore.util.PacketUtil.calculateChecksum;

public class ICMPPacketFactory {

    public static ICMPPacket parseICMPPacket(@NonNull ByteBuffer stream) throws PacketHeaderException {
        final byte type = stream.get();
        final byte code = stream.get();
        final int checksum = stream.getShort();

        final int identifier = stream.getShort();
        final int sequenceNumber = stream.getShort();

        final byte[] data = new byte[stream.remaining()];
        stream.get(data);

        if (type == 8) {
            return new ICMPPacket(type, code, checksum, identifier, sequenceNumber, data);
        } else {
            throw new PacketHeaderException("Unknown ICMP type (" + type + "). Only echo requests are supported");
        }
    }

    public static ICMPPacket buildSuccessPacket(ICMPPacket requestPacket) throws PacketHeaderException {
        return new ICMPPacket(
            0,
            0,
            0,
            requestPacket.identifier,
            requestPacket.sequenceNumber,
            requestPacket.data
        );
    }

    public static byte[] packetToBuffer(IPv4Header ipHeader, ICMPPacket packet) throws PacketHeaderException {
        byte[] ipData = IPPacketFactory.createIPv4HeaderData(ipHeader);

        ByteArrayOutputStream icmpDataBuffer = new ByteArrayOutputStream();
        icmpDataBuffer.write(packet.type);
        icmpDataBuffer.write(packet.code);

        icmpDataBuffer.write(asShortBytes(0 /* checksum placeholder */), 0, 2);

        if (packet.type == ICMPPacket.ECHO_REQUEST_TYPE || packet.type == ICMPPacket.ECHO_SUCCESS_TYPE) {
            icmpDataBuffer.write(asShortBytes(packet.identifier), 0, 2);
            icmpDataBuffer.write(asShortBytes(packet.sequenceNumber), 0, 2);

            byte[] extraData = packet.data;
            icmpDataBuffer.write(extraData, 0, extraData.length);
        } else {
            throw new PacketHeaderException("Can't serialize unrecognized ICMP packet type");
        }

        byte[] icmpPacketData = icmpDataBuffer.toByteArray();
        byte[] checksum = calculateChecksum(icmpPacketData, 0, icmpPacketData.length);

        ByteBuffer resultBuffer = ByteBuffer.allocate(ipData.length + icmpPacketData.length);
        resultBuffer.put(ipData);
        resultBuffer.put(icmpPacketData);

        // Replace the checksum placeholder
        resultBuffer.position(ipData.length + 2);
        resultBuffer.put(checksum);
        resultBuffer.position(0);

        byte[] result = new byte[resultBuffer.remaining()];
        resultBuffer.get(result);
        return result;
    }

    private static byte[] asShortBytes(int value) {
        return ByteBuffer.allocate(2).putShort((short) value).array();
    }

}
