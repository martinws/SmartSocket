package net.smartsocket.protocols.json;

import net.smartsocket.Logger;
import com.google.gson.Gson;
import com.google.gson.JsonElement;

public class RemoteJSONCall extends net.smartsocket.protocols.RemoteCall
{
	protected static transient Gson gson = new Gson();

	public RemoteJSONCall(String method)
	{
		super(method);
	}

	public RemoteJSONCall( String method, String directTo ) {
		super(method, directTo);
	}
	
	/**
	 * Instantiate a new RemoteCall object for calling a method on a client.
	 * @param method The String name of the method to be called on the client.
	 * @param directTo The JsonElement of the SmartSocketClient dataListener to direct this message to.
	 */
	public RemoteJSONCall( String method, JsonElement directTo ) 
	{
		super(method);
		properties.addProperty( "directTo", directTo.getAsString() );
	}
	
	/**
	 * Create or modify a property on the client call.
	 * @param key The key name of the property
	 * @param value The value of the property
	 * @return RemoteCall
	 */
	public RemoteJSONCall put( String key, JsonElement value ) 
	{
		try {
			properties.add( key, value );
		} catch (Exception e) {
			Logger.log( "Having problems creating Call: " + key + " - " + e.getMessage() );
		}

		return this;
	}
	
	
	/**
	 * Serialize any object into a proper JSON Object
	 * @param obj
	 * @return
	 */
	public static JsonElement serialize( Object obj ) {
		return gson.toJsonTree( obj );
	}
	
}
