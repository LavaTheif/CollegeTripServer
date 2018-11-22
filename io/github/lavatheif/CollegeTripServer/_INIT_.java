package io.github.lavatheif.CollegeTripServer;

import java.io.File;
import java.util.HashMap;

//import org.eclipse.jetty.server.Server;
//import org.eclipse.jetty.server.handler.ContextHandler;
//import org.eclipse.jetty.server.handler.ContextHandlerCollection;
//import org.eclipse.jetty.websocket.server.WebSocketHandler;
//import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory;

import io.github.lavatheif.CollegeTripServer.sockets.SocketConnection;

public class _INIT_ extends Utils {

	public _INIT_(File config) throws Exception {
		// Load Config Details
		if (!config.exists()) {//TODO change
			setUpSocket();
			return;
//			throw new Exception("Config does not exist at this location.");
		}
		HashMap<String, String> configContents = stringToJSON(readFile(config));

		System.out.println(configContents.get("test"));
		/* TODO Config details:
		 * 
		 * PORT
		 * email stuff
		 */
		
		//Set up sockets for desktop clients to connect to.
		setUpSocket();
		
		//Set up sockets for websites to connect to.
//		setUpWebSocket();
	}
	
	
	private void setUpSocket() {
		new Thread(new SocketConnection()).start();
	}


//	private void setUpWebSocket(){
//		// Set up listeners for clients.
//		try {
//			//Init the server
//			Server clientConnect = new Server(PORT);
//			ContextHandlerCollection contexts = new ContextHandlerCollection();
//			String url = "/college-trips";
//			
//			//Set data for the socket
//			WebSocketHandler wsHandler = new WebSocketHandler() {
//				@Override
//				public void configure(WebSocketServletFactory factory) {
//					//Adds the socket handler class to to socket
//					factory.register(WebSocketConnection.class);
//					// Times out after 5 seconds
//					factory.getPolicy().setIdleTimeout(5000);
//				}
//			};
//			
//			ContextHandler context = new ContextHandler();
////			Add the path for the socket
//			context.setContextPath(url);
//			context.setHandler(wsHandler);
//			contexts.addHandler(context);
//			clientConnect.setHandler(contexts);
//			
//			log("Listening on port "+PORT);
//			clientConnect.start();//Start running
//		} catch (Exception e) {
//			//An error has occurred
//			log("\n###\n\nAn error occoured while setting up server on port " + PORT + "\n\n###");
//			log(e.getStackTrace());
//			System.exit(1);
//		}
//	}
}
