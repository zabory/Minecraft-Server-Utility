package dataStructures;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Scanner;

import consoleUtilities.SummaryDisplay;

public class FileSyncServer extends Thread {

	private File properties;
	private boolean running;
	private ServerSocket serverSocket;
	private SummaryDisplay CD;
	// list of directories to sync and files to sync
	LinkedList<File> dirSync = new LinkedList<File>();
	LinkedList<File> fileSync = new LinkedList<File>();
	LinkedList<String> ignore = new LinkedList<String>();
	
	File forge;

	public FileSyncServer(File properties, SummaryDisplay CD) {
		this.properties = properties;
		this.CD = CD;
		running = true;

		int port = 0;

		try {
			Scanner propertiesInput = new Scanner(properties);

			// go through the config file
			while (propertiesInput.hasNextLine()) {
				String line = propertiesInput.nextLine();
				if (line.contains("port=")) {
					port = Integer.parseInt(line.replace("port=", ""));
				} else if (line.contains("dirs_to_sync=")) {
					// make new files for the dirs
					for (String dir : line.replace("dirs_to_sync=", "").replace(" ", "").split(",")) {
						CD.addCommandLog("Adding dir to sync:" + dir);
						dirSync.add(new File(dir));
					}
				} else if (line.contains("ignore=")) {
					for (String x : line.replace("ignore=", "").replace(" ", "").split(",")) {
						CD.addCommandLog("Ignoring file:" + x);
						ignore.add(x);
					}
				}
			}

			propertiesInput.close();

		} catch (FileNotFoundException e) {
			
		}

		// goes through all the dirs
		for (File x : dirSync) {
			x = new File(properties.getParentFile().getAbsolutePath() + "\\" + x.getPath());
			// if the directory actually exists, add all the files to the file list
			if (x.exists()) {
				fileSync.addAll(getFiles(x, ignore));
			}
		}
		
		for(int i = 0; i < fileSync.size(); i++) {
			fileSync.set(i, new File(fileSync.get(i).getAbsolutePath().replace(properties.getParentFile().getAbsolutePath(), "")));
		}
		
		for(File x : fileSync) {
			System.out.println(x.getPath());
		}

		// remove files that we dont want to sync
		for (int i = 0; i < fileSync.size(); i++) {
			File x = fileSync.get(i);
			if (ignore.contains(x.getName())) {
				fileSync.remove(x);
				i--;
			} else {
			}
		}

		// get forge file
		forge = null;

		for (File x : properties.getParentFile().listFiles()) {
			if (x.getName().contains("forge") && x.getName().contains("installer")) {
				forge = new File(x.getName());
				break;
			}
		}

		if (forge == null) {
			CD.addCommandLog("Please provide forge installer");
			running = false;
		} else {
			CD.addCommandLog("Registering forge version: "
					+ forge.getName().replace("forge-", "").replace("-installer.jar", ""));
		}

		try {
			if(forge != null) {
				CD.addCommandLog("Starting file server on port:" + port);
				serverSocket = new ServerSocket(port);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	public void run() {
		CD.addCommandLog("Waiting for connections...");
		while (running) {
			try {
				new ClientHandler(serverSocket.accept(), dirSync, fileSync, forge).start();
			} catch (IOException e) {
			}
		}
	}

	private ArrayList<File> getFiles(File dir, LinkedList<String> ignore) {
		ArrayList<File> files = new ArrayList<File>();
		for (File x : dir.listFiles()) {
			if (x.isDirectory() && !ignore.contains(x.getName())) {
				files.addAll(getFiles(x, ignore));
			} else if (!x.isDirectory()) {
				files.add(x);
			}
		}
		return files;
	}

	public void stopServer() {
		running = false;
		try {
			serverSocket.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	

	public boolean isRunning() {
		return running;
	}



	private class ClientHandler extends Thread {

		private Socket client;
		private LinkedList<File> dirSync;
		private LinkedList<File> fileSync;
		private File forge;

		public ClientHandler(Socket client, LinkedList<File> dirSync, LinkedList<File> fileSync, File forge) {
			this.client = client;
			this.dirSync = dirSync;
			this.fileSync = fileSync;
			this.forge = forge;

		}

		public void run() {

			try {
				PrintStream output = new PrintStream(client.getOutputStream());
				BufferedReader input = new BufferedReader(new InputStreamReader(client.getInputStream()));

				// Send forge version
				output.println(forge.getName());

				// listen if we need to send it
				if (Boolean.parseBoolean(input.readLine())) {
					sendFile(forge);
				}

				output.println("Pixelmon");
				output.println(5);

				// Send how many files to sync
				output.println(fileSync.size());

				for (File x : dirSync) {
					output.println("d," + x.getName());
				}

				for (File x : fileSync) {
					sendFile(x);
				}

				output.println("c");

				input.close();
				output.close();
				client.close();
			} catch (IOException e) {
			}

		}

		private void sendFile(File x) throws IOException {
			
			x = new File(properties.getParentFile().getAbsolutePath() + "\\" + x.getPath());
			
			PrintStream output = new PrintStream(client.getOutputStream());
			BufferedReader input = new BufferedReader(new InputStreamReader(client.getInputStream()));
			boolean isCorrect = false;
			while (!isCorrect) {
				output.println("f," + x.getPath().replace(properties.getParentFile().getAbsolutePath() + "\\", "") + "," + x.hashCode() + "," + x.length());
				String reply = input.readLine();
				if (reply.equals("true")) {

					byte[] bytes = new byte[(int) x.length()];
					BufferedInputStream fileInput = new BufferedInputStream(new FileInputStream(x));
					fileInput.read(bytes, 0, bytes.length);

					output.println(bytes.length);
					input.readLine();

					client.getOutputStream().write(bytes, 0, bytes.length);

					fileInput.close();

					input.readLine();
				}
				isCorrect = Boolean.parseBoolean(input.readLine());
			}
		}
	}
}
