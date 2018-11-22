package io.github.lavatheif.CollegeTripServer.sockets;

import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

import io.github.lavatheif.CollegeTripServer.Utils;

public class SocketHandler extends Utils implements Runnable{

	Socket client;
	
	public SocketHandler(Socket client) {
		this.client = client;
	}

	public void run() {
		System.out.println("Connection");
		try{
			//When a client connects, they will send a string and expect a response to that
			PrintWriter pw = new PrintWriter(client.getOutputStream());
			Scanner scan = new Scanner(client.getInputStream());
		
			String response = evaluate(scan.nextLine());
			pw.println(response);
			pw.flush();
			
			//Close IO streams
			pw.close();
			scan.close();
			System.out.println("Closed");
		}catch(Exception e){
			e.printStackTrace();
		}		
	}

}
