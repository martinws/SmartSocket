package net.smartsocket.serverclients;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import net.smartsocket.Logger;
import net.smartsocket.protocols.RemoteCall;
import net.smartsocket.protocols.binary.RemoteBinaryCall;
import net.smartsocket.protocols.json.RemoteJSONCall;
import net.smartsocket.serverextensions.BinaryTCPExtension;
import net.smartsocket.smartlobby.User;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

public class BinaryTCPClient extends TCPClient
{
	public static String kPOSITION_OF_BINARY_DATA = "positionofbinarydata";
	public static String kDATA = "data";
	public static String kNAME = "name";
	public static String kDESCRIPTION = "description";
	
	// Messages are split into 65535 byte packets to enable better mobile processing.
	//
	// Message Format is:
	//
	// PacketType - 8bit byte.
	// Packet Size - 32 bit int (currently using 16 bits, can be increased in the future.).
	// If FULL_PACKET OR INITIAL_PART_PACKET
	// 		MessageType - 8 bit byte
	// 		-- Message (See Description in MessageType)
	// ELSE 
	// 		Continuing of Message broken into 65535 bytes.
	//
	// For JSON only messages:
	// 32-bit int - JSON Message Length - UTF8 compliant.
	// UTF String containing:
	// 		{ minimum: "method", "directTo", "Optional Return Response - returnListener", "Optional Return Response - returnMethod"
	// 
	// For Binary messages:
	// 32-bit int JSON Message Length  - UTF8 compliant 
	// UTF Message - JSON Definition { minimum: "directTo" - String, "method" - String, 
	//								   "Optional Return Response - returnListener", "Optional Return Response - returnMethod"
	//								   "data" - Array of Dictionary { "name", "description", "dataPosition - transient in array"} }" +
	// Then for each data element
	// 32-bit int Data Length    
	// Data in Bytes
	// ... Repeat to Number Data Elements.
	//
	public enum PacketType {
		NOT_SET(0),
		FULL_PACKET(91),					// Full String.
		INITIAL_PART_PACKET(2),			// Partial String initialisation.
		CONTINUE_PARTIAL_PACKET(3),		// Partial String continued.
		LAST_PART_PACKET(4);			// Last Partial String
		
		private final byte type;  
		  
		PacketType(int aStatus) {  
            this.type = (byte) aStatus;  
        }  
        public byte type() {  
            return this.type;  
        }  
        
        public static PacketType fromInteger(int x) {
            switch(x) {
            	case 0: 
            		return NOT_SET;
            	case 91:
                	return FULL_PACKET;
            	case 2:
                	return INITIAL_PART_PACKET;
            	case 3: 
            		return CONTINUE_PARTIAL_PACKET;
            	case 4:
            		return LAST_PART_PACKET;
            }	
            
            return null;
        }
	}
	
	public enum MessageType {
		NOT_SET(0),					// Not Set... Throw error.
		JSON_MESSAGE(1),			// Basic JSON Message
									// Format:
									// Type - Message Type byte
									// 32-bit int - JSON Message Length - UTF8 compliant.
									// { minimum: "method", "directTo", "Optional Return Response - returnListener", "Optional Return Response - returnMethod"
		BINARY_MESSAGE(2);			// Binary Message. 
									// Format of Binary Message:
									// Type - byte
									// 32-bit int JSON Message Length  - UTF8 compliant 
									// UTF Message - JSON Definition { minimum: "directTo" - String, "method" - String, "numberDataObjects" - 16 bit int, "data" - Array of Dictionary { "name", "description", "dataPosition - transient in array"} }" +
									// Then for each data element
									// 32-bit int Data Length    
									// Data in Bytes
									// ... Repeat to Number Data Elements.
	
		private final int type;  
		  
		MessageType(int aStatus) {  
            this.type = aStatus;  
        }  
        public int type() {  
            return this.type;  
        }  
        
