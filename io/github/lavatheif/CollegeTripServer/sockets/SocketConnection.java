package io.github.lavatheif.CollegeTripServer.sockets;

import java.net.ServerSocket;
import java.net.Socket;

import io.github.lavatheif.CollegeTripServer.Utils;

public class SocketConnection implements Runnable {

	public void run() {
		try {
			boolean running = true;
			ServerSocket server = new ServerSocket(Utils.PORT);//TODO make a var in conf
			while(running){
				Socket client = server.accept();
				new Thread(new SocketHandler(client)).start();
			}
			server.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
