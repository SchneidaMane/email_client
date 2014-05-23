package assn5;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Client {

	 public static void main(String argv[]) throws Exception
	 {
		 boolean CloseConnection = false;
		 while (CloseConnection == false){
			 String serverHostname;
			 String serverPortNumber;
			 Socket clientSocket = null;
			 String inputLine;
			 boolean greetingReceived = false;
			 boolean confirmationReceived = false;
			 
			 
			 BufferedReader inFromUser = new BufferedReader(
			 			new InputStreamReader(System.in));
			 System.out.println("Enter the server's hostname.");
			 serverHostname = inFromUser.readLine();
			 System.out.println("Enter the server's port number.");
			 serverPortNumber = inFromUser.readLine();
			 
			 clientSocket = new Socket(serverHostname, Integer.parseInt(serverPortNumber));
			 
			 BufferedReader inFromServer = new BufferedReader(
						 		new InputStreamReader(
						 			clientSocket.getInputStream()));
			 
			 System.out.println(inputLine = inFromServer.readLine());
				 
				 
			 Pattern p = Pattern.compile("220(.*)");
			 Matcher m = p.matcher(inputLine);
			 	if (m.find()){
					greetingReceived = true;
				}
				else{
					System.out.println("220 Message not received. Closing connection.");
					CloseConnection = true;
					break;
				}
			 
			 
			 PrintWriter outputWriter = new PrintWriter(clientSocket.getOutputStream(), true);
			 outputWriter.println("HELO snapper.cs.unc.edu");
			 
			 while (confirmationReceived == false){
				 System.out.println(inputLine = inFromServer.readLine());
				 
				 p = Pattern.compile("250(.*)");
				 m = p.matcher(inputLine);
				 if (m.find()){
					confirmationReceived = true;
				 }
			 }
			 
			 
			
				String inboxAddress = "outgoing";
				String inputText = "";
				
				String potentialSecondEmail = "";
				Boolean[] machineState = {true,false,false};
					
				File inputFile = new File(inboxAddress);
				try {
					BufferedReader br = new BufferedReader(
					                    new InputStreamReader(
					                    new FileInputStream(inputFile)));
					String line;
					while((line = br.readLine()) != null){
					    inputText = inputText + "\n" + line;
					}
					br.close();
				} catch(IOException e) {
					return;
				}
				
				while(machineState[0] == true){
					String data = "DATA";
					String parsedMailFrom = "MAIL FROM: ";
					String parsedRcptTo = "RCPT TO: ";
					String fileMailFrom = "";
					String fileRcptTo = "";
					String fileMessage = "";
					
					if(!potentialSecondEmail.equals("")){
						inputText = potentialSecondEmail;
					}
					p = Pattern.compile("From: (<.*>)");
					m = p.matcher(inputText);
					if (m.find()){
						fileMailFrom = m.group();
					}
					fileMailFrom = fileMailFrom.replaceFirst("From: ", "");
					parsedMailFrom = parsedMailFrom + fileMailFrom;
					outputWriter.println(parsedMailFrom);
					
					while(machineState[0] == true){
						String serverInput = inFromServer.readLine();
						if(serverInput.matches("5(.*)")){
							System.out.println("QUIT");
							System.exit(0);
						}
						if(serverInput.matches("250(.*)")){
							Pattern q = Pattern.compile("To: (<.*>)");
							Matcher n = q.matcher(inputText);
							if (n.find()){
							    fileRcptTo = n.group();
							}
							fileRcptTo = fileRcptTo.replaceFirst("To: ", "");
							parsedRcptTo = parsedRcptTo + fileRcptTo;
							System.out.println(serverInput);
							outputWriter.println(parsedRcptTo);
							machineState[0] = false;
							machineState[1] = true;
						}
					}
			
					while(machineState[1] == true){
						String serverInput = inFromServer.readLine();
						if(serverInput.matches("5(.*)")){
							System.out.println(serverInput);
							System.out.println("QUIT");
							System.exit(0);
						}
						if(serverInput.matches("250(.*)")){
							System.out.println(serverInput);
							outputWriter.println(data);
							machineState[1] = false;
							machineState[2] = true;
						}
					}
						
					while(machineState[2] == true){
						String serverInput = inFromServer.readLine();
						if(serverInput.matches("5(.*)")){
							System.out.println("QUIT");
							System.exit(0);
						}
						if(serverInput.matches("354(.*)")){
							System.out.println(serverInput);
							fileMessage = inputText;
							fileMessage = fileMessage.replaceFirst("From: " + fileMailFrom + "\n", "");
							fileMessage = fileMessage.replaceFirst("To: " + fileRcptTo + "\n", "");
			
							
							try{
								String[] emailSeparation = fileMessage.split("(\\S)(\\n)From:");
								String email1 = emailSeparation[0];
								String email2 = emailSeparation[1];
								email1.replaceFirst("\n", "");
								System.out.println(email1);
								outputWriter.println(email1);
								machineState[2] = false;
								machineState[0] = true;
								potentialSecondEmail = ("From:" + email2);
							}
							catch(ArrayIndexOutOfBoundsException e){
								fileMessage = fileMessage.replaceFirst("\n", "");
								fileMessage = fileMessage.substring(0,fileMessage.length() - 1);
								outputWriter.println(fileMessage);
								machineState[2] = false;
								outputWriter.println(".");
								serverInput = inFromServer.readLine();
								
								if(serverInput.matches("250(.*)")){
									System.out.println(serverInput);
									outputWriter.println("QUIT");
									System.out.println(inFromServer.readLine());
									clientSocket.close();
									System.exit(0);
								}
								else{
									System.out.println("Error: final 250 message not received");
									System.out.println("QUIT");
									clientSocket.close();
									System.exit(0);
								}
							}
						}
					}
				}
		 }
	 }
}