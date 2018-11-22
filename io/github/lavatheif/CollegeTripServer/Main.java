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
	public static void main(String[] args){
		//For debugging:
		//TODO Remove.  Maybe make it a set location
		args = new String[1];
		args[0] = "C:/Users/Charlie/Documents/serverApp.config";
		
		if(args.length==0){
			System.out.println("Please run the server with the command:");
			System.out.println("java -jar server.jar C:\\path\\to\\.config");
			return;
		}
		try{
			new _INIT_(new File(String.join(" ", args)));//Initialise the server
		}catch(Exception e){
			e.printStackTrace();
			System.exit(1);
		}
	}
}
