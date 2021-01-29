package managers;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

import consoleUtilities.SummaryDisplay;
import dataStructures.Server;
import discord.Bot;

public class ServerManager {
	
	//int to hold server count
	// private int serverCount;
	
	//Map to hold servers by their name
	private Map<String, Server> servers;
	
	//Console
	private SummaryDisplay summaryDisplay;
	
	private boolean awaitingCommand;
	
	private Bot bot;

	public ServerManager() {
		awaitingCommand = false;
		servers = new HashMap<String, Server>();
		summaryDisplay = new SummaryDisplay();
		summaryDisplay.setFocused(true);
		bot = new Bot(this);

		new KeyboardThread().start();
	}
	
	public SummaryDisplay getCD() {
		return summaryDisplay;
	}
	
	public void processMessageFromServer(String message, String server) {
		if (awaitingCommand) {
			summaryDisplay.addCommandLog(message);
			awaitingCommand = false;
		} else {

			if ((message.contains("[Server thread/INFO]: Done")
					|| (message.contains("[minecraft/DedicatedServer]: Done")))
					&& message.contains("! For help, type \"help\"")) {
				
				summaryDisplay.addServer(servers.get(server));
				
			} else if (message.contains(" joined the game")) {
				
				summaryDisplay.addPlayer(message.split(":")[3].trim().replace(" joined the game", ""));
				if(servers.get(server).isOnGlobal()) {
					sendAllMinusOne(server, "/tellraw @a {\"text\":\"" + message.split(":")[3].trim().split(" ")[0]
						+ " joined " + server + " server\",\"color\":\"yellow\"}");
					sendLoginToPlayer(server, message.split(":")[3].trim().split(" ")[0]);
				}
				summaryDisplay.addToLog(message.split(":")[3].trim());
				
				servers.get(server).addPlayer(message.split(":")[3].trim().split(" ")[0]);
				bot.changeActiveCount(summaryDisplay.getPlayerCount());
				bot.sendMessage(message.split(":")[3].trim().split(" ")[0] + " joined " + server + " server");
			} else if (message.contains(" left the game")) {
				
				summaryDisplay.removePlayer(message.split(":")[3].trim().replace(" left the game", ""));
				if(servers.get(server).isOnGlobal())
					sendAllMinusOne(server, "/tellraw @a {\"text\":\"" + message.split(":")[3].trim().split(" ")[0]
						+ " left " + server + " server\",\"color\":\"yellow\"}");
				summaryDisplay.addToLog(message.split(":")[3].trim());
				servers.get(server).removePlayer(message.split(":")[3].trim().split(" ")[0]);
				bot.changeActiveCount(summaryDisplay.getPlayerCount());
				bot.sendMessage(message.split(":")[3].trim().split(" ")[0] + " left " + server + " server");
				
			} else if (message.split(":").length > 3 && (message.split(":")[3].startsWith(" <") && message.contains("> ") && !message.contains("[SUCCESS] Line"))) {
				summaryDisplay.addToLog(message.split(":")[3].trim());
				bot.sendMessage(server + " server:" + message.replace(message.split(":")[0], "").replace(message.split(":")[1], "").replace(message.split(":")[2], "").replace(":::", "").trim());
				if(servers.get(server).isOnGlobal())
					sendAllMinusOne(server, "tellraw @a {\"text\":\"" + message.replace(message.split(":")[0], "").replace(message.split(":")[1], "").replace(message.split(":")[2], "").replace(":::", "").trim() + "\"}");
			}
		}
	}
	