        public static MessageType fromInteger(int x) {
            switch(x) {
            	case 0: 
            		return NOT_SET;
            	case 1:
            		return JSON_MESSAGE;
            	case 2:
            		return BINARY_MESSAGE;
            }
            
            return null;
        }
	}
	

	/**
	 * Creates a new thread for a client on a TCPExtension object.
	 * @param client
	 * @param extension
	 */
	public BinaryTCPClient( Socket client, BinaryTCPExtension extension ) {
		super( client, extension );
	}

	
	/**
	 * Infinite read loop until the client closes their connections. 
	 * Expects a File Length and File before the JSON is delivered.
	 */
	protected void read() {
		int packetType;
		try 
		{
			MessageType msgType = MessageType.NOT_SET;
			PacketType previousPacket = PacketType.NOT_SET;
			
			ByteArrayOutputStream currentBuffer = null;
			
			// Messages are broken down into chunks no greater than 65535 bytes.
			//
			while ( (packetType = in.read()) != -1 ) 
			{
				Logger.log( "Client input stream open." );

				boolean badMessage = false;
				
				// Read the length of this message.
				//
				int messageLength = in.readInt();
				
				PacketType packet = PacketType.fromInteger(packetType); 
				if ( packet == PacketType.FULL_PACKET || packet == PacketType.INITIAL_PART_PACKET )
				{
					if ( previousPacket != PacketType.NOT_SET)
					{
						Logger.log( "BAD MESSAGE. Previous Message Bad. Ignoring Data "); 
						
						badMessage = true;
					}
					else 
					{					
						int messageType = in.read();
						msgType = MessageType.fromInteger(messageType);
						
						messageLength -= 1;  // Reduce the message length by 1 since we've read the first byte.
					}
				}
				
				byte[] inBytes = new byte[messageLength];
				in.read(inBytes, 0, messageLength );
				
				if ( badMessage == true )
				{
					previousPacket = PacketType.NOT_SET;
					msgType = MessageType.NOT_SET;
					currentBuffer = null;
					
					continue; // Ignore the message and move on to the next packet.
				}
				else 
				{
					if ( currentBuffer == null )
						currentBuffer = new ByteArrayOutputStream();
					
					currentBuffer.write(inBytes, 0, inBytes.length );
				}
				
				if ( packet == PacketType.LAST_PART_PACKET || packet == PacketType.FULL_PACKET )
				{
					ByteBuffer bufferToProcess = ByteBuffer.wrap( currentBuffer.toByteArray() );
					bufferToProcess.rewind(); // Start from the beginning.
					
					if ( msgType == MessageType.JSON_MESSAGE )
						processJSONMessage( bufferToProcess );
					else 
						processBinaryMessage( bufferToProcess );
					
					msgType = MessageType.NOT_SET;
					previousPacket = PacketType.NOT_SET;					
					currentBuffer = null;
				}
			}
			
			Logger.log( "Client input stream closed." + packetType );
		} 
		catch (Exception e) 
		{
			Logger.log( "Client " + Thread.currentThread().getId() + " disconnected." + e.getMessage() );
			e.printStackTrace();
		}
	}
	
	private ByteBuffer getSubByteBuffer(int length, ByteBuffer buffer)
	{
		ByteBuffer newBuffer = ByteBuffer.allocate(length);
		for ( int i = 0; i < length; i++ )
		{
			newBuffer.put( buffer.get() );
		}
		
		return newBuffer;
	}

