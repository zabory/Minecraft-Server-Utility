package app;

import managers.ServerManager;

public class app {
	
	/**
	 * TODO use parameters when starting a server instead of regular way
	 * TODO start status updates
	 */

	public static void main(String[] args) {
		ServerManager SM = new ServerManager();
		if(args.length > 0) {
			SM.startServerFromCommandLine(args[0]);
		}
	}

}
