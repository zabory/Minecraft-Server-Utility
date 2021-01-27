package app;

import managers.ServerManager;

public class app {
	
	/**
	 * TODO add the ability to switch screens to the consoles of individual and file servers
	 * TODO Discord chat integration 
	 * TODO let the user know that chat is monitored
	 * TODO let the user on join know who else is on the other servers
	 */

	public static void main(String[] args) {
		ServerManager SM = new ServerManager();
		if(args.length > 0) {
			SM.startServerFromCommandLine(args[0]);
		}
	}

}
