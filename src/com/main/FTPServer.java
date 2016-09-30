package com.main;

import java.io.*;
import java.net.*;

public class FTPServer
{
       
    private boolean isStopped;
    private int port;
    private ServerSocket ftpServer;
    
    public FTPServer(int port){
    	
    	isStopped = false;
    	this.port = port;
    }
    
    
    
    public void process(){
    	try
    	{
    	   
    		ftpServer = new ServerSocket(port);
    	    
    	    System.out.println("Server started. Please connect to it using" +
                                   "the FTP command in another window.");

    	  
    	    while (!isStopped)
    	    {
    		
    	    	Socket cliSocket = ftpServer.accept();
    	    	System.out.println("--- Opening Connection ");
    	    	new FTPSession(cliSocket).start();
    	    }
    	}
    	catch (Exception e)
    	{
    	    e.printStackTrace();
    	}
    }
    
    public void start(){
    	System.out.println("---------- STARTING server -------------");
    	process();    	
    	
    }

    public void stop(){
    	
    	try {
			ftpServer.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			System.out.println("*** Error while stopping server.");
			e.printStackTrace();
		}
    	isStopped = true;
    }
}


