package com.server.db;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

import com.interf.db.Database;

public class DatabaseServer {	
	public static void main(String args[]) {		
		if (System.getSecurityManager() == null) {
			System.setSecurityManager(new SecurityManager());
		}
		
		try {
			Database stub = new DatabaseImpl();
			
			Registry registry = LocateRegistry.getRegistry();
			registry.rebind("Database", stub);
			
			System.out.println("DatabaseServer up and running!");
		} catch (Exception e) {
			System.err.println("DatabaseServer exception: " + e.toString());
			e.printStackTrace();
		}
	}
}