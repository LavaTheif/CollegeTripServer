package io.github.lavatheif.CollegeTripServer;
/**
 * @author Charlie (LavaTheif)
 * https://lavatheif.github.io/
 * 
 * @description
 * This is the server for the colleges Trip Planner application
 */

public class Main {
	public static void main(String[] args) {
		try {
			// Initialise the server, passing the config file to it.
			new _INIT_();// Initialise the server
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
	}
}
