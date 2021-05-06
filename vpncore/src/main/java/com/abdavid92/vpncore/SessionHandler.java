/*
 *  Copyright 2014 AT&T
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
*/

package com.abdavid92.vpncore;

import android.net.ConnectivityManager;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.abdavid92.vpncore.exceptions.PacketHeaderException;
import com.abdavid92.vpncore.socket.SocketData;
import com.abdavid92.vpncore.transport.ITransportHeader;
import com.abdavid92.vpncore.transport.icmp.ICMPPacket;
import com.abdavid92.vpncore.transport.icmp.ICMPPacketFactory;
import com.abdavid92.vpncore.transport.ip.IPPacketFactory;
import com.abdavid92.vpncore.transport.ip.IPv4Header;
import com.abdavid92.vpncore.transport.tcp.TCPHeader;
import com.abdavid92.vpncore.transport.tcp.TCPPacketFactory;
import com.abdavid92.vpncore.transport.udp.UDPHeader;
import com.abdavid92.vpncore.transport.udp.UDPPacketFactory;
import static com.abdavid92.vpncore.util.PacketUtil.*;
import static com.abdavid92.vpncore.DataConst.*;

import java.io.IOException;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * handle VPN client request and response. it create a new session for each VPN client.
 * @author Borey Sao
 * Date: May 22, 2014
 */
public class SessionHandler {

	private static final String TAG = "SessionHandler";

	private static final SessionHandler handler = new SessionHandler();
	private IClientPacketWriter writer = null;
	private final SocketData packetData = SocketData.getInstance();
	private ConnectivityManager connectivityManager = null;
	private final ExecutorService pingThreadpool;
	private int[] allowedUids = new int[0];
	private String[] blockedAddress = new String[0];
	private boolean allowUnknownUid = false;

	public static SessionHandler getInstance(IClientPacketWriter writer, ConnectivityManager connectivityManager) {
		if (handler.writer == null)
			handler.writer = writer;

		if (handler.connectivityManager == null)
			handler.connectivityManager = connectivityManager;

		return handler;
	}

	private SessionHandler() {

		// Pool of threads to synchronously proxy ICMP ping requests in the background. We need to
		// carefully limit these, or a ping flood can cause us big big problems.
		this.pingThreadpool = new ThreadPoolExecutor(
				1, 20, // 1 - 20 parallel pings max
				60L, TimeUnit.SECONDS,
				new SynchronousQueue<Runnable>(),
				new ThreadPoolExecutor.DiscardPolicy() // Replace running pings if there's too many
		);
	}


	public void setAllowedUids(@NonNull int[] allowedUids) {
		if (allowedUids != null) {
			int[] uids = allowedUids.clone();

			int temp;

			for (int i = 0; i < uids.length; i++) {

				for (int y = i; y < uids.length; y++) {

					if (uids[i] > uids[y]) {
						temp = uids[i];
						uids[i] = uids[y];
						uids[y] = temp;
					}
				}
			}
			this.allowedUids = uids;
		}
	}

	public void setBlockedAddress(String[] address) {
		if (address != null)
			this.blockedAddress = address;
	}

	public void allowUnknownUid(boolean allow) {
		this.allowUnknownUid = allow;
	}

	private boolean handleUDPPacket(ByteBuffer clientPacketData, IPv4Header ipHeader, UDPHeader udpheader) {
		SessionManager sessionManager = SessionManager.getInstance();

		String srcAddress = intToIPAddress(ipHeader.getSourceIP());
		String destAddress = intToIPAddress(ipHeader.getDestinationIP());

		if (isBlockedUid(udpheader.getUid()) || isBlockedAddress(srcAddress) || isBlockedAddress(destAddress))
			return false;

		Session session = sessionManager.getSession(ipHeader.getDestinationIP(), udpheader.getDestinationPort(),
				ipHeader.getSourceIP(), udpheader.getSourcePort());

		if(session == null){
			session = sessionManager.createNewUDPSession(ipHeader.getDestinationIP(), udpheader.getDestinationPort(),
					ipHeader.getSourceIP(), udpheader.getSourcePort());
		}

		if(session == null) {
			return false;
		}

		session.setLastIpHeader(ipHeader);
		session.setLastUdpHeader(udpheader);
		int len = sessionManager.addClientData(clientPacketData, session);
		session.setDataForSendingReady(true);
		Log.d(TAG,"added UDP data for bg worker to send: "+len);
		sessionManager.keepSessionAlive(session);

		return true;
	}

