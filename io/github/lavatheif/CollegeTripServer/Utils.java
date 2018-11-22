package io.github.lavatheif.CollegeTripServer;

import java.io.File;
import java.util.Date;
import java.util.HashMap;
import java.util.Scanner;

import com.google.gson.Gson;

public class Utils {
	public static final int PORT = 25000;
	
	/**
	 * A function that takes the message from the client as an input,
	 * and returns a string to respond with.
	 * 
	 * @param message
	 * @return response
	 */
	public String evaluate(String message){
		HashMap<String, String> data = stringToJSON(message);
		String response;
		String request = data.get("request").toLowerCase();
		switch(request){
		case("new trip")://Starts making a new trip
			response = initTrip(data);
			break;
		case("set data")://Sets the variables the user enters
			response = setTripDetails(data);
			break;
		default:
			response = "{//TODO err msg}"+request;
			break;
		}
		return response;
	}
	
	private boolean validateUser(String token){
		//TODO: check if any users have this token.  If not, its invalid.
		//Tokens are valid for 30 mins, and are reset on login
		return true;
	}
	
	private String initTrip(HashMap<String, String> data) {
		if(!validateUser(data.get("token")))
			return "{\"valid\":\"false\", \"errMsg\":\"Invalid token.  Please log in.\"}";
		//Check email provided is valid
		String email = data.get("email");
		
		if(!email.contains("@woking.ac.uk")){
			return "{\"valid\":\"false\", \"errMsg\":\"Please use a valid woking college E-Mail.\"}";
		}
		//Email valid
		
		String tripID = "1234";
		//TODO: contact database, and add a new trip.
		//Generate a trip ID and add the trip to this user.
		//Return trip ID to user
		return "{\"valid\":\"true\", \"trip id\":\""+tripID+"\"}";
	}
	private String setTripDetails(HashMap<String, String> data) {
		if(!validateUser(data.get("token")))
			return "{\"valid\":\"false\", \"errMsg\":\"Invalid token.  Please log in.\"}";
		
		//check details are valid
		try{
	        String loc = data.get("location");
	        String add = data.get("address");
	        
	        String dateStart = data.get("date");//Check its in the future
	        String[] arr = dateStart.split("/");
	        
	        if(arr[1].equalsIgnoreCase("0"))
				return "{\"valid\":\"false\", \"errMsg\":\"Please select a month.\"}";
	        
	        //new Date(Year month day)
	        @SuppressWarnings("deprecation")
			Date tripDate = new Date(Integer.parseInt(arr[2])-1900,
	        		Integer.parseInt(arr[1])-1, Integer.parseInt(arr[0]));
	        if(new Date().getTime() > tripDate.getTime()){
				return "{\"valid\":\"false\", \"errMsg\":\"Date must be in the future.\"}";
	        }
	        
	        String timeStart = data.get("leaving");//Check its numeric data
	        arr = timeStart.split(":");
	        try{
	        	int hour = Integer.parseInt(arr[0]);
	        	if(hour > 24 || hour < 0)
	        		throw new Exception();

	        	int min = Integer.parseInt(arr[1]);
	        	if(min > 59 || min < 0)
	        		throw new Exception();
	        
	        }catch(Exception e){
				return "{\"valid\":\"false\", \"errMsg\":\"Please enter a valid time for the trip to start.\"}";
	        }
	        
	        boolean residential = data.get("isResidential").equalsIgnoreCase("true");//convert to bool
	        
	        String end = data.get("tripEnd");//Check its after start of trip, and is numeric
	        
	        String purp = data.get("purpose");
	        
	        int max = Integer.parseInt(data.get("maxPupils"));//Check max 80, and is greater than 0
	        if(max > 80 || max <= 0)
				return "{\"valid\":\"false\", \"errMsg\":\"Invalid number of pupils.\"}";

	        String staff = data.get("staff").replace("\n\n\n", "\n");//Check enough staff to cover students
	        staff = staff.replace("\n\n", "\n").trim();
	        System.out.println(((max-1)/20)+1);
	        System.out.println(staff.split("\n").length);
	        
	        if(!(staff.split("\n").length >= ((max-1)/20)+1))
				return "{\"valid\":\"false\", \"errMsg\":\"Not enough staff.\"}";
	        
	        
	        String trans = data.get("modeOfTransport");
	        
	        int cost = Integer.parseInt(data.get("totalCost"));//Check its a number
	        
	        //TODO Save all data to a database
		}catch(Exception e){
			//An error has occoured, so details are invalid
			return "{\"valid\":\"false\", \"errMsg\":\"Please check your inputs\"}";
		}

		//TODO: contact database, and add details to trip, if not exists.
		//If they do then throw an error.
		//Return valid to user
		return "{\"valid\":\"true\"}";
	}

	//Adds a message to the log.
	public void log(Object o){
		String msg = o.toString();
		//TODO: Save to log file.
		System.out.println(msg);
	}
	
	//Reads the entire contents of a file
	public String readFile(File file) throws Exception{
		String ret = "";
		Scanner scan = new Scanner(file);
		while(scan.hasNextLine())
			ret += scan.nextLine();
		
		scan.close();
		return ret;
	}
	
	@SuppressWarnings("unchecked")//All data should be in a string format.
	public HashMap<String, String> stringToJSON(String text){
		return new Gson().fromJson(text, HashMap.class);
	}
}
