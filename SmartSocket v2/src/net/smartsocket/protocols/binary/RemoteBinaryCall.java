package net.smartsocket.protocols.binary;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class RemoteBinaryCall extends RemoteCall
{
	static public String kNAME = "name";
	static public String kDESCRIPTION = "description";
	static public String kDATA = "data";
	
	protected transient ArrayList<Map<String,Object>> data;	 // Array of Dictionary { "name" - binary data name, "description", String, "data" - ByteBuffer }

	public RemoteBinaryCall(String method)
	{
		super(method);

		data = new ArrayList<Map<String,Object>>();
	}

	public void addBinaryData( ByteBuffer byteBuffer, String dataName, String dataDescription)
	{
		Map<String,Object> binaryData = new HashMap<String,Object>();
		
		binaryData.put(kNAME, dataName);
		binaryData.put(kDESCRIPTION, dataDescription);
		binaryData.put(kDATA, byteBuffer);
		
		data.add(binaryData);

		return;
	}
	
	
	public Map<String,Object> getBinaryDataForName(String name)
	{
		for ( Map<String,Object> dataEntry : data )
		{
			if ( dataEntry.get(kNAME).equals(name) )
				return dataEntry;
		}
		
		return null;
	}
	
	public ArrayList<Map<String,Object>> getBufferedDataArray()
	{
		return data;
	}
}