	private boolean handleTCPPacket(ByteBuffer clientPacketData, IPv4Header ipHeader, TCPHeader tcpheader) {
		int dataLength = clientPacketData.limit() - clientPacketData.position();
		int sourceIP = ipHeader.getSourceIP();
		int destinationIP = ipHeader.getDestinationIP();
		int sourcePort = tcpheader.getSourcePort();
		int destinationPort = tcpheader.getDestinationPort();

		if (isBlockedUid(tcpheader.getUid()) ||
				isBlockedAddress(intToIPAddress(sourceIP)) ||
				isBlockedAddress(intToIPAddress(destinationIP))) {
			return false;
		}

		SessionManager sessionManager = SessionManager.getInstance();

		if(tcpheader.isSYN()) {
			//3-way handshake + create new session
			//set windows size and scale, set reply time in options
			replySynAck(ipHeader,tcpheader);
		} else if(tcpheader.isACK()) {
			String key = sessionManager.createKey(destinationIP, destinationPort, sourceIP, sourcePort);
			Session session = sessionManager.getSessionByKey(key);

			if(session == null) {
				if (tcpheader.isFIN()) {
					sendLastAck(ipHeader, tcpheader);
				} else if (!tcpheader.isRST()) {
					sendRstPacket(ipHeader, tcpheader, dataLength);
				}
				else {
					Log.e(TAG,"**** ==> Session not found: " + key);
				}
				return false;
			}

			session.setLastIpHeader(ipHeader);
			session.setLastTcpHeader(tcpheader);

			//any data from client?
			if(dataLength > 0) {
				//accumulate data from client
				if(session.getRecSequence() == 0 || tcpheader.getSequenceNumber() >= session.getRecSequence()) {
					int addedLength = sessionManager.addClientData(clientPacketData, session);
					//send ack to client only if new data was added
					sendAck(ipHeader, tcpheader, addedLength, session);
				} else {
					sendAckForDisorder(ipHeader, tcpheader, dataLength);
				}
			} else {
				//an ack from client for previously sent data
				acceptAck(tcpheader, session);

				if(session.isClosingConnection()){
					sendFinAck(ipHeader, tcpheader, session);
				}else if(session.isAckedToFin() && !tcpheader.isFIN()){
					//the last ACK from client after FIN-ACK flag was sent
					sessionManager.closeSession(destinationIP, destinationPort, sourceIP, sourcePort);
					Log.d(TAG,"got last ACK after FIN, session is now closed.");
				}
			}
			//received the last segment of data from vpn client
			if(tcpheader.isPSH()){
				//push data to destination here. Background thread will receive data and fill session's buffer.
				//Background thread will send packet to client
				pushDataToDestination(session, tcpheader);
			} else if(tcpheader.isFIN()){
				//fin from vpn client is the last packet
				//ack it
				Log.d(TAG,"FIN from vpn client, will ack it.");
				ackFinAck(ipHeader, tcpheader, session);
			} else if(tcpheader.isRST()){
				resetConnection(ipHeader, tcpheader);
			}

			if(!session.isClientWindowFull() && !session.isAbortingConnection()){
				sessionManager.keepSessionAlive(session);
			}
		} else if(tcpheader.isFIN()){
			//case client sent FIN without ACK
			Session session = sessionManager.getSession(destinationIP, destinationPort, sourceIP, sourcePort);
			if(session == null)
				ackFinAck(ipHeader, tcpheader, null);
			else
				sessionManager.keepSessionAlive(session);

		} else if(tcpheader.isRST()){
			resetConnection(ipHeader, tcpheader);
		} else {
			Log.d(TAG,"unknown TCP flag");
			String str1 = getOutput(ipHeader, tcpheader, clientPacketData.array());
			Log.d(TAG,">>>>>>>> Received from client <<<<<<<<<<");
			Log.d(TAG,str1);
			Log.d(TAG,">>>>>>>>>>>>>>>>>>>end receiving from client>>>>>>>>>>>>>>>>>>>>>");
		}
		return true;
	}

