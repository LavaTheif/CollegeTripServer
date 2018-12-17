package io.github.lavatheif.CollegeTripServer;
/**
 * @author Charlie (LavaTheif)
 * https://lavatheif.github.io/
 * 
 * @description
 * This is the server for the colleges Trip Planner application
 */

import java.io.File;

public class Main {
	public static void main(String[] args) {
		// Load the config file.
		// For debugging:
		// TODO Remove. Maybe make it a set location
		args = new String[1];
		args[0] = "C:/Users/Charlie/Documents/serverApp.config";

		if (args.length == 0) {// Config path not specified.
			// Might make it a specific location, and just copy a default one to
			// the location if it doesnt exist.
			System.out.println("Please run the server with the command:");
			System.out.println("java -jar server.jar C:\\path\\to\\.config");
			return;
		}

		try {
			// Initialise the server, passing the config file to it.
			new _INIT_(new File(String.join(" ", args)));// Initialise the
															// server
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
	}
}
