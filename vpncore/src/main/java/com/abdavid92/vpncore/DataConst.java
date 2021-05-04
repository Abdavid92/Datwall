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

/**
 * @author Borey Sao
 * Date: June 2, 2014
 */
public class DataConst {
	public static final int MAX_RECEIVE_BUFFER_SIZE = 65535;
	public static final int PROTOCOL_TCP = 6;
	public static final int PROTOCOL_UDP = 17;
	public static final int PROTOCOL_ICMP = 1;
	public static final int PROTOCOL_ICMP_V6 = 58;
	/**
	 * Máximo tamaño de los paquetes transmitidos por
	 * el vpn.
	 * */
	public static int MAX_PACKET_LENGTH = 1500;
}