	private void handleICMPPacket(ByteBuffer clientPacketData, final IPv4Header ipHeader) throws PacketHeaderException {
		final ICMPPacket requestPacket = ICMPPacketFactory.parseICMPPacket(clientPacketData);
		Log.d(TAG, "Got an ICMP ping packet, type " + requestPacket.toString());

		pingThreadpool.execute(new Runnable() {
			@Override
			public void run() {
				try {
					if (!isReachable(intToIPAddress(ipHeader.getDestinationIP()))) {
						Log.d(TAG, "Failed ping, ignoring");
						return;
					}

					ICMPPacket response = ICMPPacketFactory.buildSuccessPacket(requestPacket);

					// Flip the address
					int destination = ipHeader.getDestinationIP();
					int source = ipHeader.getSourceIP();
					ipHeader.setSourceIP(destination);
					ipHeader.setDestinationIP(source);

					byte[] responseData = ICMPPacketFactory.packetToBuffer(ipHeader, response);

					Log.d(TAG, "Successful ping response");
					writer.write(responseData);
				} catch (IOException | PacketHeaderException e) {
					Log.w(TAG, "Handling ICMP failed with " + e.getMessage());
				}
			}

			private boolean isReachable(String ipAddress) {
				try {
					return InetAddress.getByName(ipAddress).isReachable(10000);
				} catch (IOException e) {
					return false;
				}
			}
		});
	}

	/**
	 * handle each packet from each vpn client
	 * @param stream ByteBuffer to be read
	 */
	@Nullable
	public Packet handlePacket(@NonNull ByteBuffer stream) throws PacketHeaderException {
		final byte[] rawPacket = new byte[stream.limit()];
		stream.get(rawPacket, 0, stream.limit());
		packetData.addData(rawPacket);
		stream.rewind();

		final IPv4Header ipHeader = IPPacketFactory.createIPv4Header(stream);

		final ITransportHeader transportHeader;
		switch (ipHeader.getProtocol()) {
			case PROTOCOL_TCP:
				transportHeader = TCPPacketFactory.createTCPHeader(stream);
				((TCPHeader) transportHeader).setUid(getConnectionOwnerUid(
						connectivityManager,
						ipHeader.getIpVersion(),
						PROTOCOL_TCP,
						intToIPAddress(ipHeader.getSourceIP()),
						transportHeader.getSourcePort(),
						intToIPAddress(ipHeader.getDestinationIP()),
						transportHeader.getDestinationPort()
				));
				break;
			case PROTOCOL_UDP:
				transportHeader = UDPPacketFactory.createUDPHeader(stream);
				((UDPHeader) transportHeader).setUid(getConnectionOwnerUid(
						connectivityManager,
						ipHeader.getIpVersion(),
						PROTOCOL_UDP,
						intToIPAddress(ipHeader.getSourceIP()),
						transportHeader.getSourcePort(),
						intToIPAddress(ipHeader.getDestinationIP()),
						transportHeader.getDestinationPort()
				));
				break;
			case PROTOCOL_ICMP:
				handleICMPPacket(stream, ipHeader);
				return null;
			default:
				Log.e(TAG, "******===> Unsupported protocol: " + ipHeader.getProtocol());
				return null;
		}

		boolean handled;
		if (transportHeader instanceof TCPHeader) {
			handled = handleTCPPacket(stream, ipHeader, (TCPHeader) transportHeader);
		} else {
			handled = handleUDPPacket(stream, ipHeader, (UDPHeader) transportHeader);
		}

		return new Packet(ipHeader, transportHeader, stream.array(), handled);
	}

	private void sendRstPacket(IPv4Header ip, TCPHeader tcp, int dataLength){
		byte[] data = TCPPacketFactory.createRstData(ip, tcp, dataLength);
		try {
			writer.write(data);
			packetData.addData(data);
			Log.d(TAG,"Sent RST Packet to client with dest => " +
					intToIPAddress(ip.getDestinationIP()) + ":" +
					tcp.getDestinationPort());
		} catch (IOException e) {
			Log.e(TAG,"failed to send RST packet: " + e.getMessage());
		}
	}

	private void sendLastAck(IPv4Header ip, TCPHeader tcp){
		byte[] data = TCPPacketFactory.createResponseAckData(ip, tcp, tcp.getSequenceNumber()+1);
		try {
			writer.write(data);
			packetData.addData(data);
			Log.d(TAG,"Sent last ACK Packet to client with dest => " +
					intToIPAddress(ip.getDestinationIP()) + ":" +
					tcp.getDestinationPort());
		} catch (IOException e) {
			Log.e(TAG,"failed to send last ACK packet: " + e.getMessage());
		}
	}

