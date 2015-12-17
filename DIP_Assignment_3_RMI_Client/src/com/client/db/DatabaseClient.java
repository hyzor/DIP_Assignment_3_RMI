package com.client.db;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.interfaces.RSAPublicKey;
import java.util.ArrayList;
import java.util.Arrays;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;

import com.interf.db.Database;
import com.interf.db.ByteRecord;

public class DatabaseClient {
	
	public static enum algs {
		DES,
		DES_3,
		AES,
		DH,
		RSA_CRT
	}
	
	private static byte[] recordsByteArray;
	private static ArrayList<ByteRecord> recordsArray;
	private static ArrayList<ByteRecord> tempRecordsArray;
	private static ArrayList<ByteRecord> tempReadRecordsArray;
	
	public static void main(String[] args) {		
		String fileName;
		int numRecords;
		int recordDataSize;
		int recordKeyByteSize = 4;	// Always an integer
		int numRecordsRead;
		int recordSize;
		
		if (args.length < 4) {
			System.out.println("Usage: HOST RECORDS_FILE_NAME NUM_RECORDS RECORD_DATA_SIZE");
			System.exit(-1);
		}
		
		String host = args[0];
		fileName = args[1];
		numRecords = Integer.valueOf(args[2]);
		recordDataSize = Integer.valueOf(args[3]);
		
		algs[] algsArr = algs.values();
		int numAlgs = algsArr.length;
		
		int curIteration = 0;
		algs curAlg = algsArr[0];
		
		long timerStart;
		long timerEnd;
		
		long[] timesMsEnc = new long[numAlgs];
		long[] timesMsDec = new long[numAlgs];
		long[] timesMsEncRes = new long[numAlgs];
		long[] timesMsDecRes = new long[numAlgs];
		
		KeyGenerator desKeyGenerator = null;
		SecretKey desSecretKey;
		Cipher desCipher = null;
		
		KeyGenerator des3KeyGenerator = null;
		SecretKey des3SecretKey;
		Cipher des3Cipher = null;
		
		KeyGenerator aesKeyGenerator = null;
		SecretKey aesSecretKey;
		Cipher aesCipher = null;
		
		int rsaBitLength = 1024;
	    KeyPairGenerator rsaKeyPairGenerator = null;
	    KeyPair rsaKeyPair;
	    RSAPrivateCrtKey rsaPrivateCrtKey;
	    RSAPublicKey rsaPublicKey;
	    Cipher rsaCipher = null;
		
		try {
			desKeyGenerator = KeyGenerator.getInstance("DES");
			des3KeyGenerator = KeyGenerator.getInstance("DESede");
			aesKeyGenerator = KeyGenerator.getInstance("AES");
			rsaKeyPairGenerator = KeyPairGenerator.getInstance("RSA");
		} catch (NoSuchAlgorithmException e1) {
			System.err.println("Invalid algorithm for KeyGenerator/KeyPairGenerator/KeyFactory!");
			e1.printStackTrace();
		}
	    
		rsaKeyPairGenerator.initialize(rsaBitLength);
		
		desSecretKey = desKeyGenerator.generateKey();
		des3SecretKey = des3KeyGenerator.generateKey();
		aesSecretKey = aesKeyGenerator.generateKey();
		rsaKeyPair = rsaKeyPairGenerator.generateKeyPair();
		rsaPrivateCrtKey = (RSAPrivateCrtKey) rsaKeyPair.getPrivate();
		rsaPublicKey = (RSAPublicKey) rsaKeyPair.getPublic();
		
		int rsaBlockSize = 117; // 128-11
		int rsaPadding = 11;
		int rsaPaddedBlockSize = rsaBlockSize + rsaPadding;
		int rsaNumBlocksPerRecord;
		int rsaTotalByteLength;
		
		if (rsaBlockSize > recordDataSize)
			rsaNumBlocksPerRecord = 1;
		else
			rsaNumBlocksPerRecord = (int)Math.ceil((double)recordDataSize / rsaBlockSize);
		byte[][] rsaBlocks = new byte[rsaNumBlocksPerRecord][rsaBlockSize];
		byte[][] rsaPaddedBlocks = new byte[rsaNumBlocksPerRecord][rsaPaddedBlockSize];
		ByteBuffer rsaByteBuffer;
		
		rsaTotalByteLength = rsaPaddedBlockSize * rsaNumBlocksPerRecord;
		byte[] rsaTargetBuffer = new byte[rsaTotalByteLength];
		
		try {
			desCipher = Cipher.getInstance("DES/ECB/PKCS5Padding");
			des3Cipher = Cipher.getInstance("DESede/ECB/PKCS5Padding");
			aesCipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
			rsaCipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
		} catch (NoSuchAlgorithmException e1) {
			System.err.println("Invalid algorithm for cipher!");
			e1.printStackTrace();
		} catch (NoSuchPaddingException e1) {
			System.err.println("Invalid padding for cipher!");
			e1.printStackTrace();
		}
		
		Database stub = null;
		try {
			Registry reg = LocateRegistry.getRegistry(host);
			stub = (Database) reg.lookup("Database");
			
		} catch (Exception e) {
			System.err.println("Client exception: " + e.toString());
			e.printStackTrace();
		}
		
		// Load records into client memory
		recordSize = recordKeyByteSize + recordDataSize;
		recordsByteArray = loadRecordsFromFile(fileName);
		numRecordsRead = recordsByteArray.length / recordSize;
		
		if (numRecordsRead != numRecords) {
			System.err.println("Invalid number of records read from file!");
			System.exit(-1);
		}
		
		recordsArray = byteArrayToByteRecords(recordsByteArray, numRecordsRead, recordKeyByteSize, recordDataSize);

		if (recordsArray.size() != numRecordsRead) {
			System.err.println("Invalid number of records read from byte array!");
			System.exit(-1);
		}
		
		int i;
		int j;
		
		printByteRecord(recordsArray.get(1));
		printByteRecord(recordsArray.get(recordsArray.size()-1));
		
		tempRecordsArray = new ArrayList<ByteRecord>(numRecords);
		tempReadRecordsArray = new ArrayList<ByteRecord>();
		
		while (curIteration < numAlgs) {
			curAlg = algsArr[curIteration];
			
			// Prepare for this iteration
			tempRecordsArray.clear();
			tempReadRecordsArray.clear();
			
			try {
				stub.clearByteRecords();
			} catch (Exception e) {
				System.err.println("Client exception: " + e.toString());
				e.printStackTrace();
			}
			
			for (i = 0; i < numRecords; ++i) {
				tempRecordsArray.add(recordsArray.get(i).clone());
			}
			
			timerStart = System.currentTimeMillis();
			// Encrypt records
			switch(curAlg) {
			case DES: {
				try {
					desCipher.init(Cipher.ENCRYPT_MODE, desSecretKey);
				} catch (InvalidKeyException e) {
					System.err.println("Invalid key for DES!");
					e.printStackTrace();
				}
				
				for (i = 0; i < numRecords; ++i) {
					try {
						tempRecordsArray.get(i).data = desCipher.doFinal(tempRecordsArray.get(i).data);
					} catch (IllegalBlockSizeException e) {
						System.err.println("Illegal block size for DES Cipher encryption!");
						e.printStackTrace();
					} catch (BadPaddingException e) {
						System.err.println("Bad padding for DES Cipher encryption!");
						e.printStackTrace();
					}
				}
				
				break;
			}
			
			case DES_3: {
				try {
					des3Cipher.init(Cipher.ENCRYPT_MODE, des3SecretKey);
				} catch (InvalidKeyException e) {
					System.err.println("Invalid key for 3-DES!");
					e.printStackTrace();
				}
				
				for (i = 0; i < numRecords; ++i) {
					try {
						tempRecordsArray.get(i).data = des3Cipher.doFinal(tempRecordsArray.get(i).data);
					} catch (IllegalBlockSizeException e) {
						System.err.println("Illegal block size for 3-DES Cipher encryption!");
						e.printStackTrace();
					} catch (BadPaddingException e) {
						System.err.println("Bad padding for 3-DES Cipher encryption!");
						e.printStackTrace();
					}
				}
				
				break;
			}
			
			case AES: {
				try {
					aesCipher.init(Cipher.ENCRYPT_MODE, aesSecretKey);
				} catch (InvalidKeyException e) {
					System.err.println("Invalid key for AES!");
					e.printStackTrace();
				}
				
				for (i = 0; i < numRecords; ++i) {
					try {
						tempRecordsArray.get(i).data = aesCipher.doFinal(tempRecordsArray.get(i).data);
					} catch (IllegalBlockSizeException e) {
						System.err.println("Illegal block size for AES Cipher encryption!");
						e.printStackTrace();
					} catch (BadPaddingException e) {
						System.err.println("Bad padding for AES Cipher encryption!");
						e.printStackTrace();
					}
				}
				
				break;
			}
			
			case DH: {
				break;
			}
			
			case RSA_CRT: {
				try {
					rsaCipher.init(Cipher.ENCRYPT_MODE, rsaPublicKey);
				} catch (InvalidKeyException e) {
					System.err.println("Invalid key for AES!");
					e.printStackTrace();
				}
				
				for (i = 0; i < numRecords; ++i) {
					// Slice data into smaller chunks
					for (j = 0; j < rsaNumBlocksPerRecord; ++j) {
						rsaBlocks[j] = Arrays.copyOfRange(tempRecordsArray.get(i).data,
								j*rsaBlockSize, (j*rsaBlockSize)+rsaBlockSize);
						
						// Cipher chunk
						try {
							rsaBlocks[j] = rsaCipher.doFinal(rsaBlocks[j]);
						} catch (IllegalBlockSizeException e) {
							System.err.println("Illegal block size for RSA Cipher encryption!");
							e.printStackTrace();
						} catch (BadPaddingException e) {
							System.err.println("Bad padding for RSA Cipher encryption!");
							e.printStackTrace();
						}
					}
					
					rsaByteBuffer = ByteBuffer.wrap(rsaTargetBuffer);
					
					for (j = 0; j < rsaNumBlocksPerRecord; ++j) {
						rsaByteBuffer.put(rsaBlocks[j]);
					}
					
					rsaByteBuffer.flip();
					tempRecordsArray.get(i).data = rsaByteBuffer.array();
				}
				
				break;
			}
			
			default:
				break;
			}
			timerEnd = System.currentTimeMillis();
			
			timesMsEnc[curIteration] = timerEnd - timerStart;
			
			timerStart = System.currentTimeMillis();
			// Send encrypted records to server
			try {
				for (i = 0; i < numRecords; ++i) {
					stub.insertByteRecord(tempRecordsArray.get(i));
				}
			} catch (Exception e) {
				System.err.println("Client exception: " + e.toString());
				e.printStackTrace();
			}
			timerEnd = System.currentTimeMillis();
			
			timesMsEncRes[curIteration] = timerEnd - timerStart;
			
			timerStart = System.currentTimeMillis();
			// Request encrypted records from server
			try {
				for (i = 0; i < numRecords; ++i) {
					tempReadRecordsArray.add(stub.getByteRecord(i));
				}
			} catch (Exception e) {
				System.err.println("Client exception: " + e.toString());
				e.printStackTrace();
			}
			timerEnd = System.currentTimeMillis();
			
			timesMsDecRes[curIteration] = timerEnd - timerStart;
			
			if (tempReadRecordsArray.size() != tempRecordsArray.size()) {
				System.err.println("The amount of records from server are not equal to client!");
			}
			
			// Decrypt records
			timerStart = System.currentTimeMillis();
			switch(curAlg) {
			case DES: {
				try {
					desCipher.init(Cipher.DECRYPT_MODE, desSecretKey);
				} catch (InvalidKeyException e) {
					System.err.println("Invalid key for DES!");
					e.printStackTrace();
				}
				
				for (i = 0; i < numRecords; ++i) {
					try {
						tempReadRecordsArray.get(i).data = desCipher.doFinal(tempReadRecordsArray.get(i).data);
					} catch (IllegalBlockSizeException e) {
						System.err.println("Illegal block size for DES Cipher decryption!");
						e.printStackTrace();
					} catch (BadPaddingException e) {
						System.err.println("Bad padding for DES Cipher decryption!");
						e.printStackTrace();
					}
				}
				
				break;
			}
			
			case DES_3: {
				try {
					des3Cipher.init(Cipher.DECRYPT_MODE, des3SecretKey);
				} catch (InvalidKeyException e) {
					System.err.println("Invalid key for 3-DES!");
					e.printStackTrace();
				}
				
				for (i = 0; i < numRecords; ++i) {
					try {
						tempReadRecordsArray.get(i).data = des3Cipher.doFinal(tempReadRecordsArray.get(i).data);
					} catch (IllegalBlockSizeException e) {
						System.err.println("Illegal block size for 3-DES Cipher decryption!");
						e.printStackTrace();
					} catch (BadPaddingException e) {
						System.err.println("Bad padding for 3-DES Cipher decryption!");
						e.printStackTrace();
					}
				}
				
				break;
			}
			
			case AES: {
				try {
					aesCipher.init(Cipher.DECRYPT_MODE, aesSecretKey);
				} catch (InvalidKeyException e) {
					System.err.println("Invalid key for AES!");
					e.printStackTrace();
				}
				
				for (i = 0; i < numRecords; ++i) {
					try {
						tempReadRecordsArray.get(i).data = aesCipher.doFinal(tempReadRecordsArray.get(i).data);
					} catch (IllegalBlockSizeException e) {
						System.err.println("Illegal block size for AES Cipher decryption!");
						e.printStackTrace();
					} catch (BadPaddingException e) {
						System.err.println("Bad padding for AES Cipher decryption!");
						e.printStackTrace();
					}
				}
				
				break;
			}
			
			case DH: {
				break;
			}
			
			case RSA_CRT: {
				try {
					rsaCipher.init(Cipher.DECRYPT_MODE, rsaPrivateCrtKey);
				} catch (InvalidKeyException e) {
					System.err.println("Invalid key for RSA!");
					e.printStackTrace();
				}
				
				for (i = 0; i < numRecords; ++i) {
					byte[] recordByteArr = tempReadRecordsArray.get(i).data;
					for (j = 0; j < rsaNumBlocksPerRecord; ++j) {
						rsaPaddedBlocks[j] = Arrays.copyOfRange(recordByteArr,
								j*rsaPaddedBlockSize, (j*rsaPaddedBlockSize) + rsaPaddedBlockSize);
						
						try {
							rsaPaddedBlocks[j] = rsaCipher.doFinal(rsaPaddedBlocks[j]);
						} catch (IllegalBlockSizeException e) {
							System.err.println("Illegal block size for RSA Cipher decryption!");
							e.printStackTrace();
						} catch (BadPaddingException e) {
							System.err.println("Bad padding for RSA Cipher decryption!");
							e.printStackTrace();
						}
					}
					
					rsaByteBuffer = ByteBuffer.wrap(rsaTargetBuffer);
					
					for (j = 0; j < rsaNumBlocksPerRecord; ++j) {
						rsaByteBuffer.put(rsaPaddedBlocks[j]);
					}
					
					rsaByteBuffer.flip();
					tempReadRecordsArray.get(i).data = rsaByteBuffer.array();
				}
				
				break;
			}
			
			default:
				break;
			}
			timerEnd = System.currentTimeMillis();
			
			timesMsDec[curIteration] = timerEnd - timerStart;
			
			curIteration++;
		}
		
		System.out.println("---------------\nResults\n---------------");
		System.out.printf("DES: Enc(%d ms) EncRes(%d ms) Dec(%d ms) DecRes(%d ms)\n", timesMsEnc[0], timesMsEncRes[0], timesMsDec[0], timesMsDecRes[0]);
		System.out.printf("3-DES: Enc(%d ms) EncRes(%d ms) Dec(%d ms) DecRes(%d ms)\n", timesMsEnc[1], timesMsEncRes[1], timesMsDec[1], timesMsDecRes[1]);
		System.out.printf("AES: Enc(%d ms) EncRes(%d ms) Dec(%d ms) DecRes(%d ms)\n", timesMsEnc[2], timesMsEncRes[2], timesMsDec[2], timesMsDecRes[2]);
		//System.out.printf("DH: Enc(%d ms) EncRes(%d ms) Dec(%d ms) DecRes(%d ms)\n", timesMsEnc[3], timesMsEncRes[3], timesMsDec[3], timesMsDecRes[3]);
		System.out.printf("RSA_CRT: Enc(%d ms) EncRes(%d ms) Dec(%d ms) DecRes(%d ms)\n", timesMsEnc[4], timesMsEncRes[4], timesMsDec[4], timesMsDecRes[4]);
	}
	
