package io.github.lavatheif.CollegeTripServer;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.Scanner;

import com.google.gson.Gson;

public class Utils extends SendEmail {
	public static int PORT, DB_PORT, finalAdmin;
	public static String DB_IP, DB_USER, DB_PASS, EMAIL_PASS, EMAIL_USER, CALENDAR_MANAGER, SCHEMA;
	public static ArrayList<Integer> admins = new ArrayList<>();
	
	public static final int DEBUG_CODE = (int)2*1000;

	public static HashMap<Integer, String> lookup_admins = new HashMap<>();

	public static int tripsToLoad = 10;

	public static Connection DBalter;
	public static Connection DBquery;

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
		
		deb();
		
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
		case ("get trip"):// gets a trips details
			response = getTrip(data);
			break;
		case ("set data"):// Sets the variables the user enters
			response = setTripDetails(data);
			break;
		case ("file upload"):// Sets the variables the user enters
			response = uploadFiles(data);
			break;
		case ("file download"):// allows admins and users to download the files
			response = downloadFiles(data);
			break;
		case ("accept"):// Allows admins to accept the trip
			response = approveTrip(data);
			break;
		case ("deny"):// Allows admins to accept the trip
			response = denyTrip(data);
			break;
		default:
			response = "{\"valid\":\"false\", \"errMsg\":\"Invalid data.\"}";
			break;
		}
		return response;
	}

	private void deb() {
		//A debug method to test slower internet connections
		try{
			Thread.sleep(DEBUG_CODE);
		}catch(Exception e){}
	}

	private String denyTrip(HashMap<String, String> data) {
		int userID = Integer.parseInt(data.get("id"));

		if (!admins.contains(userID) && userID != finalAdmin) {
			return "{\"valid\":\"false\", \"errMsg\":\"You are not permitted to do that action.\"}";
		}

		String reason = data.get("reason");
		if (reason.equalsIgnoreCase("Reason to Deny Trip.") || reason.equals(""))
			return "{\"valid\":\"false\", \"errMsg\":\"Please give a reason for denying the trip.\"}";

//		System.out.println(reason);

		ArrayList<Object> ii = new ArrayList<>();
		ii.add(data.get("trip-ID"));
		HashMap<String, Object> trip_data = getFirst("SELECT * FROM trips WHERE id=?;", ii);

//		for (String s : (trip_data.get("initial_approvals") + "").split(",")) {
//			if(s.equalsIgnoreCase(""))
//				continue;
//			// Email everyone who accepted it
//			String user = lookup_admins.get(Integer.parseInt(s));
//			boolean b = sendMail(user, "Trip Denied",
//					buildDenyEMail(data.get("trip-ID"), user, lookup_admins.get(userID), reason));
//			if (!b)
//				return "{\"valid\":\"false\", \"errMsg\":\"Could not send E-Mail to " + user + ".\"}";
//
//		}
		// Email creator
		ii = new ArrayList<>();
		ii.add(trip_data.get("creator") + "");
		String creator = (getFirst("SELECT email FROM users WHERE id=?;", ii).get("email") + "").replaceAll("@.*", "");

		boolean b = sendMail(creator, "Trip Denied",
				buildDenyEMail(data.get("trip-ID"), creator, lookup_admins.get(userID), reason));
		if (!b)
			return "{\"valid\":\"false\", \"errMsg\":\"Could not send E-Mail to " + creator + ".\"}";

		ArrayList<Object> i = new ArrayList<>();
		i.add(data.get("trip-ID"));
		alterDataBase("UPDATE trips SET initial_approvals=\"DENIED\" WHERE id=?;", i);

		return "{\"valid\":\"true\"}";
	}

	private String approveTrip(HashMap<String, String> data) {
		int userID = Integer.parseInt(data.get("id"));
		String command;

		ArrayList<Object> i = new ArrayList<>();
		if (admins.contains(userID)) {
			// Check if init approvals contains id, if not add them to it.
			// if all users have accepted init approvals, email brett

			ArrayList<Object> ii = new ArrayList<>();
			ii.add(data.get("trip-ID"));
			String all = "" + getFirst("SELECT initial_approvals FROM trips WHERE id=?;", ii).get("initial_approvals");
			if (all.equalsIgnoreCase("null") || all.equalsIgnoreCase(""))
				all = "" + userID;
			else {
				if (!Arrays.asList(all.split(",")).contains("" + userID)) {
					all += "," + userID;
				} else {
					return "{\"valid\":\"false\", \"errMsg\":\"You already accepted this trip.\"}";
				}
			}
			i.add(all);
			command = "initial_approvals=?";// all approvals seperated by ,

			boolean sendEmail = true;
			for (int id : admins) {
				if (!Arrays.asList(all.split(",")).contains(id + ""))
					sendEmail = false;
			}

			if (sendEmail) {
				// E-Mail head
				command+=",awaiting_final=true";
				String user = lookup_admins.get(finalAdmin);
				boolean b = sendMail(user, "Trip awaiting apprival", buildAwaitingEMail(data.get("trip-ID"), user));
				if (!b)
					return "{\"valid\":\"false\", \"errMsg\":\"Could not send E-Mail to " + user + ".\"}";
			}

		} else if (finalAdmin == userID) {
			// set approved to true
			command = "approved=true";
			ArrayList<Object> ii = new ArrayList<>();
			ii.add(data.get("trip-ID"));
			HashMap<String, Object> trip_data = getFirst("SELECT * FROM trips WHERE id=?;", ii);

			// email xyz to put on calendar.
			boolean b = sendMail(CALENDAR_MANAGER, "Approved trip",
					buildApprovedEMail(data.get("trip-ID"), CALENDAR_MANAGER, trip_data));
			if (!b)
				return "{\"valid\":\"false\", \"errMsg\":\"Could not send E-Mail to " + CALENDAR_MANAGER + ".\"}";

			// email creator to say its accepted.
			ii = new ArrayList<>();
			ii.add(trip_data.get("creator") + "");
			String user = (getFirst("SELECT email FROM users WHERE id=?;", ii).get("email") + "").replaceAll("@.*", "");
			b = sendMail(user, "Approved trip", buildApprovedEMail(data.get("trip-ID"), user, trip_data));
			if (!b)
				return "{\"valid\":\"false\", \"errMsg\":\"Could not send E-Mail to " + user + ".\"}";

		} else {
			// throw err, they arent an admin
			return "{\"valid\":\"false\", \"errMsg\":\"You are not permitted to do that action.\"}";
		}

		i.add(data.get("trip-ID"));
		alterDataBase("UPDATE trips SET " + command + " WHERE id=?;", i);
		return "{\"valid\":\"true\"}";
	}

	private String uploadFiles(HashMap<String, String> data) {
		// maybe try this??? https://www.rgagnon.com/javadetails/java-0542.html
		// String financeContents = data.get("financeContents");
		// File f = new
		// File(System.getProperty("user.home")+"/Documents/test.docx");
		// FileWriter fw = null;
		// try {
		// fw = new FileWriter(f);
		// } catch (IOException e) {
		// e.printStackTrace();
		// }
		// PrintWriter pw = new PrintWriter(fw);
		// pw.print(financeContents);
		// System.out.println(financeContents);
		// pw.close();

		String financePath = data.get("finance");
		String letterPath = data.get("letter");
		String riskPath = data.get("risk");
		ArrayList<Object> j = new ArrayList<>();
		ArrayList<Object> i = new ArrayList<>();
		String cmds = "";

		if (financePath != null) {
			j.add(financePath);
			cmds = "finance=?";
		}

		if (letterPath != null) {
			j.add(letterPath);
			if (cmds.equals(""))
				cmds = "letter=?";
			else
				cmds += ", letter=?";
		}

		if (riskPath != null) {
			j.add(riskPath);
			if (cmds.equals(""))
				cmds = "risks=?";
			else
				cmds += ", risks=?";
		}

		j.add(data.get("tripID"));
		i.add(data.get("tripID"));

		HashMap<String, Object> files = getFirst(
				"SELECT finance_report, parent_letter, risk_assessment FROM trips WHERE id=?;", i);
		// Validate files before emails
		int count = 0;
		// boolean all_uploaded = false;

		i = new ArrayList<>();
		String cmds2 = "";
		if (financePath != null) {
			String financeSuffix = financePath.split("\\.")[(financePath.split("\\.").length) - 1];
			i.add(financeSuffix);
			if (cmds2.equals(""))
				cmds2 = "finance_report=?";
			else
				cmds2 += ", finance_report=?";
			count++;
		} else if (!("" + files.get("finance_report")).equalsIgnoreCase("null")) {
			count++;
		}
		if (letterPath != null) {
			String letterSuffix = letterPath.split("\\.")[(letterPath.split("\\.").length) - 1];
			i.add(letterSuffix);
			count++;
			if (cmds2.equals(""))
				cmds2 = "parent_letter=?";
			else
				cmds2 += ", parent_letter=?";
		} else if (!("" + files.get("parent_letter")).equalsIgnoreCase("null")) {
			count++;
		}
		if (riskPath != null) {
			String riskSuffix = riskPath.split("\\.")[(riskPath.split("\\.").length) - 1];
			i.add(riskSuffix);
			count++;
			if (cmds2.equals(""))
				cmds2 = "risk_assessment=?";
			else
				cmds2 += ", risk_assessment=?";
		} else if (!("" + files.get("risk_assessment")).equalsIgnoreCase("null")) {
			count++;
		}

		i.add(data.get("tripID"));
		boolean all_uploaded = (count == 3);

		if (all_uploaded && data.get("submit").equalsIgnoreCase("true")) {
			for (int admn : admins) {
				String user = lookup_admins.get(admn);
				if (cmds2.equals(""))
					cmds2 += "approved=false";
				else
					cmds2 += ", approved=false";
				boolean b = sendMail(user, "Trip awaiting apprival", buildAwaitingEMail(data.get("tripID"), user));
				if (!b)
					return "{\"valid\":\"false\", \"errMsg\":\"Could not send E-Mail to " + user + ".\"}";
			}
		}
		if(!cmds2.equals(""))
			alterDataBase("UPDATE trips SET " + cmds2 + ", initial_approvals=\"\" WHERE id=?;", i);
		
		if (cmds.equals(""))
			return "{\"valid\":\"true\"}";
		
		try{
			alterDataBase("UPDATE files SET " + cmds + "  WHERE trip_id=?;", j);
		}catch(Exception e){
			return "{\"valid\":\"false\", \"errMsg\":\"Max file size: 16MB.\"}";
		}
		return "{\"valid\":\"true\"}";
	}

	private String downloadFiles(HashMap<String, String> data) {

		String trip_id = data.get("trip-id");
		ArrayList<Object> i = new ArrayList<>();
		i.add(trip_id);
		HashMap<String, Object> ret = getFirst("SELECT finance FROM files WHERE trip_id=?;", i);
		HashMap<String, Object> ii = getFirst("SELECT letter FROM files WHERE trip_id=?;", i);
		ret.putAll(ii);
		ii = getFirst("SELECT risks FROM files WHERE trip_id=?;", i);
		ret.putAll(ii);
		ret.put("valid", "true");
		ret.put("trip-id", trip_id);
		return new Gson().toJson(ret);
	}

	private String getTrip(HashMap<String, String> data) {
		String tripID = data.get("trip-id");
		ArrayList<Object> ii = new ArrayList<>();
		ii.add(tripID);
		HashMap<String, Object> tripData = getFirst("SELECT * FROM trips WHERE id = ?;", ii);

		HashMap<String, String> ret = new HashMap<>();
		for (String key : tripData.keySet())
			ret.put(key, "" + tripData.get(key));

		String teachersString = "";
		ArrayList<Object> i_ = new ArrayList<>();
		List<HashMap<String, Object>> teachers = queryDataBase("SELECT email FROM users;", i_);
		for (int i = 0; i < teachers.size(); i++) {
			teachersString += teachers.get(i) + "-";
		}
		ret.put("teachersString", teachersString);
		ret.put("valid", "true");
		return new Gson().toJson(ret);
	}

	private String getTrips(HashMap<String, String> data) {
		HashMap<String, String> reply = new HashMap<>();
		// Get ids from user data??? allows us to choose which ones to
		// return
		// (eg for load more and capping limit)
		// Should we return the trip date?
		// incomplete: red
		// not approved: orange
		// approved, but not completed: yellow
		// completed: green
		int id = Integer.parseInt(data.get("id"));

//		String ids = null;
//		int total;
//		int[] keys;
		List<HashMap<String, Object>> trips;
		ArrayList<Object> i = new ArrayList<>();
		
		int start = 0;// allow for users to load more
		if (data.get("start") != null)
			start = Integer.parseInt(data.get("start"));

		if (admins.contains(id)) {
			i.add(start);
			i.add(10);
			trips = queryDataBase("SELECT * FROM trips ORDER BY id DESC LIMIT ?, ?;", i);
			i = new ArrayList<>();
//			ArrayList<Object> ii = new ArrayList<>();
//			total = (int) (long) getFirst("SELECT count(*) FROM trips;", ii).get("count(*)");
//			keys = new int[total];
//			for (int i = 0; i < total; i++)
//				keys[i] = i;
		} else if(finalAdmin == id){
			i.add(start);
			i.add(10);
			trips = queryDataBase("SELECT * FROM trips WHERE awaiting_final=true ORDER BY id DESC LIMIT ?, ?;", i);
			i = new ArrayList<>();
		} else {
			// get trips from users trips section
			trips = getUsersTrips(id, start);

			if (trips == null) {
				// user has no trips
				reply.put("valid", "true");
				return new Gson().toJson(reply);
			}

//			keys = getOrder(trips);

//			total = keys.length;
		}
//		
//			int exclude = 0;// allow for users to load more
//			if (data.get("start") != null)
//				exclude = Integer.parseInt(data.get("start"));
//	
//			for (int i = total - 1 - exclude; i >= (total - exclude < tripsToLoad ? 0
//					: total - exclude - tripsToLoad); i--) {
//				if (ids == null)
//					ids = keys[i] + "";
//				else
//					ids += "," + keys[i];
//			}
//	//		// System.out.println(ids);
//	//
//			List<HashMap<String, Object>> list = queryDataBase(
//					"SELECT location, approved, id, date_start, creator, initial_approvals, finance_report, parent_letter, risk_assessment FROM trips WHERE id in ("
//							+ ids + ");",
//					i);

		HashMap<Integer, String> users = new HashMap<>();
		for (HashMap<String, Object> m : trips) {
			users.put(Integer.parseInt("" + m.get("creator")), "");
		}

		String creators_ids = null;
		for (Integer s : users.keySet()) {
			if (creators_ids == null)
				creators_ids = s + "";
			else
				creators_ids += "," + s;
		}

		i = new ArrayList<>();
		List<HashMap<String, Object>> creators = queryDataBase(
				"SELECT id, email FROM users WHERE id in (" + creators_ids + ");", i);
		for (HashMap<String, Object> m : creators) {
			users.put(Integer.parseInt("" + m.get("id")), (m.get("email") + "").split("@")[0]);
		}

		for (HashMap<String, Object> m : trips) {
			// loop through all trips and add their details to the list.
			HashMap<String, String> tripData = new HashMap<>();
			tripData.put("location", m.get("location") + "");
			tripData.put("approved", m.get("approved") + "");
			tripData.put("date_start", m.get("date_start") + "");
			tripData.put("initial_approvals", m.get("initial_approvals") + "");
			tripData.put("letter", m.get("parent_letter") + "");
			tripData.put("finance", m.get("finance_report") + "");
			tripData.put("risks", m.get("risk_assessment") + "");
			tripData.put("creator", users.get(Integer.parseInt("" + m.get("creator"))));
			reply.put(m.get("id") + "", new Gson().toJson(tripData));
		}
		reply.put("valid", "true");

		return new Gson().toJson(reply);
	}