	private void ackFinAck(IPv4Header ip, TCPHeader tcp, Session session){
		long ack = tcp.getSequenceNumber() + 1;
		long seq = tcp.getAckNumber();
		byte[] data = TCPPacketFactory.createFinAckData(ip, tcp, ack, seq, true, true);
		try {
			writer.write(data);
			packetData.addData(data);
			if(session != null){
				session.getSelectionKey().cancel();
				SessionManager.getInstance().closeSession(session);
				Log.d(TAG,"ACK to client's FIN and close session => "+ intToIPAddress(ip.getDestinationIP())+":"+tcp.getDestinationPort()
						+ "-" + intToIPAddress(ip.getSourceIP())+":"+tcp.getSourcePort());
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void sendFinAck(@NonNull IPv4Header ip, @NonNull TCPHeader tcp, @NonNull Session session){
		final long ack = tcp.getSequenceNumber();
		final long seq = tcp.getAckNumber();
		final byte[] data = TCPPacketFactory.createFinAckData(ip, tcp, ack, seq,true,false);
		final ByteBuffer stream = ByteBuffer.wrap(data);
		try {
			writer.write(data);
			packetData.addData(data);
			Log.d(TAG,"00000000000 FIN-ACK packet data to vpn client 000000000000");
			IPv4Header vpnip = null;
			try {
				vpnip = IPPacketFactory.createIPv4Header(stream);
			} catch (PacketHeaderException e) {
				e.printStackTrace();
			}

			TCPHeader vpntcp = null;
			try {
				if (vpnip != null)
					vpntcp = TCPPacketFactory.createTCPHeader(stream);
			} catch (PacketHeaderException e) {
				e.printStackTrace();
			}

			if(vpnip != null && vpntcp != null){
				String sout = getOutput(vpnip, vpntcp, data);
				Log.d(TAG,sout);
			}
			Log.d(TAG,"0000000000000 finished sending FIN-ACK packet to vpn client 000000000000");

		} catch (IOException e) {
			Log.e(TAG,"Failed to send ACK packet: "+e.getMessage());
		}
		session.setSendNext(seq + 1);
		//avoid re-sending it, from here client should take care the rest
		session.setClosingConnection(false);
	}

	private void pushDataToDestination(Session session, TCPHeader tcp){
		session.setDataForSendingReady(true);
		session.setTimestampReplyto(tcp.getTimeStampSender());
		session.setTimestampSender((int) System.currentTimeMillis());

		Log.d(TAG,"set data ready for sending to dest, bg will do it. data size: "
                + session.getSendingDataSize());
	}
	
	/**
	 * send acknowledgment packet to VPN client
	 * @param ipheader IP Header
	 * @param tcpheader TCP Header
	 * @param acceptedDataLength Data Length
	 * @param session Session
	 */
	private void sendAck(IPv4Header ipheader, TCPHeader tcpheader, int acceptedDataLength, Session session){
		long acknumber = session.getRecSequence() + acceptedDataLength;
		Log.d(TAG,"sent ack, ack# "+session.getRecSequence()+" + "+acceptedDataLength+" = "+acknumber);
		session.setRecSequence(acknumber);
		byte[] data = TCPPacketFactory.createResponseAckData(ipheader, tcpheader, acknumber);
		try {
			writer.write(data);
			packetData.addData(data);
		} catch (IOException e) {
			Log.e(TAG,"Failed to send ACK packet: " + e.getMessage());
		}
	}

	private void sendAckForDisorder(IPv4Header ipHeader, TCPHeader tcpheader, int acceptedDataLength) {
		long ackNumber = tcpheader.getSequenceNumber() + acceptedDataLength;
		Log.d(TAG,"sent ack, ack# " + tcpheader.getSequenceNumber() +
				" + " + acceptedDataLength + " = " + ackNumber);
		byte[] data = TCPPacketFactory.createResponseAckData(ipHeader, tcpheader, ackNumber);
		try {
			writer.write(data);
			packetData.addData(data);
		} catch (IOException e) {
			Log.e(TAG,"Failed to send ACK packet: " + e.getMessage());
		}
	}

	/**
	 * acknowledge a packet and adjust the receiving window to avoid congestion.
	 * @param tcpHeader TCP Header
	 * @param session Session
	 */
	private void acceptAck(TCPHeader tcpHeader, Session session){
		boolean isCorrupted = isPacketCorrupted(tcpHeader);
		session.setPacketCorrupted(isCorrupted);
		if(isCorrupted){
			Log.e(TAG,"prev packet was corrupted, last ack# " + tcpHeader.getAckNumber());
		}
		if(tcpHeader.getAckNumber() > session.getSendUnack() ||
				tcpHeader.getAckNumber() == session.getSendNext()){
			session.setAcked(true);
			//Log.d(TAG,"Accepted ack from client, ack# "+tcpheader.getAckNumber());
			
			if(tcpHeader.getWindowSize() > 0){
				session.setSendWindowSizeAndScale(tcpHeader.getWindowSize(), session.getSendWindowScale());
			}
			session.setSendUnack(tcpHeader.getAckNumber());
			session.setRecSequence(tcpHeader.getSequenceNumber());
			session.setTimestampReplyto(tcpHeader.getTimeStampSender());
			session.setTimestampSender((int) System.currentTimeMillis());
		} else {
			Log.d(TAG,"Not Accepting ack# "+tcpHeader.getAckNumber() +" , it should be: "+session.getSendNext());
			Log.d(TAG,"Prev sendUnack: "+session.getSendUnack());
			session.setAcked(false);
		}
	}

	/**
	 * set connection as aborting so that background worker will close it.
	 * @param ip IP
	 * @param tcp TCP
	 */
	private void resetConnection(IPv4Header ip, TCPHeader tcp){
		Session session = SessionManager.getInstance().getSession(ip.getDestinationIP(), tcp.getDestinationPort(),
				ip.getSourceIP(), tcp.getSourcePort());
		if(session != null){
			session.setAbortingConnection(true);
		}
	}

	/**
	 * create a new client's session and SYN-ACK packet data to respond to client
	 * @param ip IP
	 * @param tcp TCP
	 */
	private void replySynAck(IPv4Header ip, TCPHeader tcp) {
		ip.setIdentification(0);
		Packet packet = TCPPacketFactory.createSynAckPacketData(ip, tcp);
		
		TCPHeader tcpheader = (TCPHeader) packet.getTransportHeader();
		
		Session session = SessionManager.getInstance().createNewSession(ip.getDestinationIP(),
				tcp.getDestinationPort(), ip.getSourceIP(), tcp.getSourcePort());
		if(session == null)
			return;
		
		int windowScaleFactor = (int) Math.pow(2, tcpheader.getWindowScale());
		//Log.d(TAG,"window scale: Math.power(2,"+tcpheader.getWindowScale()+") is "+windowScaleFactor);
		session.setSendWindowSizeAndScale(tcpheader.getWindowSize(), windowScaleFactor);
		Log.d(TAG,"send-window size: " + session.getSendWindow());
		session.setMaxSegmentSize(tcpheader.getMaxSegmentSize());
		session.setSendUnack(tcpheader.getSequenceNumber());
		session.setSendNext(tcpheader.getSequenceNumber() + 1);
		//client initial sequence has been incremented by 1 and set to ack
		session.setRecSequence(tcpheader.getAckNumber());

		try {
			writer.write(packet.getBuffer());
			packetData.addData(packet.getBuffer());
			Log.d(TAG,"Send SYN-ACK to client");
		} catch (IOException e) {
			Log.e(TAG,"Error sending data to client: " + e.getMessage());
		}
	}

	private boolean isBlockedUid(int uid) {
		if (allowUnknownUid && uid == 0 || uid == -1)
			return false;

		int begin = 0;
		int end = allowedUids.length;

		while (begin < end) {
			int middle = (begin + end) / 2;

			if (allowedUids[middle] == uid)
				return false;
			if (allowedUids[middle] > uid)
				end = middle - 1;
			else if (allowedUids[middle] < uid)
				begin = middle + 1;
		}

		return true;
	}

	private boolean isBlockedAddress(String address) {
		for (String ba : blockedAddress) {
			if (ba.equals(address))
				return true;
		}
		return false;
	}
}//end class
