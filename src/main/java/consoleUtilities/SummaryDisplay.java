package consoleUtilities;

import java.io.IOException;
import java.util.LinkedList;

import dataStructures.Server;

public class SummaryDisplay extends Display {

	private final static int LOG_LENGTH = 10;
	private final static int SERVER_COL_SIZE = 3;
	private final static int PLAYER_COL_SIZE = 3;
	private final static int LOG_COL_SIZE = 8;
	private final static int OPTION_COL_SIZE = 1;

	private LinkedList<String> players;
	private LinkedList<Server> servers;
	private LinkedList<String> log;
	private LinkedList<String> commandLog;

	private int selectedIndex;

	public SummaryDisplay() {
		players = new LinkedList<String>();
		servers = new LinkedList<Server>();
		log = new LinkedList<String>();
		commandLog = new LinkedList<String>();
		selectedIndex = -1;
	}

	public void addServer(Server server) {
		servers.add(server);
		drawDisplay();
	}

	public void removeServer(String server) {
		for (Server x : servers) {
			if (x.getServerName().equals(server)) {
				servers.remove(x);
				break;
			}
		}

		setSelectedIndex(selectedIndex);
		drawDisplay();
	}

	public int getPlayerCount() {
		return players.size();
	}

	public void addPlayer(String player) {
		players.add(player);
		drawDisplay();
	}

	public void removePlayer(String player) {
		players.remove(player);
		drawDisplay();
	}

	public void addCommandLog(String line) {
		commandLog.add(line);
		drawDisplay();
	}

	public Server getSelected() {
		if (selectedIndex != -1) {
			return servers.get(selectedIndex);
		}
		return null;
	}

	public void clearCommandLog() {
		commandLog = new LinkedList<String>();
		drawDisplay();
	}

	public boolean setSelectedIndex(int selectedIndex) {
		if (selectedIndex < servers.size()) {
			this.selectedIndex = selectedIndex;
			drawDisplay();
			return true;
		}
		selectedIndex = -1;
		drawDisplay();
		return false;
	}

	public boolean setSelectedIndex(String name) {
		for (int i = 0; i < servers.size(); i++) {
			if (servers.get(i).getServerName().equals(name)) {
				selectedIndex = i;
				return true;
			}
		}
		return false;
	}

	public void addToLog(String message) {
		if (log.size() >= LOG_LENGTH) {
			log.removeFirst();
			log.add(message);
		} else {
			log.add(message);
		}
		drawDisplay();
	}

	public synchronized void drawDisplay() {
		if (isFocused()) {
			// clear display
			try {
				new ProcessBuilder("cmd", "/c", "cls").inheritIO().start().waitFor();
				System.out.println("");
			} catch (InterruptedException | IOException e) {
				e.printStackTrace();
			}

			printHead("Server", SERVER_COL_SIZE);
			System.out.print(" ");
			printHead("Options", OPTION_COL_SIZE);
			System.out.print("\t");
			printHead("Players", 3);
			System.out.println();

			for (int i = 0; i < servers.size() + 1 || i < players.size() + 1; i++) {

				if (servers.size() > 0 && i < servers.size()) {
					if (i == selectedIndex) {
						printCol(">" + servers.get(i).getServerName(), SERVER_COL_SIZE);
						System.out.print(" ");
						printCol("[" + (servers.get(i).isOnGlobal() ? "x" : " ") + "] ["
								+ (servers.get(i).isFileHostingOn() ? "x" : " ") + "]", OPTION_COL_SIZE);
					} else {
						printCol(" " + servers.get(i).getServerName(), SERVER_COL_SIZE);
						System.out.print(" ");
						printCol("[" + (servers.get(i).isOnGlobal() ? "x" : " ") + "] ["
								+ (servers.get(i).isFileHostingOn() ? "x" : " ") + "]", OPTION_COL_SIZE);
					}
				} else if (i == servers.size()) {
					printEnd(SERVER_COL_SIZE);
					System.out.print(" ");
					printEnd(OPTION_COL_SIZE);
				} else if (i > servers.size()) {
					for (int j = 0; j < SERVER_COL_SIZE; j++) {
						System.out.print("\t");
					}
					System.out.print("\t");
				}

				System.out.print("\t");

				// add stuff for players

				if (players.size() > 0 && i < players.size()) {

					printCol(" " + players.get(i), PLAYER_COL_SIZE);

				} else if (i == players.size()) {
					printEnd(PLAYER_COL_SIZE);
				} else if (i > players.size()) {
					for (int j = 0; j < PLAYER_COL_SIZE; j++) {
						System.out.print("\t");
					}
				}

				System.out.println("");
				// this ends the top display loop
			}

			printHead("Log", LOG_COL_SIZE);
			System.out.println("");
			for (int i = 0; i < log.size(); i++) {
				printCol(" " + log.get(i), LOG_COL_SIZE);
				System.out.println();
			}

			printEnd(LOG_COL_SIZE);
			System.out.println("");

			for (String x : commandLog) {
				System.out.println(x);
			}

			System.out.print("Command:");
		}
	}

}
