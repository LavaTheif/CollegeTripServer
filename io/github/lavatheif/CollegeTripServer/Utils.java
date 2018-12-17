package io.github.lavatheif.CollegeTripServer;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.Scanner;

import com.google.gson.Gson;

public class Utils {
	public static final int PORT = 25000;

	/**
	 * A function that takes the message from the client as an input, and
	 * returns a string to respond with.
	 * 
	 * @param message
	 * @return response
	 */
	public String evaluate(String message) {
		// Initialise variables
		HashMap<String, String> data = stringToJSON(message);
		String response;
		String request = data.get("request").toLowerCase();

		// Check if they want to log in. If not, check if they already are.
		if (request.equalsIgnoreCase("login")) {
			response = logIn(data);// Logs user in
			return response;
		}
		// if users token and id dont match, tell them to log in.
		if (!validateUser(data.get("token"), data.get("id")))
			return "{\"valid\":\"false\", \"errMsg\":\"Invalid token.  Please log in.\"}";

		// Start processing the various requests.
		switch (request) {
		case ("trips"):// returns all trips that the user is able to view
			/*
			 * Returns: trip ID : {Trip location, creator, isAccepted}
			 */
			response = getTrips(data);
			break;
		case ("new trip"):// Starts making a new trip
			response = initTrip(data);
			break;
		case ("set data"):// Sets the variables the user enters
			response = setTripDetails(data);
			break;
		case ("file upload"):// Sets the variables the user enters
			alterDataBase("UPDATE trips SET approved=false WHERE id=" + data.get("tripID") + ";");
			response = "{\"valid\":\"true\", \"errMsg\":\"//TODO.\"}";
			break;
		default:
			response = "{\"valid\":\"false\", \"errMsg\":\"Invalid data.\"}";
			break;
		}
		return response;
	}

	private String getTrips(HashMap<String, String> data) {
		HashMap<String, String> reply = new HashMap<>();
		// TODO Get ids from user data??? allows us to choose which ones to
		// return
		// (eg for load more and capping limit)
		// Should we return the trip date?
		// incomplete: red
		// not approved: orange
		// approved, but not completed: yellow
		// completed: green
		List<HashMap<String, Object>> l = queryDataBase(
				"SELECT location, approved, id, date_start FROM trips WHERE creator = " + data.get("id") + ";");

		// TODO get trips from users json section (change to text)

		for (HashMap<String, Object> m : l) {
			// loop through all trips and add their details to the list.
			HashMap<String, String> tripData = new HashMap<>();
			tripData.put("location", m.get("location") + "");
			tripData.put("approved", m.get("approved") + "");
			tripData.put("date_start", m.get("date_start") + "");
			tripData.put("creator",
					("" + getFirst("SELECT email FROM users WHERE id=" + data.get("id") + ";").get("email"))
							.split("@")[0] + "");
			reply.put(m.get("id") + "", new Gson().toJson(tripData));
		}
		reply.put("valid", "true");

		return new Gson().toJson(reply);
	}

	private String logIn(HashMap<String, String> data) {
		// Check email provided is valid
		String email = data.get("email");
		if (!email.contains("@woking.ac.uk")) {
			return "{\"valid\":\"false\", \"errMsg\":\"Please use a valid woking college E-Mail.\"}";
		}
		// Email valid

		// TODO Check password against college systems
		// if pw is correct, continue

		// pull account from DB
		HashMap<String, Object> user = getFirst("SELECT id FROM users WHERE email = \"" + email + "\";");

		if (user == null)
			return "{\"valid\":\"false\", \"errMsg\":\"Invalid account.\"}";

		int id = (int) user.get("id");
		String token = generateToken();

		// save token to DB and send data to client, allowing the login.
		alterDataBase("UPDATE users SET token=\"" + token + "\" WHERE id=" + id + ";");
		return "{\"valid\":\"true\", \"token\":\"" + token + "\", \"id\":\"" + id + "\"}";
	}

	private String generateToken() {
		// This generates a random token for authorisation.
		String token = "";
		for (int i = 0; i < 15; i++) {
			// TODO Make it properly
			Random random = new Random();
			token += random.toString().charAt(random.nextInt(random.toString().length()));
		}
		return token;
	}

	private boolean validateUser(String token, String id) {
		// check if user has this token. If not, its invalid.
		// TODO Tokens are valid for 30 mins??, and are reset on login
		if (token == null || id == null)// no token exists
			return false;

		// makes sure users ID is correct
		Integer.parseInt(id);// checks its valid

		HashMap<String, Object> data = getFirst("SELECT token FROM users WHERE id=" + id + ";");
		if (data == null)// account doesnt exist
			return false;

		String validToken = data.get("token") + "";

		// return if the users token is equal to the valid token.
		return token.equalsIgnoreCase(validToken);
	}

