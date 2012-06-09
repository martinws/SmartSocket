package net.smartsocket.serverextensions;

import java.util.ArrayList;
import java.util.Map;

import net.smartsocket.serverclients.TCPClient;

import com.google.gson.JsonObject;

/**
 * The BinaryTCPExtension class is an abstract class that provides the shell around which all extensions with extra binary data 
 * using the TCP protocol will use. This class handles setting up the actual server, creating the initial console gui, extension tabs on the gui,
 * as well as accepting connections and creating separate thread for each client (TCPClient).
 * @author martinws
 */

public abstract class BinaryTCPExtension extends TCPExtension
{

	public BinaryTCPExtension( int port ) {
		super(port);
	}

	public abstract boolean onDataSpecial( TCPClient client, String methodName, JsonObject params, ArrayList<Map<String, Object>> binaryData );
}
