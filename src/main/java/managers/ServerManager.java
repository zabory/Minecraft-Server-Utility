package managers;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

import consoleUtilities.ConsoleDisplay;
import dataStructures.Server;
import discord.Bot;

public class ServerManager {
	
	//int to hold server count
	// private int serverCount;
	
	//Map to hold servers by their name
	private Map<String, Server> servers;
	
	//Console
	private ConsoleDisplay CD;
	
	private boolean awaitingCommand;
	
	private Bot bot;

	public ServerManager() {
		awaitingCommand = false;
		servers = new HashMap<String, Server>();
		CD = new ConsoleDisplay();
		bot = new Bot(this);

		new KeyboardThread().start();
	}
	
	public ConsoleDisplay getCD() {
		return CD;
	}
	
	public void processMessageFromServer(String message, String server) {
		if (awaitingCommand) {
			CD.addCommandLog(message);
			awaitingCommand = false;
		} else {

			if ((message.contains("[Server thread/INFO]: Done")
					|| (message.contains("[minecraft/DedicatedServer]: Done")))
					&& message.contains("! For help, type \"help\"")) {
				
				CD.addServer(servers.get(server));
				
			} else if (message.contains(" joined the game")) {
				
				CD.addPlayer(message.split(":")[3].trim().replace(" joined the game", ""));
				if(servers.get(server).isOnGlobal()) {
					sendAllMinusOne(server, "/tellraw @a {\"text\":\"" + message.split(":")[3].trim().split(" ")[0]
						+ " joined " + server + " server\",\"color\":\"yellow\"}");
					sendLoginToPlayer(server, message.split(":")[3].trim().split(" ")[0]);
				}
				CD.addToLog(message.split(":")[3].trim());
				
				servers.get(server).addPlayer(message.split(":")[3].trim().split(" ")[0]);
				bot.changeActiveCount(CD.getPlayerCount());
				bot.sendMessage(message.split(":")[3].trim().split(" ")[0] + " joined " + server + " server");
			} else if (message.contains(" left the game")) {
				
				CD.removePlayer(message.split(":")[3].trim().replace(" left the game", ""));
				if(servers.get(server).isOnGlobal())
					sendAllMinusOne(server, "/tellraw @a {\"text\":\"" + message.split(":")[3].trim().split(" ")[0]
						+ " left " + server + " server\",\"color\":\"yellow\"}");
				CD.addToLog(message.split(":")[3].trim());
				servers.get(server).removePlayer(message.split(":")[3].trim().split(" ")[0]);
				bot.changeActiveCount(CD.getPlayerCount());
				bot.sendMessage(message.split(":")[3].trim().split(" ")[0] + " left " + server + " server");
				
			} else if (message.contains(" <") && message.contains("> ") && !message.contains("[SUCCESS] Line")) {
				
				CD.addToLog(message.split(":")[3].trim());
				bot.sendMessage(server + " server:" + message.replace(message.split(":")[0], "").replace(message.split(":")[1], "").replace(message.split(":")[2], "").replace(":::", "").trim());
				if(servers.get(server).isOnGlobal())
					sendAllMinusOne(server, "tellraw @a {\"text\":\"" + message.replace(message.split(":")[0], "").replace(message.split(":")[1], "").replace(message.split(":")[2], "").replace(":::", "").trim() + "\"}");
			}
		}
	}
	
