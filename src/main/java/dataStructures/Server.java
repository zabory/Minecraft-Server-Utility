package dataStructures;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.LinkedList;

import consoleUtilities.ServerConsoleDisplay;
import managers.ServerManager;

public class Server {

	// Name of the server
	private String serverName;

	// Process of the server
	private Process server;
	
	// Process of the file sync server
	private FileSyncServer fileSyncServer;
	
	// List of players
	private LinkedList<String> players;
	
	// Server manager
	private ServerManager SM;
	
	private File serverFile;
	
	private boolean isOnGlobal;
	private boolean isFileHostingOn;
	
	private ServerConsoleDisplay SCD;

	/**
	 * Construct the server
	 * 
	 * @param serverFile   File location of the minecraft server jar
	 * @param startCommand Starting command for the jar
	 * @param serverName   Name of the server
	 * @param SM		   Server Manager
	 */
	public Server(File serverFile, String startCommand, String serverName, ServerManager SM) {
		
		this.serverFile = serverFile;
		this.SM = SM;
		
		players = new LinkedList<String>();
		
		this.serverName = serverName;
		
		isOnGlobal = true;
		isFileHostingOn = false;
		
		SCD = new ServerConsoleDisplay();
		
		ServerThread st = new ServerThread(serverFile, startCommand);
		st.start();
	}
	
	public ServerConsoleDisplay getSCD() {
		return SCD;
	}

	/**
	 * Send a command to the server
	 * 
	 * @param command to be sent
	 */
	public void sendCommand(String command) {
		OutputStream output = server.getOutputStream();
		try {
			output.write(command.getBytes());
			output.flush();
		} catch (IOException e) {
			
		}
	}

	public String getServerName() {
		return serverName;
	}
	
	public void addPlayer(String player) {
		players.add(player);
	}
	
	public void removePlayer(String player) {
		players.remove(player);
	}

	public LinkedList<String> getPlayers(){
		return players;
	}

	public boolean isOnGlobal() {
		return isOnGlobal;
	}

	public void setOnGlobal(boolean isOnGlobal) {
		this.isOnGlobal = isOnGlobal;
	}

	public boolean isFileHostingOn() {
		return isFileHostingOn;
	}

	public void setFileHostingOn(boolean isFileHostingOn) {
		this.isFileHostingOn = isFileHostingOn;
	}
	
	public void startFileHosting(File properties) {
		isFileHostingOn = true;
		//build object
		fileSyncServer = new FileSyncServer(properties, SM.getCD());
		
		if(fileSyncServer.isRunning()) {
			//start thread
			fileSyncServer.start();
		}else {
			isFileHostingOn = false;
		}
	}
	
	
	public File getServerFile() {
		return serverFile;
	}

	public void stopFileHosting() {
		if(isFileHostingOn) {
			fileSyncServer.stopServer();
		}
		isFileHostingOn = false;
	}

	/**
	 * Server thread class to run the server
	 * 
	 * @author Ben Shabowski
	 *
	 */
	class ServerThread extends Thread {

		private File serverFile;
		private String startCommand;

		public ServerThread(File serverFile, String startCommand) {
			this.serverFile = serverFile;
			this.startCommand = startCommand;
		}

		public void run() {

			ProcessBuilder pb = new ProcessBuilder("cmd", "/c", startCommand);
			pb.directory(serverFile.getParentFile());
			

			try {
				server = pb.start();
				BufferedReader input = new BufferedReader(new InputStreamReader(server.getInputStream()));
				String line;
				
				new ErrorThread().start();

				while ((line = input.readLine()) != null && SM != null) {
					SCD.addToLog(line);
					SM.processMessageFromServer(line, serverName);
				}
								
			} catch (IOException e) {
				
			}
			
			SM.closeServer(serverName);
		}
		
		private class ErrorThread extends Thread {
			
			public void run() {
				BufferedReader errorinput = new BufferedReader(new InputStreamReader(server.getErrorStream()));
				@SuppressWarnings("unused")
				String line = "";
				
				try {
					while((line = errorinput.readLine()) != null) {
						
					}
				} catch (IOException e) {
					
				}
				SM.closeServer(serverName);
			}
		}
	}
}
