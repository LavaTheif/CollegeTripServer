package io.github.lavatheif.CollegeTripServer.sockets;

import java.net.ServerSocket;
import java.net.Socket;

import io.github.lavatheif.CollegeTripServer.Utils;

public class SocketConnection implements Runnable {

	public void run() {
		try {
			boolean running = true;
			// start the server on PORT
			// a var in conf
			ServerSocket server = new ServerSocket(Utils.PORT);

			while (running) {// while the app is running
				Socket client = server.accept();// if a client connects,
				// pass them to the socket handler via a new thread, so that
				// more clients can still connect.
				new Thread(new SocketHandler(client)).start();
			}
			server.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
