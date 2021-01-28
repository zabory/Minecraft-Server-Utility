package app;

import managers.ServerManager;

public class app {
	
	/**
	 * TODO add the ability to switch screens to the consoles of individual and file servers
	 */

	public static void main(String[] args) {
		ServerManager SM = new ServerManager();
		if(args.length > 0) {
			SM.startServerFromCommandLine(args[0]);
		}
	}

}
