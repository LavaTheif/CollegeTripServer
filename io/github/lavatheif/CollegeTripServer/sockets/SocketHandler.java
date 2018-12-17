package io.github.lavatheif.CollegeTripServer.sockets;

import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

import io.github.lavatheif.CollegeTripServer.Utils;

public class SocketHandler extends Utils implements Runnable {

	private Socket client;

	public SocketHandler(Socket client) {
		// Saves the client in this class
		this.client = client;
	}

	public void run() {
		// Client has connected
		// System.out.println("Connection");
		try {
			// When a client connects, they will always send a
			// string and expect a response to that

			// init vars
			PrintWriter pw = new PrintWriter(client.getOutputStream());
			Scanner scan = new Scanner(client.getInputStream());

			// evaluate their request, and generate a response.
			String response = evaluate(scan.nextLine());

			// send the response to the client.
			pw.println(response);
			pw.flush();

			// Close IO streams
			pw.close();
			scan.close();
			// Client has disconnected
			// System.out.println("Closed");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
