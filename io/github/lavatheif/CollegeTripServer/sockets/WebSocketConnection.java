package io.github.lavatheif.CollegeTripServer.sockets;

//import java.io.IOException;
//
//import org.eclipse.jetty.websocket.api.Session;
//import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
//import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
//import org.eclipse.jetty.websocket.api.annotations.OnWebSocketError;
//import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
//import org.eclipse.jetty.websocket.api.annotations.WebSocket;
//
import io.github.lavatheif.CollegeTripServer.Utils;
//
//@WebSocket
public class WebSocketConnection extends Utils {
	
	//This is the class that handles websocket connections.
	//It is currently not used, however should work if all
	//websocket code is uncommented, and Jetty is added.
	
//	
//	@OnWebSocketClose
//	public void onClose(Session session, int statusCode, String reason) {
//	}
//
//	@OnWebSocketError
//	public void onError(Throwable t) {
//		if (t.getMessage() != null)
//			System.out.println("Error: " + t.getMessage());
//	}
//
//	@OnWebSocketConnect
//	public void onConnect(Session session) {
//	}
//
//	@OnWebSocketMessage
//	public void onMessage(Session client, String message) {
//		try {
//			System.out.println(message);
//			client.getRemote().sendString(evaluate(message));
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
//	}

}
