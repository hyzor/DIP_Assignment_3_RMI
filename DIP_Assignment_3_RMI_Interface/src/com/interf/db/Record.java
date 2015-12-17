package com.interf.db;

import java.io.Serializable;

public class Record implements Serializable {	
	/**
	 * 
	 */
	private static final long serialVersionUID = -3907318371545416725L;
	
	public Record(int key, char[] data) {
		this.key = key;
		this.data = data;
	}
	
	public int key;
	public char[] data;
}
