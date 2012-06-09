package net.smartsocket.protocols;

import net.smartsocket.Logger;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class RemoteCall
{
	/**
	 * The core container of the RemoteCall object.
	 */
	public transient JsonObject properties = new JsonObject();

	/**
	 * Instantiate a new RemoteCall object for calling a method on a client.
	 * @param method The String name of the method to be called on the client.
	 * @param directTo The String name of the SmartSocketClient dataListener to direct this message to.
	 */
	public RemoteCall( String method, String directTo ) {
		properties.addProperty( "method", method );
		properties.addProperty( "directTo", directTo );
	}

	/**
	 * Instantiate a new RemoteCall object for calling a method on a client.
	 * @param method
	 */
	public RemoteCall( String method ) {
		properties.addProperty( "method", method );
	}

	/**
	 * Create or modify a property on the client call.
	 * @param key The key name of the property
	 * @param value The value of the property
	 * @return  
	 */
	public RemoteCall put( String key, String value ) {
		properties.addProperty( key, value );
		return this;
	}

	/**
	 * Create or modify a property on the client call.
	 * @param key
	 * @param value
	 * @return
	 */
	public RemoteCall put( String key, Boolean value ) {
		properties.addProperty( key, value );
		return this;
	}

	/**
	 * Create or modify a property on the client call.
	 * @param key
	 * @param value
	 * @return
	 */
	public RemoteCall put( String key, Number value ) {
		properties.addProperty( key, value );
		return this;
	}

	/**
	 * Create or modify a property on the client call.
	 * @param key
	 * @param value
	 * @return
	 */
	public RemoteCall put( String key, Character value ) {
		properties.addProperty( key, value );
		return this;
	}

	/**
	 * Create or modify a property on the client call.
	 * @param key The key name of the property
	 * @param value The value of the property
	 * @return  
	 */
	public RemoteCall put( String key, JsonElement value ) {

		try {
			properties.add( key, value );
		} catch (Exception e) {
			Logger.log( "Having problems creating Call: " + key + " - " + e.getMessage() );
		}

		return this;
	}
}