	private void messageToSummary(String line) {
		summaryDisplay.addCommandLog("Command:" + line);
		
		switch (line.split(" ")[0]) {
		
		//show console of focused server
		case "console":
			if(summaryDisplay.getSelected()!= null) {
				summaryDisplay.setFocused(false);
				summaryDisplay.getSelected().getSCD().setFocused(true);
			}
			break;
		//clear the console
		case "clear":
			summaryDisplay.clearCommandLog();
			break;
		//sends a help message to the console
		case "help":
			summaryDisplay.addCommandLog("clear: clears command log");
			summaryDisplay.addCommandLog("console: goes to the focused servers console");
			summaryDisplay.addCommandLog("select <server name>: Selects a server for focus");
			summaryDisplay.addCommandLog("generateFSO: generates a file server config for the focused server");
			summaryDisplay.addCommandLog("remove <player name>: removes player from list of players");
			summaryDisplay.addCommandLog("startFSO: starts file server for focused server");
			summaryDisplay.addCommandLog("stopFSO: stops file server for focused server");
			summaryDisplay.addCommandLog("start <server name> <server jar location> <start command||start.bat>: starts the server"
					+ "\n\tserver name: name of the server"
					+ "\n\tjar location: location of the server.jar"
					+ "\n\tstart command or start.bat: start the server with these params, or just use this bat file");
			summaryDisplay.addCommandLog("start <start file locaiton>: reads the start file line by line starting the servers within");
			summaryDisplay.addCommandLog("stopall: stops all servers");
			summaryDisplay.addCommandLog("stop: stops focused server");
			summaryDisplay.addCommandLog("toggleChat: toggles global chat for focused server");
			summaryDisplay.addCommandLog("options: shows what the check boxes mean in the options column");
			summaryDisplay.addCommandLog("! <command>: Sends command to focused server");
			break;
		//select a new focus for the server
		case "select":
			if(summaryDisplay.setSelectedIndex(line.replace("select ", ""))) {
				summaryDisplay.addCommandLog("Server focus now on server:" + line.replace("select ", ""));
			}else {
				summaryDisplay.addCommandLog("There is no server with that index");
			}
			break;
		//generates File server options
		case "generateFSO":
			if(summaryDisplay.getSelected()!= null) {
				File properties = new File(summaryDisplay.getSelected().getServerFile().getParentFile().getAbsolutePath() + "\\modmanager.properties");
				try {
					properties.createNewFile();
					FileWriter out = new FileWriter(properties);
					
					out.write("port=50020\n");
					out.write("dirs_to_sync=mods,config\n");
					out.write("ignore=");
					out.flush();
					out.close();
					
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			break;
		//remove player from list
		case "remove":
			summaryDisplay.removePlayer(line.split(" ")[1]);
			summaryDisplay.drawDisplay();
			bot.changeActiveCount(summaryDisplay.getPlayerCount());
			break;
		//starts file server
		case "startFSO":
			if(summaryDisplay.getSelected()!= null) {
				File properties = null;
				for(File x : summaryDisplay.getSelected().getServerFile().getParentFile().listFiles()) {
					if(x.getName().equals("modmanager.properties")) {
						properties = x;
						break;
					}
				}
				if(properties != null) {
					summaryDisplay.getSelected().startFileHosting(properties);
					summaryDisplay.drawDisplay();
				}else {
					summaryDisplay.addCommandLog("Unable to find modmanager.properties. Please generate with \"generateFSO\" command.");
				}
			}
			break;
			
			//stops file server
					case "stopFSO":
						if(summaryDisplay.getSelected()!= null) {
							summaryDisplay.addCommandLog("Stopping file hosting");
							summaryDisplay.getSelected().stopFileHosting();
							summaryDisplay.drawDisplay();
						}
						break;
		//start a server
		case "start":
			startServerFromCommandLine(line.replace("start ", ""));
			break;
		case "stopall":
			summaryDisplay.addCommandLog("Sending stop commands to all servers");
			servers.forEach((key, value) -> {
				value.stopFileHosting();
				value.sendCommand("stop\n");
			});
			break;
		case "stop":
			summaryDisplay.addCommandLog("Sending stop commands to stop focused server");
			if(summaryDisplay.getSelected()!= null) {
				summaryDisplay.getSelected().stopFileHosting();
				summaryDisplay.getSelected().sendCommand("stop\n");
			}
			break;
		case "toggleChat":
			if(summaryDisplay.getSelected() != null) {
				summaryDisplay.addCommandLog("Toggling global chat for focused server");
				summaryDisplay.getSelected().setOnGlobal(!summaryDisplay.getSelected().isOnGlobal());
				summaryDisplay.drawDisplay();
			}
			break;
		case "options":
			summaryDisplay.addCommandLog("1: If the server is on global chat connect");
			summaryDisplay.addCommandLog("2: If the server has a file sync server running");
			break;
		case "!":
			if(summaryDisplay.getSelected()!= null) {
				summaryDisplay.getSelected().sendCommand(line.replace("! ", "") + "\n");
				awaitingCommand = true;
			}
			break;
		case "\n":
			summaryDisplay.drawDisplay();
			break;
		default:
			summaryDisplay.addCommandLog("Unknown command. Type \"help\" for help.");
			break;
		}
	}
	
	public void processMessageFromConsole(String line) {
		if(summaryDisplay.isFocused()) {
			messageToSummary(line);
		}else {
			if(line.equals("back")) {
				servers.forEach((key, server) -> {
					server.getSCD().setFocused(false);
				});
				summaryDisplay.setFocused(true);
			}else {
				servers.forEach((key, server) -> {
					if(server.getSCD().isFocused()) {
						server.sendCommand(line + "\n");
					}
				});
			}
		}
	}
	
	public void processMessageFromDiscord(String message) {
		sendAll("/tellraw @a {\"text\":\"" + message + "\"}");
		summaryDisplay.addToLog(message);
	}

	private void sendLoginToPlayer(String serverName, String player) {
		Server server = servers.get(serverName);
		server.sendCommand(
				"/tellraw " + player + " {\"text\":\"***********************************************\",\"color\":\"dark_blue\"}\n");
		server.sendCommand("/tellraw " + player
				+ " {\"text\":\"*This server is on global chat*\",\"bold\":true,\"color\":\"dark_blue\"}\n");
		server.sendCommand(
				"/tellraw " + player + " {\"text\":\"***********************************************\",\"color\":\"dark_blue\"}\n");
		server.sendCommand("/tellraw " + player + " {\"text\":\"Current population:" + summaryDisplay.getPlayerCount()
				+ "\",\"color\":\"dark_gray\"}\n");
		server.sendCommand("/tellraw " + player + " {\"text\":\"Servers on global chat: " + getGlobalChatList()
				+ "\",\"color\":\"dark_gray\"}\n");
		if (bot.isActive()) {
			server.sendCommand("/tellraw " + player
					+ " [\"\",{\"text\":\"Chat is also connected to the \",\"color\":\"dark_gray\"},{\"text\":\"Discord\",\"underlined\":true,\"color\":\"dark_purple\",\"clickEvent\":{\"action\":\"open_url\",\"value\":\"https://discord.gg/gf4Nn4nS3d\"},\"hoverEvent\":{\"action\":\"show_text\",\"contents\":{\"text\":\"Click to join\"}}},{\"text\":\" server\",\"color\":\"dark_gray\"}]\n");
		}

	}
	
	private String getGlobalChatList() {
		String list = "";
		
		for(String x: servers.keySet()) {
			if(servers.get(x).isOnGlobal()) {
				list += ", " + x;
			}
		}
		
		return list.replaceFirst(", ", "");
	}
	
	private void sendAll(String message) {
		for (String key : servers.keySet()) {
			if (servers.get(key).isOnGlobal()) {
				servers.get(key).sendCommand(message + "\n");
			}
		}
	}
	
	public void log(String message) {
		summaryDisplay.addCommandLog(message);
	}
	
	private void sendAllMinusOne(String server, String message) {
		for (String key : servers.keySet()) {
			if (!key.equals(server) && servers.get(key).isOnGlobal()) {
				servers.get(key).sendCommand(message + "\n");
			}
		}
	}
	
	public void closeServer(String server) {
		summaryDisplay.removeServer(server);
		for (String player : servers.get(server).getPlayers()) {
			summaryDisplay.removePlayer(player);
		}
	}
	
	public void startServer(File serverFile, String startCommand, String serverName) {

		summaryDisplay.addCommandLog("Starting " + serverName + " server");
		servers.put(serverName, new Server(serverFile, startCommand, serverName, this));
	}
	
	public void startServerFromCommandLine(String line) {
		//start <Server name> <file path to server jar> <server starter/commands>
		String name = "";
		String filePath = "";
		String command = "";
		
		String newLine = "";
		
		boolean inside = false;
		for(char x : line.toCharArray()) {
			if(x == ' ' && !inside) {
				newLine += "::::";
			}else if(x == ' ' && inside) {
				newLine += ' ';
			}else if(x == '"') {
				inside = !inside;
			}else {
				newLine += x;
			}
		}
		
		if(newLine.split("::::").length == 3) {
			name = newLine.split("::::")[0];
			filePath = newLine.split("::::")[1];
			command = newLine.split("::::")[2];
			
			if(new File(filePath).exists()) {
				startServer(new File(filePath), command, name);
			}else {
				summaryDisplay.addCommandLog("Server file cannot be found");
			}
		}else if(newLine.split("::::").length == 2){
			name = newLine.split("::::")[0];
			filePath = newLine.split("::::")[1];
			
			if(new File(filePath).exists()) {
				
				for(File x : new File(filePath).getParentFile().listFiles()) {
					if(x.getName().contains(".bat")) {
						command = x.getName();
						break;
					}
				}
				
				startServer(new File(filePath), command, name);
			}else {
				summaryDisplay.addCommandLog("Server file cannot be found");
			}
		}else if(newLine.split("::::").length == 1){
			if(new File(newLine).exists()) {
				try {
					Scanner fileInput = new Scanner(new File(newLine));
					while(fileInput.hasNextLine()) {
						startServerFromCommandLine(fileInput.nextLine());
					}
					fileInput.close();
					
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				}
			}else {
				summaryDisplay.addCommandLog("Invalid command");
			}
		}else {
			summaryDisplay.addCommandLog("Invalid command");
		}
	}
	
	private class KeyboardThread extends Thread {

		public void run() {

			Scanner kb = new Scanner(System.in);
			String input = "";
			
			while(!input.equals("quit")) {
				input = kb.nextLine();
				processMessageFromConsole(input);
			}
			
			kb.close();
		}

	}
}
