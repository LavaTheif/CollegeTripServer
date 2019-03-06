package io.github.lavatheif.CollegeTripServer;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;

//import org.eclipse.jetty.server.Server;
//import org.eclipse.jetty.server.handler.ContextHandler;
//import org.eclipse.jetty.server.handler.ContextHandlerCollection;
//import org.eclipse.jetty.websocket.server.WebSocketHandler;
//import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory;

import io.github.lavatheif.CollegeTripServer.sockets.SocketConnection;

public class _INIT_ extends Utils {

	public _INIT_() throws Exception {
		// Load Config Details
		File config = new File(System.getProperty("user.home")+"/trip-planner.conf");
		
		if (!config.exists()) {
			String[] config_default = {"##WARNING: Changing the port the server runs on means you will need to edit the 'SERVER_PORT' variable in the client program, and recompile it.",
							"SERVER_PORT: 25000",
							"DB_IP: localhost",
							"DB_PORT: 3306",
							"##DB account details",
							"DB_USERNAME: \"test_user\"",
							"DB_PASSWORD: \"Password123_\"",
							"##email account details",
							"EMAIL_USERNAME: \"trips\"",
							"EMAIL_PASSWORD: \"Password123_\"",
							"##staff who initially approve the trip",
							"LEVEL_1_STAFF: [\"LBE\", \"ACR\"]",
							"##final approval staff member",
							"LEVEL_2_STAFF: \"BRF\"",
							"##username of the college calendar manager",
							"CALENDAR_MANAGER: \"DCO\""
							};
			String[] DB_SETUP = {"CREATE DATABASE trips;",
					"USE trips;",
					"CREATE TABLE trips(id INT, creator VARCHAR(10), location VARCHAR(100), address TEXT, date_start VARCHAR(20), time_start VARCHAR(15), end VARCHAR(15), is_residential BOOLEAN, purpose TEXT, max_students INT, staff TEXT, groups TEXT, transport VARCHAR(100), cost INT, finance_report VARCHAR(10), parent_letter VARCHAR(10), risk_assessment VARCHAR(10), initial_approvals TEXT, approved BOOLEAN);",
					"CREATE TABLE users(id INT, email VARCHAR(30), trips TEXT, token VARCHAR(30));",
					"--please import all users into the users table.  'id' can be anything, but email must end with @woking.ac.uk"
			};
			config.createNewFile();
			FileWriter f = new FileWriter(config);
			PrintWriter pw = new PrintWriter(f);
			for(String i : config_default)
				pw.println(i);
			pw.close();
			
			System.out.println("Created new config at "+config.getAbsolutePath());
			
			File DB_SETUP_FILE = new File(System.getProperty("user.home")+"/db_setup.txt");
			DB_SETUP_FILE.createNewFile();
			f = new FileWriter(DB_SETUP_FILE);
			pw = new PrintWriter(f);
			for(String i : DB_SETUP)
				pw.println(i);
			pw.close();
			
			System.out.println("Created DB_SETUP file at "+DB_SETUP_FILE.getAbsolutePath());
		}
		
		String[] config_data = readFile(config).replaceAll("##.*\n", "").split("\n");
		HashMap<String, String> configContents = new HashMap<>();
		
		for(String data : config_data){
			String key = data.split(":")[0];
			String val = data.replaceAll("[a-zA-Z0-9_]*:", "");
			configContents.put(key, val.replaceAll("\"", "").trim());
		}

		/*
		 * Config details:
		 * 
		 * 
		 * PORT, ip, username, password email login details admin accounts
		 * higher admin accounts
		 */
		
		Utils.PORT = Integer.parseInt(configContents.get("SERVER_PORT"));
		Utils.DB_PORT = Integer.parseInt(configContents.get("DB_PORT"));
		Utils.DB_IP = (configContents.get("DB_IP"));
		Utils.DB_USER = (configContents.get("DB_USERNAME"));
		Utils.DB_PASS = (configContents.get("DB_PASSWORD"));
		Utils.EMAIL_USER = (configContents.get("EMAIL_USERNAME"));
		Utils.EMAIL_PASS = (configContents.get("EMAIL_PASSWORD"));
		Utils.CALENDAR_MANAGER = (configContents.get("CALENDAR_MANAGER"));
		
		initDBs();
		
		//Load admins from config
		ArrayList<String> admUsrnms = new ArrayList<>();
		String admnData = configContents.get("LEVEL_1_STAFF").replaceAll("\\[|\\]|\\\"", "");

		for(String admn : admnData.split(","))
			admUsrnms.add(admn.trim());

		String finalAdmn = configContents.get("LEVEL_2_STAFF");
		
		ArrayList<Object> obj;
		for(String s : admUsrnms){
			obj = new ArrayList<>();
			obj.add(s+"@woking.ac.uk");
			int id = Integer.parseInt(getFirst("SELECT id FROM users WHERE email=?;", obj).get("id")+"");
			admins.add(id);
			lookup_admins.put(id, s);
		}
		
		obj = new ArrayList<>();
		obj.add(finalAdmn+"@woking.ac.uk");
		finalAdmin = Integer.parseInt(getFirst("SELECT id FROM users WHERE email=?;", obj).get("id")+"");
		lookup_admins.put(finalAdmin, finalAdmn);

		// Set up sockets for desktop clients to connect to.
		setUpSocket();

		// We currently aren't using this.
		// Set up sockets for websites to connect to.
		// setUpWebSocket();
	}

	private void setUpSocket() {
		// start them on a new thread.
		new Thread(new SocketConnection()).start();
	}

	// This code would set up a websocket, but we arent using it atm.
	// It requires Jetty, which can be added using maven, however the
	// college systems dont allow maven for some reason.
	{
		// private void setUpWebSocket(){
		// // Set up listeners for clients.
		// try {
		// //Init the server
		// Server clientConnect = new Server(PORT);
		// ContextHandlerCollection contexts = new ContextHandlerCollection();
		// String url = "/college-trips";
		//
		// //Set data for the socket
		// WebSocketHandler wsHandler = new WebSocketHandler() {
		// @Override
		// public void configure(WebSocketServletFactory factory) {
		// //Adds the socket handler class to to socket
		// factory.register(WebSocketConnection.class);
		// // Times out after 5 seconds
		// factory.getPolicy().setIdleTimeout(5000);
		// }
		// };
		//
		// ContextHandler context = new ContextHandler();
		//// Add the path for the socket
		// context.setContextPath(url);
		// context.setHandler(wsHandler);
		// contexts.addHandler(context);
		// clientConnect.setHandler(contexts);
		//
		// log("Listening on port "+PORT);
		// clientConnect.start();//Start running
		// } catch (Exception e) {
		// //An error has occurred
		// log("\n###\n\nAn error occoured while setting up server on port " +
		// PORT + "\n\n###");
		// log(e.getStackTrace());
		// System.exit(1);
		// }
		// }
	}
}
