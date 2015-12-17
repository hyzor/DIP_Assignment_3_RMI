package com.interf.db;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface Database extends Remote {
	public int getNumRecords() throws RemoteException;
	public int getRecordKeyByteSize() throws RemoteException;
	public int getRecordDataByteSize() throws RemoteException;
	
	public Record getRecord(int recordId) throws RemoteException;
	public void insertRecord(Record record) throws RemoteException;
	public void createRecord(int key, char[] data) throws RemoteException;
	
	public void clearRecords() throws RemoteException;
	
	public void insertByteRecord(ByteRecord byteRecord) throws RemoteException;
	public ByteRecord getByteRecord(int byteRecordId) throws RemoteException;
	public void clearByteRecords() throws RemoteException;
}