	/**
	 * The method processes incoming data from the client, and routes them to the proper methods on the server extension.
	 * @param line
	 * @throws IOException 
	 */
	private void processJSONMessage( ByteBuffer byteBuffer ) throws IOException 
	{	
		byteBuffer.rewind();
		
		int jsonLength = byteBuffer.getInt();

		ByteBuffer jsonBuffer = getSubByteBuffer( jsonLength, byteBuffer );
		String line = new String( jsonBuffer.array(), "UTF-8");
		
		//# Add the size of this line of text to our inboundByte variable for gui usage
		setInboundBytes( getInboundBytes() + line.getBytes().length );
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

			//# Get ready to create dynamic method call to extension
			Class<?>[] classes = new Class[2];
			classes[0] = getClass();
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
	 * The method processes incoming data from the client, and routes them to the proper methods on the server extension.
	 * @param line
	 * @throws IOException 
	 */
	private void processBinaryMessage( ByteBuffer byteBuffer ) throws IOException 
	{
		int jsonLength = byteBuffer.getInt();

		ByteBuffer jsonBuffer = getSubByteBuffer( jsonLength, byteBuffer );
		String line = new String( jsonBuffer.array(), "UTF-8");
				
		//# Add the size of this line of text to our inboundByte variable for gui usage
		setInboundBytes( getInboundBytes() + line.getBytes().length );
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

			//# Get ready to create dynamic method call to extension
			Class<?>[] classes = new Class[3];
			classes[0] = BinaryTCPClient.class;
			classes[1] = JsonObject.class;
			classes[2] = null;
			
			// Now read the binary data.
			//
			JsonElement jsonElement = params.get(kDATA);
			ArrayList<Map<String,Object>> dataList = null;
			if ( jsonElement != null )
			{
				JsonArray metaBinaryDataArray = jsonElement.getAsJsonArray();

				dataList = new ArrayList<Map<String,Object>>();
				for ( int i = 0; i < metaBinaryDataArray.size(); i++ )
				{	
					int imageSize = byteBuffer.getInt();
					byte[] subBytes = new byte[imageSize];

					byteBuffer.get(subBytes, 0, imageSize);

					JsonObject associatedJsonObject = getMetaDataForBinaryData(i, params);
					HashMap<String,Object> metaData = new HashMap<String,Object>();

					metaData.put( kDATA, ByteBuffer.wrap(subBytes) );
					metaData.put( kNAME, associatedJsonObject.get(kNAME));
					metaData.put( kDESCRIPTION, associatedJsonObject.get(kDESCRIPTION) );
					dataList.add( metaData );
				}
			}
			
			//# First let's send this message to the extensions onDataSpecial to see if
			//# the extension wants to process this message in its own special way.
			if ( ( ( _extension instanceof BinaryTCPExtension ) && 
			     ((BinaryTCPExtension) _extension).onDataSpecial( this, methodName, params, dataList ) == false ) &&
			     _extension.onDataSpecial( this, methodName, params ) == false) 
			{
				Object[] outputParams;
				
				if ( dataList != null)
				{
					Object[] o = { this, params, dataList };
					outputParams = o;
					
					classes[2] = ArrayList.class;
				}
				else
				{
					Object[] o = { this, params };
					outputParams = o;	
					
					
				}
				
				//# Try to call the method on the desired extension class
				//# This is only executed if onDataSpecial returns false on our extension.
				m = _extension.getExtension().getMethod( methodName, classes );
				m.invoke( _extension.getExtensionInstance(), outputParams );
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
		} finally {
			
		}
	}
	
	/**
	 * 
	 * @param dataPosition
	 * @param params
	 * @return
	 * @throws exception if there is no data for defined position.
	 */
	private JsonObject getMetaDataForBinaryData(int dataPosition, JsonObject params)
	{
		JsonArray metaBinaryDataArray = params.get(kDATA).getAsJsonArray();
		
		for ( JsonElement element : metaBinaryDataArray )
		{
			if ( element.isJsonObject() == false )
			{
				throw new JsonParseException("Not a Json Object when reading the meta binary data array.");
			}
			
			JsonObject dataObject = element.getAsJsonObject();
			int position = dataObject.get(kPOSITION_OF_BINARY_DATA).getAsInt();
		
			if (dataPosition == position)
				return dataObject;
		}
		
		throw new JsonParseException("Json Object not found for dataPosition:" + dataPosition);
	}
	
	
	private static final int MAX_MSGBODY_SZ = 0xFFF0;
 	private static final int MAX_MSG_SZ = 0xFFFF;
 	
	public void send( RemoteCall call )
	{
		if ( call instanceof RemoteJSONCall )
			sendRemoteJSONCall( (RemoteJSONCall) call );
		else if ( call instanceof RemoteBinaryCall )
			sendRemoteBinaryCall( (RemoteBinaryCall) call );
		else
			super.send(call);
	}
	
	/**
	 * The RemoteCall message to send to this client. Break any message into sizeable chunks < the writeUTF limitations.
	 * 
	 * @param message
	 * @see net.smartsocket.protocols.binary.RemoteCall
	 */
	public void sendRemoteJSONCall( RemoteJSONCall call ) 
	{
		try 
		{			
			String properties = call.properties.toString();
			byte[] jsonBytes = properties.getBytes("UTF-8");
			
			ByteBuffer byteBuffer = ByteBuffer.allocate( jsonBytes.length + 5 ); // 1 for type, 4 for the 32 bit json length.
			byteBuffer.put( (byte) MessageType.JSON_MESSAGE.type() );
			byteBuffer.putInt( jsonBytes.length ); // Json length.
			byteBuffer.put( jsonBytes );

			byte[] sendBytes = byteBuffer.array();
			
			int sentBytes = 0; 
			while ( sentBytes < sendBytes.length )
			{
				ByteBuffer sendBuffer = ByteBuffer.allocate( MAX_MSG_SZ );

				int messageLength = MAX_MSGBODY_SZ;
				PacketType headerValue;
				if ( sendBytes.length < MAX_MSGBODY_SZ )
				{
					headerValue = PacketType.FULL_PACKET;
					messageLength = sendBytes.length;
				}
				else if ( sentBytes == 0 )
					headerValue = PacketType.INITIAL_PART_PACKET;
				else if ( messageLength < properties.length() - sentBytes )
					headerValue = PacketType.CONTINUE_PARTIAL_PACKET;
				else // ( stringLength > strLength - sentBytes )			
				{
					headerValue = PacketType.LAST_PART_PACKET;
					messageLength = sendBytes.length - sentBytes;
				}
				
				byte type = (byte) headerValue.type();
				sendBuffer.put(type);
				sendBuffer.putInt(messageLength);
				sendBuffer.put(Arrays.copyOfRange(sendBytes, sentBytes, messageLength ) );	

				int size = sendBuffer.position();
				sendBuffer.rewind();
				
				byte[] sendArray = new byte[size];
				sendBuffer.get(sendArray, 0, size);

//				out.writeByte( type );				// Message Packet Type.
//				out.writeInt(messageLength);		// Message Packet Length.
				
//				byte[] subBytes = Arrays.copyOfRange(sendBytes, sentBytes, messageLength );
				out.write( sendArray );
				out.flush();
												
				sentBytes += messageLength;
			}

		} catch (Exception e) {
			System.err.println( "Write error (" + e + "): " + call );
		}
	}
	
	public void sendRemoteBinaryCall( RemoteBinaryCall call )
	{
		Set<Map.Entry<String, JsonElement>> originalSet = call.properties.entrySet();
		JsonObject localProperties = new JsonObject();

		// Create a local copy of the JsonObject to add the binary meta data to.
		//
		Iterator<Map.Entry<String, JsonElement>> it = originalSet.iterator();
		while (it.hasNext()) 
		{
			Map.Entry<String, JsonElement> keyValue = it.next();

			localProperties.add(keyValue.getKey(), keyValue.getValue());        	 
		}

		// Build the 'temporary' json representation of the binary data and it's associated attributes.
		//
		JsonArray associatedArray = new JsonArray();
		ArrayList<Map<String,Object>> binaryList = call.getBufferedDataArray();

		ByteBuffer binaryDataBuffer = null;
		for ( int i = 0; i < binaryList.size(); i++ )
		{
			Map<String,Object> binaryElement = binaryList.get(i);

			JsonObject associatedJsonObj = new JsonObject();
			String name = (String) binaryElement.get(kNAME);
			associatedJsonObj.add( kNAME, new JsonPrimitive(name) );

			String description = (String) binaryElement.get(kDESCRIPTION);
			associatedJsonObj.add( kDESCRIPTION, new JsonPrimitive(description) );
			associatedJsonObj.add( kPOSITION_OF_BINARY_DATA, new JsonPrimitive(i));
			associatedArray.add(associatedJsonObj);

			ByteBuffer binaryData = (ByteBuffer) binaryElement.get(kDATA);
			binaryData.rewind(); // Make sure we are reading from the beginning.
			
			int sizeofData = binaryData.capacity() + 4; // 4 for the int we will put first.
			if ( binaryDataBuffer == null )
				binaryDataBuffer = ByteBuffer.allocate( sizeofData ); // Size of dataSize.
			else 
			{	
				// Increase the buffer limit.
				//
				binaryDataBuffer.limit( binaryDataBuffer.capacity() + sizeofData );
			}
			
			// Write the size of the data (int 32-bit).
			binaryDataBuffer.putInt(binaryData.remaining());
			// Write the binaryData
			binaryDataBuffer.put(binaryData.array());
		}
		
		localProperties.add(kDATA,  associatedArray);

		try 
		{		
			String properties = localProperties.toString();
			byte[] jsonBytes = properties.getBytes("UTF-8");

			ByteBuffer sendBytes = ByteBuffer.allocate( jsonBytes.length + 5 + binaryDataBuffer.capacity() ); // 1 for type, 4 for the 32 bit json length.
			sendBytes.put( (byte) MessageType.BINARY_MESSAGE.type() );
			sendBytes.putInt( jsonBytes.length ); // Json length.
			sendBytes.put( jsonBytes );
			sendBytes.put( binaryDataBuffer.array() );

			byte[] bytesToSend = sendBytes.array();
			byte sentBytes = 0; 
			while ( sentBytes < bytesToSend.length )
			{
				int messageLength = MAX_MSGBODY_SZ;
				PacketType headerValue;

				if ( bytesToSend.length < MAX_MSGBODY_SZ )
				{
					headerValue = PacketType.FULL_PACKET;
					messageLength = bytesToSend.length;
				}
				else if ( sentBytes == 0 )
					headerValue = PacketType.INITIAL_PART_PACKET;
				else if ( messageLength < properties.length() - sentBytes )
					headerValue = PacketType.CONTINUE_PARTIAL_PACKET;
				else // ( stringLength > strLength - sentBytes )			
				{
					headerValue = PacketType.LAST_PART_PACKET;
					messageLength = bytesToSend.length - sentBytes;
				}

//				byte[] subBytes = Arrays.copyOfRange(bytesToSend, sentBytes, messageLength );
//				byte type = (byte) headerValue.type();
//				out.writeByte( type );			// Packet Type.
//				out.writeInt(messageLength); 	// Length of the Sub Messages.

				ByteBuffer sendBuffer = ByteBuffer.allocate( MAX_MSG_SZ );
				byte type = (byte) headerValue.type();
				sendBuffer.put(type);
				sendBuffer.putInt(messageLength);
				sendBuffer.put(Arrays.copyOfRange(bytesToSend, sentBytes, messageLength ) );	

				int size = sendBuffer.position();
				sendBuffer.rewind();
				
				byte[] sendArray = new byte[size];
				sendBuffer.get(sendArray, 0, size);
				
				// Now send the message part.
				//
				out.write(sendArray);
				out.flush();

				sentBytes += messageLength;
			}

		} catch (Exception e) {
			System.err.println( "Write error (" + e + "): " + call );
		}
	}
	

	/**
	 * Send a RemoteCall to a selected list of TCPClients
	 * @param userList
	 * @param message 
	 */
	public static void send( Map<String, User> userList, net.smartsocket.protocols.RemoteCall message ) {
		for ( Map.Entry<String, User> user : userList.entrySet() ) {
			user.getValue().getTcpClient().send( message );
		}
	}
}
