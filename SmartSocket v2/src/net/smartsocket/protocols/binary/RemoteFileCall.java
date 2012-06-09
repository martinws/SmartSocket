package net.smartsocket.protocols.binary;

import java.io.File;
import java.io.FileInputStream;

import net.smartsocket.Logger;

import com.google.gson.JsonElement;

/**
 * The RemoteCall class is a simple way to construct a JSON formatted message to send to a client This call is capable of sending files through the socket.<br/><br/>
 * <b>Usage:</b><br/>
 *<code>
 *RemoteCall call = new RemoteCall("<b>methodNameOnClientApplication</b>");<br/>
 *call.put("<b>propertyName</b>", "<b>propertyValue</b>");<br/>
 *client.send(call);<br/>
 *</code>
 * @author XaeroDegreaz
 */
public class RemoteFileCall extends net.smartsocket.protocols.json.RemoteJSONCall {

	/**
	 * An optional file to send through the socket.
	 */
	public transient File file;
	
	public RemoteFileCall(String method)
	{
		super(method);
	}

	/**
	 * Create or modify a property on the client call.
	 * @param key
	 * @param value
	 * @return RemoteCall
	 */
	public RemoteFileCall put( String key, Boolean value ) {
		properties.addProperty( key, value );
		return this;
	}

	/**
	 * Create or modify a property on the client call.
	 * @param key
	 * @param value
	 * @return RemoteCall
	 */
	public RemoteFileCall put( String key, Number value ) {
		properties.addProperty( key, value );
		return this;
	}
 
	/**
	 * Create or modify a property on the client call.
	 * @param key
	 * @param value
	 * @return RemoteCall
	 */
	public RemoteFileCall put( String key, Character value ) {
		properties.addProperty( key, value );
		return this;
	}

	/**
	 * Create or modify a property on the client call.
	 * @param key The key name of the property
	 * @param value The value of the property
	 * @return RemoteCall
	 */
	public RemoteFileCall put( String key, JsonElement value ) {

		try {
			properties.add( key, value );
		} catch (Exception e) {
			Logger.log( "Having problems creating Call: " + key + " - " + e.getMessage() );
		}

		return this;
	}
	
	/**
	 * Add a file to be sent along with the socket transmission.
	 * @param file The file to send
	 * @return RemoteCall
	 */
	public RemoteFileCall put(File file) {
		this.file = file;
		return this;
	}

	public Object[] toByteArray() {
		long fileSize = file.length();
		byte[] fileBytes = new byte[ (int)fileSize ];
		FileInputStream fis;
		
		try {
			fis = new FileInputStream( file );
			fis.read( fileBytes );
		} catch (Exception e) {
			Logger.log( e.getMessage() );
		}		
		
		return new Object[]{fileSize, fileBytes, properties.toString() };
	}
}
