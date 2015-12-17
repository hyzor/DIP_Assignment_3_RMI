package com.interf.db;

import java.io.Serializable;

public class ByteRecord implements Serializable {	
	/**
	 * 
	 */
	private static final long serialVersionUID = -5706598464289398188L;
	public ByteRecord(int key, byte[] data) {
		this.key = key;
		this.data = data;
	}
	
	public ByteRecord() {
		this.key = -1;
		this.data = null;
	}
	
	public ByteRecord clone() {
		ByteRecord byteRecord = new ByteRecord();
		byteRecord.key = this.key;
		byteRecord.data = this.data.clone();
		return byteRecord;
	}
	
	public int key;
	public byte[] data;
}
