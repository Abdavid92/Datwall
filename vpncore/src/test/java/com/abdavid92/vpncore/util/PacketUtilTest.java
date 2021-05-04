package com.abdavid92.vpncore.util;

import android.net.InetAddresses;

import com.abdavid92.vpncore.exceptions.PacketHeaderException;

import org.junit.Test;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;

import static org.junit.Assert.*;

public class PacketUtilTest {

    @Test
    public void intToIPAddress() {
        int r1 = (int) (192 * Math.pow(2, 24));
        int r2 = (int) (168 * Math.pow(2, 16));
        int r3 = (int) (143 * Math.pow(2, 8));
        int r4 = 1;

        int tr = Math.abs(r1 + r2 + r3 + r4);

        String ip = PacketUtil.intToIPAddress(tr);

        System.out.print(ip);
    }

    @Test
    public void ipAddressToInt() throws PacketHeaderException {
        String ip = "64.233.187.99";
        String ip2 = "192.168.143.1";

        int n = PacketUtil.ipAddressToInt(ip2);

        assertEquals(ip2, PacketUtil.intToIPAddress(n));
    }
}