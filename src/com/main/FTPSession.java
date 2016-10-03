package com.main;

import java.io.*;
import java.net.*;
import java.util.StringTokenizer;

class Constants {
	public static String _220 = "220 Ready to execute commands\r\n";
	public static String _221 = "221 Closed\r\n";
	public static String _230 = "230 Welcome to my wonderful FTP server \r\n";
	public static String _530 = "530 Not logged in.\r\n";
	public static String _200 = "200 PORT command Success \r\n";
	public static String _500 = "500 Failure \r\n";
	public static String CRLF = "\r\n";
}

public class FTPSession extends Thread {
	//
	// Path information
	//
	private String root;
	private String currDirectory;
	private String fileSeparator;

	//
	// TELNET Connection / COMMANDs
	//
	private Socket client;
	private PrintWriter out;
	private BufferedReader in;

	//
	// Data Connection
	//
	private Socket dataConnection;
	private OutputStream dataOut;

	//
	// Is anyone logged in?
	//
	private boolean hasCurrentUser;

	
	public FTPSession(Socket client) {

		this.client = client;
		fileSeparator = System.getProperty("file.separator");
		root = "/home/root1/tmp/ftp";
		// root = "/home/root1";
		currDirectory = root;

	}

	public void run() {
		try {
			in = new BufferedReader(new InputStreamReader(
					client.getInputStream()));
			out = new PrintWriter(client.getOutputStream());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		out.write(Constants._220);
		out.flush();
		System.out.println("We r here....");
		while (true) {
			String cmdLine = "";
			try {
				cmdLine = in.readLine();

				// Close Socket when client disconnects.
				if (!executeCommand(cmdLine)) {
					break;
				}

			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			System.out.println("----- cmd :" + cmdLine);
		}

		System.out.println("+++ Ending thread");
	}

	//
	// Execute a single command. This function should return "false" if the
	// command was "quit", and true in every other case.
	//
	private boolean executeCommand(String c) throws IOException {
		int index = c.indexOf(' ');
		String command = ((index == -1) ? c.toUpperCase() : (c.substring(0,
				index)).toUpperCase());
		String args = ((index == -1) ? null : c
				.substring(index + 1, c.length()));

		//
		// For debugging purposes...
		//
		System.out.println("Command: " + command + " Args: " + args);

		
		//
		// Deal with each command in its own method, please.
		//

		// ADD SOME CODE HERE.
		if (command.equals("USER")) {
			boolean isSuccess = handleUser(args);

			if(isSuccess){
				hasCurrentUser = true;
			}else{
				
			}
			
		}else if(hasCurrentUser){
			
			if (command.equals("QUIT")) {
				hasCurrentUser = false;
				handleQuit();
				return false;
			} else if (command.equals("PWD")) {
				handlePwd();
			} else if (command.equals("PORT")) {
				try {
					handlePort(args);
					writeMsgToSocket(Constants._200);
					doFlush();
				} catch (Exception e) {
					// TODO Auto-generated catch block
					writeMsgToSocket(Constants._500);
					doFlush();
					e.printStackTrace();
				}

			} else if (command.equals("LIST")) {
				String listings[] = nlstHelper(args);
				if (listings != null) {
					writeMsgToStream(out, 125, "Here comes the directory listing.",Constants.CRLF);
					doFlush();

					for (String file : listings) {
						dataOut.write((file + "\n").getBytes());
						// dataOut.flush();
					}

					writeMsgToStream(out, 226, "Directory send OK.",Constants.CRLF);
					doFlush();

				} else {
					writeMsgToStream(out, 450, "Error occured.",Constants.CRLF);
					doFlush();
				}

				dataOut.close();
				dataConnection.close();

			} else if (command.equals("TYPE")) {
				writeMsgToStream(out, 200, "Success.",Constants.CRLF);
				doFlush();
			} else if (command.equals("RETR")) {

				writeMsgToStream(out, 150, "Opening connection for file.",Constants.CRLF);
				doFlush();
				boolean isSuccess = handleRetr(args);
				if (isSuccess) {
					writeMsgToStream(out, 226, "Transfer complete.",Constants.CRLF);
					doFlush();
				} else {
					writeMsgToStream(out, 550, "Failed to open file.",Constants.CRLF);
					doFlush();
				}

			}/*else if (command.equals("SYST")) {
				writeMsgToStream(out, 215, "UNIX Type.",Constants.CRLF);
				doFlush();
			}*/
			
		}else{
			writeMsgToStream(out, 530, "Not logged in.",Constants.CRLF);
			doFlush();
		}
		
		return true;
	}

	// HANDLE RETR command
	private boolean handleRetr(String args) {

		String filename = currDirectory;
		if (args != null) {
			filename = filename + fileSeparator + args;
		}

		File f = new File(filename);

		InputStream in = null;
		try {
			in = new FileInputStream(filename);
		} catch (FileNotFoundException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
			return false;
		}
		if (f.exists() && f.isFile()) {

			byte b[] = new byte[16 * 1024];

			int count;
			try {
				while ((count = in.read(b)) > 0) {
					dataOut.write(b, 0, count);
				}
				
				in.close();
				dataOut.close();
				dataConnection.close();
				
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return false;
			}
			
			return true;

		} else {
			return false;
		}
	}

	// HANDLE PWD command
	private void handlePwd() {
		writeMsgToStream(out, 257, "\"" + currDirectory + "\"",Constants.CRLF);
		doFlush();
	}

	// HANDLE QUIT command
	private void handleQuit() {		
		writeMsgToStream(out, 221, "Closed.",Constants.CRLF);
		doFlush();
		/*try {
			client.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}*/
	}

	// HANDLE USER command
	private boolean handleUser(String args) {
		if (args.equals("anon")) {
			
			writeMsgToStream(out, 230, "Welcome to my wonderful FTP server.",Constants.CRLF);
			doFlush();
			writeMsgToStream(out, 331, "Please specify the password.", Constants.CRLF);			
			doFlush();
			return true;
		} else {			
			writeMsgToStream(out,530,"Not - logged in.",Constants.CRLF);
			doFlush();
			writeMsgToStream(out,331,"No such user.",Constants.CRLF);
			doFlush();			
			return false;
		}

	}

	private void doFlush() {
		out.flush();
	}

	private void writeMsgToSocket(String message) {
		out.write(message);
	}
	
	private void doFlushN() {
		out.flush();
	}

	//
	// Deals with writing data to specified stream
	//
	private void writeMsgToStream(OutputStream stream, int code, String message, String CRLF) throws IOException {		
		stream.write((code+" "+message+" "+CRLF).getBytes());
	}
	
	//
	// Deals with writing data to specified stream
	//
	private void writeMsgToStream(PrintWriter writer, int code, String message, String CRLF) {
		String msg = "";
		String c = Integer.toString(code);
		if(CRLF == null || CRLF.equals("")){
			writer.write(c+" "+message);
			return;
		}
		writer.write(c+" "+message+" "+CRLF);
	}

	//
	// Dealing with the CWD command.
	//
	// Acceptable arguments: .. OR . OR relative path name not including .. or .
	//
	private void handleCwd(String args) {
		String filename = currDirectory;

		//
		// First the case where we need to go back up a directory.
		//
		if (args.equals("..")) {
			int ind = filename.lastIndexOf(fileSeparator);
			if (ind > 0) {
				filename = filename.substring(0, ind);
			}
		}

		//
		// Don't do anything if the user did "cd .". In the other cases,
		// append the argument to the current directory.
		//
		else if ((args != null) && (!args.equals("."))) {
			filename = filename + fileSeparator + args;
		}

		//
		// Now make sure that the specified directory exists, and doesn't
		// attempt to go to the FTP root's parent directory. Note how we
		// use a "File" object to test if a file exists, is a directory, etc.
		//
		File f = new File(filename);

		if (f.exists() && f.isDirectory()
				&& (filename.length() >= root.length())) {
			currDirectory = filename;			
			writeMsgToStream(out,250,"The current directory has been changed to "+currDirectory,null);
		} else {
			writeMsgToStream(out,550,"Requested action not taken. File unavailable.",null);		}
	}

	private void handlePort(String args) throws Exception {
		//
		// Extract the host name (well, really its IP address) and the port
		// number
		// from the arguments.
		//
		StringTokenizer st = new StringTokenizer(args, ",");
		String hostName = st.nextToken() + "." + st.nextToken() + "."
				+ st.nextToken() + "." + st.nextToken();

		int p1 = Integer.parseInt(st.nextToken());
		int p2 = Integer.parseInt(st.nextToken());
		int p = p1 * 256 + p2;

		//
		// You need to complete this one.
		//

		dataConnection = new Socket(hostName, p);
		dataOut = dataConnection.getOutputStream();
	}

	//
	// A helper for the NLST command. The directory name is obtained by
	// appending "args" to the current directory.
	//
	// Return an array containing names of files in a directory. If the given
	// name is that of a file, then return an array containing only one element
	// (this name). If the file or directory does not exist, return null.
	//
	private String[] nlstHelper(String args) throws IOException {
		//
		// Construct the name of the directory to list.
		//
		String filename = currDirectory;
		if (args != null) {
			filename = filename + fileSeparator + args;
		}

		//
		// Now get a File object, and see if the name we got exists and is a
		// directory.
		//
		File f = new File(filename);

		if (f.exists() && f.isDirectory()) {
			return f.list();
		} else if (f.exists() && f.isFile()) {
			String[] allFiles = new String[1];
			allFiles[0] = f.getName();
			return allFiles;
		} else {
			return null;
		}
	}
}
