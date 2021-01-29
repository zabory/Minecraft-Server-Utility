package consoleUtilities;

import java.io.IOException;
import java.util.LinkedList;

public class ServerConsoleDisplay extends Display {
	
	private static final int LOG_MEMORY_SIZE = 200;
	
	LinkedList<String> log;
	
	public ServerConsoleDisplay() {
		log = new LinkedList<String>();
	}
	
	public void addToLog(String line) {
		if(log.size() < LOG_MEMORY_SIZE) {
			log.add(line);
		}else {
			log.removeFirst();
			log.add(line);
		}
		if(isFocused()) {
			System.out.println(line);
		}
	}

	@Override
	void drawDisplay() {
		if (isFocused()) {
			// clear display
			try {
				new ProcessBuilder("cmd", "/c", "cls").inheritIO().start().waitFor();
			} catch (InterruptedException | IOException e) {
				e.printStackTrace();
			}
			
			for(String x : log) {
				System.out.println(x);
			}
		}
	}
}