	private String initTrip(HashMap<String, String> data) {
		// Generate a trip ID and add the trip to this user.
		// get the number of items in the database, and set this as the id.
		// IDs start at 0 and count up
		int tripID = (int) (long) getFirst("SELECT count(*) FROM trips;").get("count(*)");

		// contact database, and add a new trip.
		alterDataBase("INSERT INTO trips(id, creator) VALUES(" + tripID + ", \"" + data.get("id") + "\");");

		// TODO save trip to users data

		// Return trip ID to user
		return "{\"valid\":\"true\", \"trip id\":\"" + tripID + "\"}";
	}

	private String setTripDetails(HashMap<String, String> data) {
		// This method sets all the details for a trip. (not the files)
		String tripID = data.get("tripID");// gets the trip id

		// get the trip from the DB and check that the user created it.
		if (!(getFirst("SELECT creator FROM trips WHERE id=" + tripID + ";").get("creator") + "")
				.equals(data.get("id")))
			return "{\"valid\":\"false\", \"errMsg\":\"You did not create this trip.\"}";

		// check details are valid
		try {
			// This is the command executed on the DB
			String command = "";

			// Get trip location
			String loc = data.get("location");
			command += "location=\"" + loc + "\", ";

			// get trip address
			String add = data.get("address");
			command += "address=\"" + add + "\", ";

			// get trip date
			String dateStart = data.get("date");

			// Check its in the future
			String[] arr = dateStart.split("/");
			// Month is still set to "select"
			if (arr[1].equalsIgnoreCase("0"))
				return "{\"valid\":\"false\", \"errMsg\":\"Please select a month.\"}";

			// new date with args is depreciated, but its fine.
			@SuppressWarnings("deprecation")
			// The date that the trip will be
			// new Date(Year month day)
			Date tripDate = new Date(Integer.parseInt(arr[2]) - 1900, Integer.parseInt(arr[1]) - 1,
					Integer.parseInt(arr[0]));

			// check if the time now is greater than the time of the trip
			if (new Date().getTime() > tripDate.getTime()) {
				return "{\"valid\":\"false\", \"errMsg\":\"Date must be in the future.\"}";
			}

			// It was valid, so add it to the command
			command += "date_start=\"" + dateStart + "\", ";

			// get the time that the trip leaves
			String timeStart = data.get("leaving");
			// Check its numeric data
			int[] start;
			try {
				// checks that it is in a correct format
				start = isValidTime(timeStart, null);
			} catch (Exception e) {
				return "{\"valid\":\"false\", \"errMsg\":\"Please enter a valid time for the trip to start.\"}";
			}

			// valid so add it to the command
			command += "time_start=\"" + timeStart + "\", ";

			// get if its a residential trip, and convert it to a bool
			boolean residential = data.get("isResidential").equalsIgnoreCase("true");
			command += "is_residential=" + residential + ", ";

			// get when the trip ends
			String end = data.get("tripEnd");
			// Check its after start of trip, and is numeric
			try {
				// if its residential, then the end needs to be
				// a number of days, else the time needs to be in
				// the future.
				if (residential) {
					// checks the length is a number
					int e = Integer.parseInt(end);
					if (e <= 0)
						throw new Exception();
				} else {
					// check time is in a valid format and is after 'start'
					isValidTime(end, start);
				}
			} catch (Exception e) {
				return "{\"valid\":\"false\", \"errMsg\":\"Please enter a valid time for the trip to end.\"}";
			}
			// It was valid, so add it to the command.
			command += "end=\"" + end + "\", ";

			// get the purpose of the trip
			String purp = data.get("purpose");
			command += "purpose=\"" + purp + "\", ";

			// get the maximum number of pupils
			int max = Integer.parseInt(data.get("maxPupils"));
			// Check its between 80 and 1
			if (max > 80 || max <= 0)
				return "{\"valid\":\"false\", \"errMsg\":\"Invalid number of pupils.\"}";

			// valid, so add it to the command
			command += "max_students=" + max + ", ";

			// get the staff attending, and get rid of any extra new lines.
			String staff = data.get("staff").replace("\n\n\n", "\n");
			staff = staff.replace("\n\n", "\n").trim();

			// Check enough staff to cover students
			if (staff.equals(""))// no staff added
				return "{\"valid\":\"false\", \"errMsg\":\"Not enough staff.\"}";

			// if the number of staff (seperated by a new line) is not
			// greater than or equal to the number required for the
			// max students attending, return an error.
			if (!(staff.split("\n").length >= ((max - 1) / 20) + 1))
				return "{\"valid\":\"false\", \"errMsg\":\"Not enough staff.\"}";

			// TODO: add staff to command
			// command+="staff=[{\"staff\":\""+staff.replace("\n", ",")+"\"}],
			// ";
			command += "staff=null, ";

			// get mode of transport
			String trans = data.get("modeOfTransport");
			command += "transport=\"" + trans + "\", ";

			// get the total cost, and check its a number
			double cost = Double.parseDouble(data.get("totalCost"));
			// Check its greater than or equal to 0
			// (There could be a free trip, so allow 0
			if (cost < 0)
				return "{\"valid\":\"false\", \"errMsg\":\"Invalid total cost.\"}";
			// add it to command
			command += "cost=" + cost;

			// contact database, and add details to trip.
			alterDataBase("UPDATE trips SET " + command + " WHERE id=" + tripID + ";");
			// Return valid to user
			return "{\"valid\":\"true\"}";
		} catch (Exception e) {
			// An error has occoured, so details are invalid
			return "{\"valid\":\"false\", \"errMsg\":\"Please check your inputs.\"}";
		}

	}

