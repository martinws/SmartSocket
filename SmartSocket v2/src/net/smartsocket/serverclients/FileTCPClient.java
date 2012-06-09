package net.smartsocket.serverclients;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.Socket;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

import net.smartsocket.Config;
import net.smartsocket.Logger;
import net.smartsocket.forms.StatisticsTracker;
import net.smartsocket.protocols.binary.RemoteFileCall;
import net.smartsocket.serverextensions.TCPExtension;
import net.smartsocket.smartlobby.User;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

public class FileTCPClient extends TCPClient
{
	/**
	 * Creates a new thread for a client on a TCPExtension object.
	 * @param client
	 * @param extension
	 */
	public FileTCPClient( Socket client, TCPExtension extension ) {
		super(client, extension);
	}
	
	/**
	 * Infinite read loop until the client closes their connections
	 */
	protected void read() {
		String input = null;
		byte[] bytes = null;
		int red;
		try {
			while ( (red = in.read()) != -1 ) {
				Logger.log( "Client input stream open." );

				long fileLength = getFileLength();
				byte[] fileBytes = getFileBytes( fileLength );
				String jsonHeader = readJsonHeader();

				Logger.log( "Server message: " + fileLength + " / " + jsonHeader );

				process( jsonHeader, fileBytes );
			}
			Logger.log( "Client input stream closed." + red );
		} catch (Exception e) {
			Logger.log( "Client " + Thread.currentThread().getId() + " disconnected." + e.getMessage() );
			e.printStackTrace();
		}
	}

	public long getFileLength() {
		
		try {
			return in.readLong();
		} catch (Exception e) {
		}
		
		return 0;
	}

	protected byte[] getFileBytes( long fileLength ) throws IOException {
		byte[] fileBytes = new byte[(int) fileLength];

		for ( int i = 0; i < fileLength; i++ ) {
			fileBytes[i] = in.readByte();
		}

		return fileBytes;
	}

	/**
	 * The method processes incoming data from the client, and routes them to the proper methods on the server extension.
	 * @param line
	 */
	private void process( String line, byte[] fileBytes ) {
		//# Add the size of this line of text to our inboundByte variable for gui usage
		setInboundBytes( getInboundBytes() + line.getBytes().length + fileBytes.length );
		Logger.log( "Client " + Thread.currentThread().getId() + " says: " + line );

		//# Reflection
		Method m = null;

		//# Setup method and params
		String methodName = null;
		JsonObject params = null;

		try {
			//# Get the particulars of the JSON call from the client
			params = (JsonObject) new JsonParser().parse( line );
			methodName = params.get( "method" ).getAsString();

			if ( fileBytes.length == 0 ) {
				//# Get ready to create dynamic method call to extension
				Class[] classes = new Class[2];
				classes[0] = TCPClient.class;
				classes[1] = JsonObject.class;
				
				//# First let's send this message to the extensions onDataSpecial to see if
				//# the extension wants to process this message in its own special way.
				if ( _extension.onDataSpecial( this, methodName, params ) == false ) {

					//# Try to call the method on the desired extension class
					//# This is only executed if onDataSpecial returns false on our extension.
					Object[] o = { this, params };
					m = _extension.getExtension().getMethod( methodName, classes );
					m.invoke( _extension.getExtensionInstance(), o );
				}
			} else {
				//# We are here becuase we are also trying to pass a file to a method on our extension for writing.
				//# Get ready to create dynamic method call to extension
				Class[] classes = new Class[3];
				classes[0] = TCPClient.class;
				classes[1] = JsonObject.class;
				classes[2] = byte[].class;
				
				//# First let's send this message to the extensions onDataSpecial to see if
				//# the extension wants to process this message in its own special way.
				if ( _extension.onDataSpecial( this, methodName, params ) == false ) {

					//# Try to call the method on the desired extension class
					//# This is only executed if onDataSpecial returns false on our extension.
					Object[] o = { this, params, fileBytes };
					m = _extension.getExtension().getMethod( methodName, classes );
					m.invoke( _extension.getExtensionInstance(), o );
				}
			}

		} catch (JsonParseException e) {
			Logger.log( "[" + _extension.getExtensionName() + "] Client has tried to pass invalid JSON" );
		} catch (NoSuchMethodException e) {
			Logger.log( "[" + _extension.getExtensionName() + "] The method: " + methodName + " does not exist" );
		} catch (IllegalAccessException e) {
			Logger.log( "[" + _extension.getExtensionName() + "] The method: " + methodName + " is not accessible from this scope." );
		} catch (InvocationTargetException e) {
			Logger.log( "[" + _extension.getExtensionName() + "] The method: \'" + methodName + "\' reports: "
					+ e.getTargetException().getMessage() + " in JSONObject string: " + params.toString() );
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * The RemoteCall message to send to this client. This send method is capable of sending files through
	 * the socket as well. This send method is not compatible with clients using the ActionScript 3.0 SmartSocket Client API
	 * @param message
	 * @see net.smartsocket.protocols.binary.RemoteFileCall
	 */
	public void send( net.smartsocket.protocols.binary.RemoteFileCall call ) {
		System.out.println( "OUTGOING: " + call.properties.toString() );
		try {
			byte[] fileBytes = new byte[0];

			if ( call.file != null ) {
				long fileLength = call.file.length();
				fileBytes = new byte[(int) fileLength];

				call.properties.addProperty( "fileSize", fileLength );

				FileInputStream fileInputStream = new FileInputStream( call.file );
				fileInputStream.read( fileBytes );
			}

			out.write( 0 );
			out.writeLong( fileBytes.length );			
			
			if(fileBytes.length != 0) {
				out.write( fileBytes );
			}
			
			out.writeUTF( call.properties.toString() );
			
			out.flush();
		} catch (Exception e) {
			System.err.println( "Write error (" + e + "): " + call );
		}
	}

	/**
	 * Send a RemoteCall to a selected list of TCPClients
	 * @param userList
	 * @param message 
	 */
	public static void send( Map<String, User> userList, RemoteFileCall message ) {
		for ( Map.Entry<String, User> user : userList.entrySet() ) {
			user.getValue().getTcpClient().send( message );
		}
	}
}