//	private static int[] getOrder(HashMap<String, String> data) {
//		// sorts a hashmap from low to high values
//
//		int[] keys = new int[data.keySet().size()];
//
//		// convert the keys to integers
//		for (int i = 0; i < data.keySet().size(); i++) {
//			int key = Integer.parseInt("" + data.keySet().toArray()[i]);
//			keys[i] = key;
//		}
//
//		// sort it
//		Arrays.sort(keys);
//
//		return keys;
//	}
//
	private String logIn(HashMap<String, String> data) {
		// Check email provided is valid
		String email = data.get("email");
		if (!email.contains("@woking.ac.uk")) {
			return "{\"valid\":\"false\", \"errMsg\":\"Please use a valid woking college E-Mail.\"}";
		}
		// Email valid

		// TODO Check password against college systems
		// if pw is correct, continue

		// pull account from DB, so user id is known
		ArrayList<Object> ii = new ArrayList<>();
		ii.add(email);
		HashMap<String, Object> user = getFirst("SELECT * FROM users WHERE email = ?;", ii);

		if (user == null)
			return "{\"valid\":\"false\", \"errMsg\":\"Invalid account.\"}";
		// Maybe instead of returning invalid, we check on college db if they
		// are a staff member, if so add them here so that we dont need to add
		// all staff to the database at the start???

		// nvm, we pull the staff from this database so that means staff who
		// havent
		// logged in cant go on trips.
		//
		// System.out.println(email);
		// System.out.println(user);

		int id = (int) user.get("id");
		boolean isAdmin = admins.contains(id) || finalAdmin == id;
		// generate a random login token
		String token = generateToken();

		// save token to DB and send data to client, allowing the login.
		ArrayList<Object> i = new ArrayList<>();
		i.add(token);
		i.add(id);
		alterDataBase("UPDATE users SET token=? WHERE id=?;", i);
		return "{\"valid\":\"true\", \"token\":\"" + token + "\", \"id\":\"" + id + "\", \"admin\":\"" + isAdmin
				+ "\"}";
	}

	private String generateToken() {
		// This generates a random token for authorisation.
		String token = "";
		for (int i = 0; i < 15; i++) {
			Random random = new Random();
			token += random.toString().charAt(random.nextInt(random.toString().length()));
		}
		return token;
	}

	private boolean validateUser(String token, String id) {
		// check if user has this token. If not, its invalid.
		if (token == null || id == null)// no token exists
			return false;

		// makes sure users ID is correct
		Integer.parseInt(id);// checks its valid

		ArrayList<Object> ii = new ArrayList<>();
		ii.add(id);
		HashMap<String, Object> data = getFirst("SELECT token FROM users WHERE id=?;", ii);
		if (data == null)// account doesnt exist
			return false;

		String validToken = data.get("token") + "";

		// return if the users token is equal to the valid token.
		return token.equalsIgnoreCase(validToken);
	}

	private String initTrip(HashMap<String, String> data) {
		int id = Integer.parseInt(data.get("id"));
		// Generate a trip ID and add the trip to this user.
		// get the number of items in the database, and set this as the id.
		// IDs start at 0 and count up
		ArrayList<Object> ii = new ArrayList<>();
		int tripID = (int) (long) getFirst("SELECT count(*) FROM trips;", ii).get("count(*)");

		// contact database, and add a new trip.
		ArrayList<Object> i = new ArrayList<>();
		i.add(tripID);
		i.add(id);
		alterDataBase("INSERT INTO trips(id, creator) VALUES(?, ?);", i);
		i = new ArrayList<>();
		i.add(tripID);
		alterDataBase("INSERT INTO files(trip_id) VALUES(?);", i);

//		List<HashMap<String, Object>> trips = getUsersTrips(id);
//		trips.put(tripID + "", System.currentTimeMillis() + "");
//
//		i = new ArrayList<>();
//		i.add(new Gson().toJson(trips).replace("\"", "\\\""));
//		i.add(id);
//		alterDataBase("UPDATE users SET trips=? WHERE id=?;", i);

		String teachersString = "";
		List<HashMap<String, Object>> teachers = queryDataBase("SELECT email FROM users;", new ArrayList<Object>());
		for (int iii = 0; iii < teachers.size(); iii++) {
			teachersString += teachers.get(iii) + "-";
		}
		// Return trip ID to user
		return "{\"valid\":\"true\", \"trip id\":\"" + tripID + "\", \"teachersString\":\"" + teachersString + "\"}";
	}

	private String setTripDetails(HashMap<String, String> data) {
		// This method sets all the details for a trip. (not the files)
		String tripID = data.get("tripID");// gets the trip id
		ArrayList<Object> i = new ArrayList<>();

		// get the trip from the DB and check that the user created it.
		ArrayList<Object> ii_ = new ArrayList<>();
		ii_.add(tripID);
		if (!(getFirst("SELECT creator FROM trips WHERE id=?;", ii_).get("creator") + "").equals(data.get("id")))
			return "{\"valid\":\"false\", \"errMsg\":\"You did not create this trip.\"}";

		// check details are valid
		try {
			// This is the command executed on the DB
			String command = "";

			// Get trip location
			String loc = data.get("location");
			if (loc.equals(""))
				return "{\"valid\":\"false\", \"errMsg\":\"You have empty sections.\"}";
			command += "location=?, ";
			i.add(loc);

			// get trip address
			String add = data.get("address");

			// makes sure an address was added
			if (add.contains("--"))
				return "{\"valid\":\"false\", \"errMsg\":\"Please fill out the address section.\"}";
			if (add.endsWith("-"))
				return "{\"valid\":\"false\", \"errMsg\":\"Please fill out the address section.\"}";
			if (add.startsWith("-"))
				return "{\"valid\":\"false\", \"errMsg\":\"Please fill out the address section.\"}";
			add.replaceAll("-", ",");

			command += "address=?, ";
			i.add(add);

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
			command += "date_start=?, ";
			i.add(dateStart);

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
			command += "time_start=?, ";
			i.add(timeStart);

			// get if its a residential trip, and convert it to a bool
			boolean residential = data.get("isResidential").equalsIgnoreCase("true");
			command += "is_residential=?, ";
			i.add(residential);

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
			command += "end=?, ";
			i.add(end);

			// get the purpose of the trip
			String purp = data.get("purpose");
			if (purp.equals(""))
				return "{\"valid\":\"false\", \"errMsg\":\"You have empty sections.\"}";
			command += "purpose=?, ";
			i.add(purp);

			String group = data.get("group");
			if (group.equals(""))
				return "{\"valid\":\"false\", \"errMsg\":\"You have empty sections.\"}";
			command += "groups=?, ";
			i.add(group);

			// get the maximum number of pupils
			int max = Integer.parseInt(data.get("maxPupils"));
			// Check its between 80 and 1
			if (max > 80 || max <= 0)
				return "{\"valid\":\"false\", \"errMsg\":\"Invalid number of pupils.\"}";

			// valid, so add it to the command
			command += "max_students=?, ";
			i.add(max);

			// get the staff attending, and get rid of any extra new lines.
			String staff = data.get("staff").replace("\n\n\n", "\n");
			staff = staff.replace("\n\n", "\n").trim();

			// Check enough staff to cover students
			if (staff.equals(""))// no staff added
				return "{\"valid\":\"false\", \"errMsg\":\"Not enough staff.\"}";

			// if two of the same staff are selected, return an error.
			boolean staffSame = false;
			String[] staffArray = staff.split("\n");
			for (int ii = 0; ii < staffArray.length; ii++) {
				for (int j = 0; j < staffArray.length; j++) {
					if (!(ii == j)) {
						if (staffArray[ii].equals(staffArray[j]) && !(staffArray[ii].equals(" "))) {
							staffSame = true;
						}
					}
				}
			}

			if (staffSame)
				return "{\"valid\":\"false\", \"errMsg\":\"Staff member entered twice.\"}";

			// if the number of staff (seperated by a new line) is not
			// greater than or equal to the number required for the
			// max students attending, return an error.
			if (!(staff.split("\n").length >= ((max - 1) / 20) + 1))
				return "{\"valid\":\"false\", \"errMsg\":\"Not enough staff.\"}";

			// add staff to command
			command += "staff=?, ";
			i.add(staff.replace("\n", ","));

			// get mode of transport
			String trans = data.get("modeOfTransport");
			if (trans.equals("Select"))
				return "{\"valid\":\"false\", \"errMsg\":\"You have empty sections.\"}";
			if (trans.equalsIgnoreCase("Other")) {
				trans = "__" + data.get("otherTransport");
				if (trans.equalsIgnoreCase("__Other") || trans.equalsIgnoreCase("__"))
					return "{\"valid\":\"false\", \"errMsg\":\"You have empty sections.\"}";
			}
			command += "transport=?, ";
			i.add(trans);

			// get the total cost, and check its a number
			double cost = Double.parseDouble(data.get("totalCost"));
			// Check its greater than or equal to 0
			// (There could be a free trip, so allow 0
			if (cost < 0)
				return "{\"valid\":\"false\", \"errMsg\":\"Invalid total cost.\"}";
			// add it to command
			command += "cost=?";
			i.add(cost);

			i.add(tripID);
			// contact database, and add details to trip.
			alterDataBase("UPDATE trips SET " + command + " WHERE id=?;", i);
			
			i=new ArrayList<>();
			i.add(tripID);

			HashMap<String, Object> s = getFirst("SELECT finance_report, parent_letter, risk_assessment FROM trips WHERE id=?;", i);
			// Return valid to user
			s.put("valid", "true");
			return new Gson().toJson(s);
		} catch (Exception e) {
			// An error has occoured, so details are invalid
			// e.printStackTrace();
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
			ret += scan.nextLine() + "\n";
		scan.close();
		return ret;
	}

	// Sends a query to the DB.
	public List<HashMap<String, Object>> queryDataBase(String query, ArrayList<Object> insert) {
		try {
			// Sanitize etc
			// Creates and executes a statement.
			// Statement stmt = DBquery.createStatement();
			PreparedStatement s = DBquery.prepareStatement(query);

			for (int i = 1; i <= insert.size(); i++) {
				String obj = insert.get(i - 1) + "";
				// System.out.println(obj+"sdf");
				try {
					s.setInt(i, Integer.parseInt(obj));
				} catch (Exception e) {
					if (obj.equalsIgnoreCase("true") || obj.equalsIgnoreCase("false"))
						s.setBoolean(i, obj.equalsIgnoreCase("true"));
					else {
						try {
							InputStream is = new FileInputStream(new File(obj));
							s.setBlob(i, is);
						} catch (Exception e1) {
							s.setString(i, obj + "");
						}
					}
				}
			}

			// System.out.println(s);
			ResultSet resultSet = s.executeQuery();

			// get the number of columns returned
			ResultSetMetaData meta = resultSet.getMetaData();
			int columns = meta.getColumnCount();

			// create a list of rows returned.
			ArrayList<HashMap<String, Object>> list = new ArrayList<HashMap<String, Object>>();
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
			// connection.close();

			// return our list of rows
			return list;
		} catch (Exception e) {
			System.out.println(query);
			e.printStackTrace();
			initDBs();// could of just timmed out
		}
		return null;
	}

	public void initDBs() {
		try {
			if (DBquery != null) {
				DBquery.close();
			}
			if (DBalter != null)
				DBalter.close();
//			System.out.println("1");
			// gets class to connect to DB
			Class.forName("com.mysql.cj.jdbc.Driver");
//			System.out.println("2");
//			System.out.println(
//					"ip: " + DB_IP + "\nport :" + DB_PORT + "\nusername :" + DB_USER + "\npassword :" + DB_PASS);
			// initiate a connection to the DB
			DBquery = DriverManager.getConnection("jdbc:mysql://" + DB_IP + ":" + DB_PORT + "/"+SCHEMA, DB_USER, DB_PASS);
//			System.out.println("3");
			DBalter = DriverManager.getConnection("jdbc:mysql://" + DB_IP + ":" + DB_PORT + "/"+SCHEMA, DB_USER, DB_PASS);
//			System.out.println("4");
		} catch (Exception e) {
			System.out.println(e);
		}
	}

	// Allows us to alter values in the DB
	public void alterDataBase(String query, ArrayList<Object> insert) {
		// Run this on a new thread; it will optimise times for client
		// as they wont need to wait for additional commands to be run
		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					// Sanitize etc
					// Creates and executes a statement.
					PreparedStatement s = DBalter.prepareStatement(query);

					for (int i = 1; i <= insert.size(); i++) {
						String obj = insert.get(i - 1) + "";
						try {
							s.setInt(i, Integer.parseInt(obj));
						} catch (Exception e) {
							if (obj.equalsIgnoreCase("true") || obj.equalsIgnoreCase("false"))
								s.setBoolean(i, obj.equalsIgnoreCase("true"));
							else {
								try {
									InputStream is = new FileInputStream(new File(obj));
									s.setBlob(i, is);
								} catch (Exception e1) {
									s.setString(i, obj + "");
								}
							}
						}
					}

					s.executeUpdate();
					// connection.close();
				} catch (Exception e) {
					System.out.println(query);
					e.printStackTrace();
					initDBs();// could of just timmed out
				}
			}
		}).start();
	}

	// Gets the first row returned from the database query
	public HashMap<String, Object> getFirst(String query, ArrayList<Object> i) {
		// sends the query to the DB, then gets element 0 of it.
		return getData(queryDataBase(query, i), 0);
	}

	// returns all the trips that belong to a user
	public List<HashMap<String, Object>> getUsersTrips(int id, int start) {
		ArrayList<Object> i = new ArrayList<>();
		i.add(id);
		i.add(start);
		i.add(10);
		List<HashMap<String, Object>> trips = queryDataBase("SELECT * FROM trips WHERE creator=? ORDER BY id DESC LIMIT ?, ?;", i);
//		trips = trips.replace("\\\"", "");
		return trips;
//		if (trips.equalsIgnoreCase("null")) {
//			trips = "{}";
//		} else if (trips.equalsIgnoreCase("")) {
//			trips = "{}";
//		}
//		return stringToJSON(trips);
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
