package com.main;

import java.io.File;

public class Tester {

	public static void main(String[] args) {
		// TODO Auto-generated method stub

		File f = new File("/home/root1");
		String list[] = f.list();
		
		for(String p : list){
			//System.out.println(" : "+p);
		}
		
		FTPServer server = new FTPServer(3001);
		server.start();
		
	}

}