	// checks that a time is valid
	private int[] isValidTime(String timeStart, int[] store) throws Exception {
		// splits it into hours and mins.
		String[] arr = timeStart.split(":");

		// check hour is in range
		int hour = Integer.parseInt(arr[0]);
		if (hour > 24 || hour < 0)
			throw new Exception();

		// check mins are in range
		int min = Integer.parseInt(arr[1]);
		if (min > 59 || min < 0)
			throw new Exception();

		// if we havent been passed a time,
		// then return the current one as an array
		if (store == null) {
			store = new int[2];
			store[0] = hour;
			store[1] = min;
		} else {
			// check if current time is after store

			if (hour < store[0]) {// if end is less than start, throw err
				throw new Exception();
			} else {
				if (hour == store[0] && min <= store[1]) {
					// if it ends the same hour, and the minute it ends is <= to
					// when it starts, throw err
					throw new Exception();
				}
			}
		}
		return store;
	}

	// Reads the entire contents of a file, used for the config
	public String readFile(File file) throws Exception {
		String ret = "";
		Scanner scan = new Scanner(file);
		while (scan.hasNextLine())
			ret += scan.nextLine();
		scan.close();
		return ret;
	}

	// Sends a query to the DB.
	// TODO: change username, password, port and IP to be in config.
	public List<HashMap<String, Object>> queryDataBase(String query) {
		try {
			// gets class to connect to DB
			Class.forName("com.mysql.cj.jdbc.Driver");

			// initiate a connection to the DB
			Connection connection = DriverManager.getConnection("jdbc:mysql://13.59.238.54:3306/trips", "test_user",
					"Password123_");

			// TODO Sanitize etc
			// Creates and executes a statement.
			Statement stmt = connection.createStatement();
			ResultSet resultSet = stmt.executeQuery(query);

			// get the number of columns returned
			ResultSetMetaData meta = resultSet.getMetaData();
			int columns = meta.getColumnCount();

			// create a list of rows returned.
			ArrayList<HashMap<String, Object>> list = new ArrayList<HashMap<String, Object>>();// TODO
																								// change

			// while there are more rows
			while (resultSet.next()) {
				// create a map for the row
				HashMap<String, Object> row = new HashMap<String, Object>(columns);
				// loop through every column in this row, and add it to the map
				for (int i = 1; i <= columns; ++i) {
					row.put(meta.getColumnName(i), resultSet.getObject(i));
				}
				// add the map to our list
				list.add(row);
			}
			// close result set and connection to prevent a leak.
			resultSet.close();
			connection.close();

			// return our list of rows
			return list;
		} catch (Exception e) {
			System.out.println(e);
		}
		return null;
	}

	// Allows us to alter values in the DB
	public boolean alterDataBase(String query) {
		try {
			// gets class to connect to DB
			Class.forName("com.mysql.cj.jdbc.Driver");

			// initiate a connection to the DB
			Connection connection = DriverManager.getConnection("jdbc:mysql://13.59.238.54:3306/trips", "test_user",
					"Password123_");

			// TODO Sanitize etc
			// Creates and executes a statement.
			Statement stmt = connection.createStatement();
			boolean b = stmt.execute(query);
			connection.close();

			// returns the boolean returned from executing the statement,
			// should it be needed anywhere.
			return b;
		} catch (Exception e) {
			System.out.println(query);
			System.out.println(e);
			return false;
		}
	}

	// Gets the first row returned from the database query
	public HashMap<String, Object> getFirst(String query) {
		// sends the query to the DB, then gets element 0 of it.
		return getData(queryDataBase(query), 0);
	}

	// Returns the database row with the index of id
	public static HashMap<String, Object> getData(List<HashMap<String, Object>> list, int id) {

		// if the list is null, or is empty, then return null
		if (list == null)
			return null;
		if (list.size() == 0)
			return null;

		// otherwise return the map at index 'id'
		return list.get(id);
	}

	// converts from a string to a JSON HashMap using GSON
	@SuppressWarnings("unchecked") // All data should be in a string format.
	public HashMap<String, String> stringToJSON(String text) {
		return new Gson().fromJson(text, HashMap.class);
	}
}