	private static byte[] loadRecordsFromFile(String filename) {
		Path path = Paths.get(filename);
		byte[] data = null;
		
		try {
			data = Files.readAllBytes(path);
		} catch (IOException e) {
			System.err.println("Failed to load record file! (" + e.toString() + ")");
			e.printStackTrace();
		}
		
		return data;
	}
	
	private static ArrayList<ByteRecord> byteArrayToByteRecords(byte[] recordsByteArray, int numRecords,
			int recordKeyByteSize, int recordDataByteSize) {
		ArrayList<ByteRecord> records = new ArrayList<ByteRecord>(numRecords);
		int recordSize = recordKeyByteSize + recordDataByteSize;
		
		ByteBuffer byteBuffer;
		
		for (int i = 0; i < numRecords; ++i) {			
			// Get the bytes of the current record, and then get the corresponding key & data bytes
			byte[] recordBytes = Arrays.copyOfRange(recordsByteArray, (i*recordSize), (i*recordSize) + recordSize);
			byte[] keyBytes = Arrays.copyOfRange(recordBytes, 0, recordKeyByteSize);
			byte[] dataBytes = Arrays.copyOfRange(recordBytes, recordKeyByteSize, recordSize);
			
			// Put the key bytes into a wrapped buffer so that we can convert it into an integer
			byteBuffer = ByteBuffer.wrap(keyBytes).order(java.nio.ByteOrder.LITTLE_ENDIAN); // Intel processor => Little endian storage
			
			records.add(new ByteRecord(byteBuffer.getInt(), dataBytes));
		}
		
		return records;
	}
	
	private static void printByteRecord(ByteRecord byteRecord) {
		System.out.printf("%d %d%s\n", byteRecord.key, byteRecord.data[0], new String(byteRecord.data));
	}
}