	public void processMessageFromConsole(String line) {
		
		CD.addCommandLog("Command:" + line);
		
		
		switch (line.split(" ")[0]) {
		//clear the console
		case "clear":
			CD.clearCommandLog();
			break;
		//sends a help message to the console
		case "help":
			CD.addCommandLog("clear: clears command log");
			CD.addCommandLog("select <server name>: Selects a server for focus");
			CD.addCommandLog("generateFSO: generates a file server config for the focused server");
			CD.addCommandLog("remove <player name>: removes player from list of players");
			CD.addCommandLog("startFSO: starts file server for focused server");
			CD.addCommandLog("stopFSO: stops file server for focused server");
			CD.addCommandLog("start <server name> <server jar location> <start command||start.bat>: starts the server"
					+ "\n\tserver name: name of the server"
					+ "\n\tjar location: location of the server.jar"
					+ "\n\tstart command or start.bat: start the server with these params, or just use this bat file");
			CD.addCommandLog("start <start file locaiton>: reads the start file line by line starting the servers within");
			CD.addCommandLog("stopall: stops all servers");
			CD.addCommandLog("stop: stops focused server");
			CD.addCommandLog("toggleChat: toggles global chat for focused server");
			CD.addCommandLog("options: shows what the check boxes mean in the options column");
			CD.addCommandLog("! <command>: Sends command to focused server");
			break;
		//select a new focus for the server
		case "select":
			if(CD.setSelectedIndex(line.replace("select ", ""))) {
				CD.addCommandLog("Server focus now on server:" + line.replace("select ", ""));
			}else {
				CD.addCommandLog("There is no server with that index");
			}
			break;
		//generates File server options
		case "generateFSO":
			if(CD.getSelected()!= null) {
				File properties = new File(CD.getSelected().getServerFile().getParentFile().getAbsolutePath() + "\\modmanager.properties");
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
			CD.removePlayer(line.split(" ")[1]);
			CD.drawDisplay();
			bot.changeActiveCount(CD.getPlayerCount());
			break;
		//starts file server
		case "startFSO":
			if(CD.getSelected()!= null) {
				File properties = null;
				for(File x : CD.getSelected().getServerFile().getParentFile().listFiles()) {
					if(x.getName().equals("modmanager.properties")) {
						properties = x;
						break;
					}
				}
				if(properties != null) {
					CD.getSelected().startFileHosting(properties);
					CD.drawDisplay();
				}else {
					CD.addCommandLog("Unable to find modmanager.properties. Please generate with \"generateFSO\" command.");
				}
			}
			break;
			
			//stops file server
					case "stopFSO":
						if(CD.getSelected()!= null) {
							CD.addCommandLog("Stopping file hosting");
							CD.getSelected().stopFileHosting();
							CD.drawDisplay();
						}
						break;
		//start a server
		case "start":
			startServerFromCommandLine(line.replace("start ", ""));
			break;
		case "stopall":
			CD.addCommandLog("Sending stop commands to all servers");
			servers.forEach((key, value) -> {
				value.stopFileHosting();
				value.sendCommand("stop\n");
			});
			break;
		case "stop":
			CD.addCommandLog("Sending stop commands to stop focused server");
			if(CD.getSelected()!= null) {
				CD.getSelected().stopFileHosting();
				CD.getSelected().sendCommand("stop\n");
			}
			break;
		case "toggleChat":
			if(CD.getSelected() != null) {
				CD.addCommandLog("Toggling global chat for focused server");
				CD.getSelected().setOnGlobal(!CD.getSelected().isOnGlobal());
				CD.drawDisplay();
			}
			break;
		case "options":
			CD.addCommandLog("1: If the server is on global chat connect");
			CD.addCommandLog("2: If the server has a file sync server running");
			break;
		case "!":
			if(CD.getSelected()!= null) {
				CD.getSelected().sendCommand(line.replace("! ", "") + "\n");
				awaitingCommand = true;
			}
			break;
		case "\n":
			CD.drawDisplay();
			break;
		default:
			CD.addCommandLog("Unknown command. Type \"help\" for help.");
			break;
		}
	}
	
	public void processMessageFromDiscord(String message) {
		sendAll("/tellraw @a {\"text\":\"" + message + "\"}");
		CD.addToLog(message);
	}

	private void sendLoginToPlayer(String serverName, String player) {
		Server server = servers.get(serverName);
		server.sendCommand(
				"/tellraw " + player + " {\"text\":\"***********************************************\",\"color\":\"dark_blue\"}\n");
		server.sendCommand("/tellraw " + player
				+ " {\"text\":\"*This server is on global chat*\",\"bold\":true,\"color\":\"dark_blue\"}\n");
		server.sendCommand(
				"/tellraw " + player + " {\"text\":\"***********************************************\",\"color\":\"dark_blue\"}\n");
		server.sendCommand("/tellraw " + player + " {\"text\":\"Current population:" + CD.getPlayerCount()
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
		CD.addCommandLog(message);
	}
	
	private void sendAllMinusOne(String server, String message) {
		for (String key : servers.keySet()) {
			if (!key.equals(server) && servers.get(key).isOnGlobal()) {
				servers.get(key).sendCommand(message + "\n");
			}
		}
	}
	
	public void closeServer(String server) {
		CD.removeServer(server);
		for (String player : servers.get(server).getPlayers()) {
			CD.removePlayer(player);
		}
	}
	
	public void startServer(File serverFile, String startCommand, String serverName) {

		CD.addCommandLog("Starting " + serverName + " server");
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
				CD.addCommandLog("Server file cannot be found");
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
				CD.addCommandLog("Server file cannot be found");
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
				CD.addCommandLog("Invalid command");
			}
		}else {
			CD.addCommandLog("Invalid command");
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
