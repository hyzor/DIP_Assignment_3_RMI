package com.server.db;

import java.nio.ByteBuffer;
//import java.nio.CharBuffer;
//import java.nio.charset.Charset;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Arrays;

import com.interf.db.ByteRecord;
import com.interf.db.Database;
import com.interf.db.Record;

public class DatabaseImpl extends UnicastRemoteObject implements Database {

	protected DatabaseImpl() throws RemoteException {
		super();
		
		records = new ArrayList<Record>();
		byteRecords = new ArrayList<ByteRecord>();
	}
	
	private int recordKeyByteSize;
	private int recordDataByteSize;
	
	protected void loadByteArrayIntoRecords(byte[] recordsByteArray, int numRecords,
			int recordKeyByteSize, int recordDataByteSize) throws RemoteException {
	int recordSize = recordKeyByteSize + recordDataByteSize;
	this.recordKeyByteSize = recordKeyByteSize;
	this.recordDataByteSize = recordDataByteSize;
	
	ByteBuffer byteBuffer;
	
	for (int i = 0; i < numRecords; ++i) {			
		// Get the bytes of the current record, and then get the corresponding key & data bytes
		byte[] recordBytes = Arrays.copyOfRange(recordsByteArray, (i*recordSize), (i*recordSize) + recordSize);
		byte[] keyBytes = Arrays.copyOfRange(recordBytes, 0, recordKeyByteSize);
		byte[] dataBytes = Arrays.copyOfRange(recordBytes, recordKeyByteSize, recordSize);
		
		// Put the key bytes into a wrapped buffer so that we can convert it into an integer
		byteBuffer = ByteBuffer.wrap(keyBytes).order(java.nio.ByteOrder.LITTLE_ENDIAN); // Intel processor => Little endian storage
		
		records.add(new Record(byteBuffer.getInt(), new String(dataBytes).toCharArray()));
	}
	}
	
	protected void loadByteArrayIntoByteRecords(byte[] recordsByteArray, int numRecords,
			int recordKeyByteSize, int recordDataByteSize) {
		int recordSize = recordKeyByteSize + recordDataByteSize;
		this.recordKeyByteSize = recordKeyByteSize;
		this.recordDataByteSize = recordDataByteSize;
		
		ByteBuffer byteBuffer;
		
		for (int i = 0; i < numRecords; ++i) {			
			// Get the bytes of the current record, and then get the corresponding key & data bytes
			byte[] recordBytes = Arrays.copyOfRange(recordsByteArray, (i*recordSize), (i*recordSize) + recordSize);
			byte[] keyBytes = Arrays.copyOfRange(recordBytes, 0, recordKeyByteSize);
			byte[] dataBytes = Arrays.copyOfRange(recordBytes, recordKeyByteSize, recordSize);
			
			// Put the key bytes into a wrapped buffer so that we can convert it into an integer
			byteBuffer = ByteBuffer.wrap(keyBytes).order(java.nio.ByteOrder.LITTLE_ENDIAN); // Intel processor => Little endian storage
			
			byteRecords.add(new ByteRecord(byteBuffer.getInt(), dataBytes));
		}
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = -605608118325697624L;
	
	private ArrayList<Record> records;
	private ArrayList<ByteRecord> byteRecords;

	@Override
	public int getNumRecords() throws RemoteException {
		return records.size();
	}

	@Override
	public Record getRecord(int recordId) throws RemoteException {
		if (recordId >= records.size())
			return null;
		
		return records.get(recordId);
	}
	
	@Override
	public void insertRecord(Record record) throws RemoteException {
		records.add(record);
	}

	@Override
	public int getRecordKeyByteSize() throws RemoteException {
		return recordKeyByteSize;
	}

	@Override
	public int getRecordDataByteSize() throws RemoteException {
		return recordDataByteSize;
	}

	@Override
	public void createRecord(int key, char[] data) {
		records.add(new Record(key, data));
	}

	@Override
	public void clearRecords() throws RemoteException {
		records.clear();
	}

	@Override
	public void insertByteRecord(ByteRecord byteRecord) throws RemoteException {
		byteRecords.add(byteRecord);
	}

	@Override
	public ByteRecord getByteRecord(int byteRecordId) throws RemoteException {
		return byteRecords.get(byteRecordId);
	}

	@Override
	public void clearByteRecords() throws RemoteException {
		byteRecords.clear();
	}
}
