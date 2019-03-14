package io.github.lavatheif.CollegeTripServer;

import java.util.HashMap;
import java.util.Properties;

import javax.mail.Message;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

public class SendEmail {
	
	public static boolean sendMail(String to, String subj_text, String msg_text){
		final String username = Utils.EMAIL_USER + "@woking.ac.uk";
		final String password = Utils.EMAIL_PASS;
	
		Properties props = new Properties();
		props.put("mail.smtp.auth", "true");
		props.put("mail.smtp.host", "mail.woking.ac.uk");
		props.put("mail.smtp.port", "587");
		props.put("mail.smtp.ssl.trust", "mail.woking.ac.uk");
//		props.put("mail.debug", "true");

		Session session = Session.getInstance(props, new javax.mail.Authenticator() {
			protected PasswordAuthentication getPasswordAuthentication() {
				return new PasswordAuthentication(username, password);
			}
		});

		try {
			Message message = new MimeMessage(session);
			message.setFrom(new InternetAddress(username, "Trip Organiser"));
			message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to+"@woking.ac.uk"));
			message.setSubject(subj_text);
			message.setText(msg_text);

			Transport.send(message);
//			System.out.println("Sent mail");
			return true;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}
	
	public static String buildAwaitingEMail(String id, String username){
		String content = "Hello "+username+", \n\nA trip is currently awaiting your approval!\n"
				+ "Log into the trip planner to view its details.\n\n\n"
				+ "Trip ID: "+id+"\n";
//		System.out.println(content);
		return content;
	}
	
	public static String buildApprovedEMail(String id, String to, HashMap<String, Object> trip_data) {
		boolean res = (""+trip_data.get("is_residential")).equalsIgnoreCase("true");
		String content = "Hello "+to+", \n\nA trip to "+trip_data.get("location")+" was just approved!\n"
				+ "This trip will be taking place on "+trip_data.get("date_start")+" at "+trip_data.get("time_start")+"\n"
				+ "it is"+(res?"":"n't")+" a residential trip, ending "+(res?"after "+trip_data.get("end")+" days":"at "+trip_data.get("end"))+"\n"
				+ "Trip ID: "+id+"\n";
//		System.out.println(content);
		return content;
	}

	public static String buildDenyEMail(String id, String user, String by, String reason) {
		String content = "Hello "+user+", \n\nA trip was just denied by "+by+"\n\n"
				+ "Reason: "+reason
				+ "\n\nIf you are the owner of this trip, please edit the details.  If not, we are just letting you know that you may need to accept it again soon.\n"
				+ "Trip ID: "+id+"\n";
//		System.out.println(content);
		return content;
	}

}
