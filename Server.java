package assn5;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Server {

	public static void main(String argv[]) throws Exception
	{
		boolean SocketOK = false;
		ServerSocket welcomeSocket = null;
		BufferedReader inFromUser = new BufferedReader(
	 									new InputStreamReader(System.in));
		
		System.out.println("Enter receiving port number.");
		
		while (SocketOK == false){
			try {
				welcomeSocket = new ServerSocket(Integer.parseInt(inFromUser.readLine()));
				SocketOK = true;
			} catch (BindException e) {
				System.out.println("Can't use that socket. Try another socket number.");
			}
			catch (IllegalArgumentException e) {
				System.out.println("Can't use that socket. Try another socket number.");
			}
		}
while (true){
		while (true){
			try{
				
					Socket connectionSocket = null;
					String clientSentence = null;
					boolean closeConnection = false;
					boolean heloReceived = false;
					String fileWriteName = null;
				
					while (connectionSocket == null){
						connectionSocket = welcomeSocket.accept();
					}
					
					
					
					PrintWriter outputWriter = new PrintWriter(connectionSocket.getOutputStream(), true);
					BufferedReader inFromClient = new BufferedReader(
															new InputStreamReader(
																	connectionSocket.getInputStream()));
					
					outputWriter.println("220 classroom.cs.unc.edu");
					
					
					clientSentence = inFromClient.readLine();
					System.out.println(clientSentence);
						
					Pattern p = Pattern.compile("HELO(.*)");
					Matcher m = p.matcher(clientSentence);
					if (m.find()){
						heloReceived = true;
						clientSentence = clientSentence.replaceFirst("HELO ", "");
					}
					else{
						System.out.println("HELO not received. Closing connection.");
						break;
					}
					
					outputWriter.println("250 Hello " + clientSentence + ", pleased to meet you.");
					
				
					while(closeConnection == false){
						boolean mailFromAccepted = false;
						boolean dataAccepted = false;
						boolean bodyAccepted = false;
						boolean ableToReceiveData = false;
						String finalMailFrom = null;
						String rcptToOrData = null;
						String emailBody = "";
						List<String> rcptToArray = new ArrayList<String>();
						
						String[] errorMsgs = new String[3];
						errorMsgs[0] = "500 Syntax error: command unrecognized";
						errorMsgs[1] = "501 Syntax error in parameters or arguments";
						errorMsgs[2] = "503 Bad sequence of commands";
				
						String newString = inFromClient.readLine();
						System.out.println(newString);
						finalMailFrom = ParseMailFrom(newString, errorMsgs);
						if (finalMailFrom != null){
							mailFromAccepted = true;
							outputWriter.println("250 OK");
						}
						else{
							System.out.println("Ill-formatted MAIL FROM command. Closing Connection.");
							break;
						}
						
						
						
						
						while (dataAccepted == false){
							newString = inFromClient.readLine();
							System.out.println(newString);
							rcptToOrData = ParseRcptTo(newString, errorMsgs, ableToReceiveData);
							if (rcptToOrData != null){
								rcptToArray.add(rcptToOrData);
								ableToReceiveData = true;
								outputWriter.println("250 OK");
							}
							if (rcptToOrData == null){
								if (ableToReceiveData == true){
									rcptToOrData = ParseData(newString, errorMsgs, ableToReceiveData);
									if (rcptToOrData != null){
										dataAccepted = true;
										outputWriter.println("354 Start mail input; end with <CRLF>.<CRLF>");
									}
								}
							}	
						}
						
						while (bodyAccepted == false){
							newString = inFromClient.readLine();
							System.out.println(newString);
							if (newString.equals(".")){
							    outputWriter.println("250 OK");
								bodyAccepted = true;
							}
							else{
								emailBody = emailBody + newString + "\n";
							}
						}
						
						while (closeConnection == false){
							newString = inFromClient.readLine();
							System.out.println(newString);
							if (newString.equals("QUIT")){
								outputWriter.println("221 Bye");
								closeConnection = true;
							}
						}
						
						for (int i = 0; i < rcptToArray.size(); i++){
							try {
								fileWriteName = rcptToArray.get(i);
								fileWriteName = fileWriteName.replaceFirst("(.*)@", "");
								PrintWriter writer = new PrintWriter(fileWriteName, "UTF-8");
								writer.println("From: <" + finalMailFrom + ">");
								for (int j = 0; j < rcptToArray.size(); j++){
									writer.println("To: <" + rcptToArray.get(j) + ">");
								}
								writer.println(emailBody);
								writer.close();
							} catch (FileNotFoundException e) {
								e.printStackTrace();
							} catch (UnsupportedEncodingException e) {
								e.printStackTrace();
							}
						}
					}
				}
				catch(NullPointerException e){
					System.out.println("Client interrupted connection. Returning to beginning of processing loop");
					break;
				}
			}
		}
	}
	
	static String ParseMailFrom(String inputline, String[] errorMsgs) {
    
		inputline = inputline.trim();    
		String delims = "[:>]";
		String[] tokens = inputline.split(delims);
		
		if((tokens[0].matches("RCPT TO"))&&(!(tokens[0].matches("rcpt to")))){
			System.out.println(errorMsgs[2]);
			return null;
		}
		if((tokens[0].matches("DATA"))&&(!(tokens[0].matches("data")))){
			System.out.println(errorMsgs[2]);
			return null;
		}
		if(!(tokens[0].matches("MAIL FROM"))&&(!(tokens[0].matches("mail from")))){
			System.out.println(errorMsgs[0]);
			return null;
		}
		if((inputline.matches("(.*)\\SMAIL FROM(.*)"))&&!(tokens[0].matches("(.*)\\Smail from(.*)"))){
			System.out.println(errorMsgs[0]);
	        return null;
	    }
	    if((!inputline.matches("(.*)<\\S(.*)"))){
	    	System.out.println(errorMsgs[1]);
	        return null;
	    }
	    if((!inputline.matches("(.*)\\S@(.*)"))){
	    	System.out.println(errorMsgs[1]);
	        return null;
	    }
	    if(inputline.matches("(.*)<(.*)\\w(.*)\\W\\w(.*)@(.*)")){
	    	System.out.println(errorMsgs[1]);
	        return null;
	    }
	    if((!tokens[1].matches("<(.*)@(.*)"))&&(!(tokens.length==2))){
	    	System.out.println(errorMsgs[1]);
	        return null;
	    }
	    if((tokens[1].matches("<@(.*)"))){
	    	System.out.println(errorMsgs[1]);
	        return null;
	    }
	    if((tokens[1].matches("(.*)\\S(.*)<(.*)"))){
	    	System.out.println(errorMsgs[1]);
	        return null;
	    }
	    if((inputline.matches("(.*)@(.*)\\s(.*)>"))){
	    	System.out.println(errorMsgs[1]);
	        return null;
	    }
	    if((inputline.matches("(.*)@\\W(.*)>"))){
	    	System.out.println(errorMsgs[1]);
	        return null;
	    }
	    if(inputline.matches("(.*)[.][.](.*)")){
	    	System.out.println(errorMsgs[1]);
	        return null;
	    }
	    if((inputline.matches("(.*)>\\S"))){
	    	System.out.println(errorMsgs[1]);
	        return null;
	    }
	    if(!inputline.matches("(.*)<(.*)>(.*)")){
	    	System.out.println(errorMsgs[1]);
	        return null;
	    }
	
	    Pattern p = Pattern.compile("(<.*>)");
	    Matcher m = p.matcher(inputline);
	    if (m.find()){
	    inputline = m.group();
	    inputline = inputline.replace("<", "");
	    inputline = inputline.replace(">", "");
	    return inputline;
	    }
	    return inputline;
	}
	
	
	static String ParseRcptTo(String inputline, String[] errorMsgs, boolean ableToReceiveData) {
	    
		inputline = inputline.trim();    
		String delims = "[:>]";
		String[] tokens = inputline.split(delims);

		if((tokens[0].matches("MAIL FROM"))&&(!(tokens[0].matches("mail from")))){
			System.out.println(errorMsgs[2]);
			return null;
		}
		if(ableToReceiveData == false){
			if(inputline.matches("DATA")||(inputline.matches("data"))){
				System.out.println(errorMsgs[2]);
				return null;
			}
		}
		if(ableToReceiveData == true){
			if(inputline.matches("DATA")||(inputline.matches("data"))){
				return null;
			}
		}
		if(!(tokens[0].matches("RCPT TO"))&&(!(tokens[0].matches("rcpt to")))){
			System.out.println(errorMsgs[0]);
			return null;
		}
		if((inputline.matches("(.*)\\SRCPT TO(.*)"))&&!(tokens[0].matches("(.*)\\Srcpt to(.*)"))){
			System.out.println(errorMsgs[0]);
	        return null;
	    }
	    if((!inputline.matches("(.*)<\\S(.*)"))){
	    	System.out.println(errorMsgs[1]);
	        return null;
	    }
	    if((!inputline.matches("(.*)\\S@(.*)"))){
	    	System.out.println(errorMsgs[1]);
	        return null;
	    }
	    if(inputline.matches("(.*)<(.*)\\w(.*)\\W\\w(.*)@(.*)")){
	    	System.out.println(errorMsgs[1]);
	        return null;
	    }
	    if((!tokens[1].matches("<(.*)@(.*)"))&&(!(tokens.length==2))){
	    	System.out.println(errorMsgs[1]);
	        return null;
	    }
	    if((tokens[1].matches("<@(.*)"))){
	    	System.out.println(errorMsgs[1]);
	        return null;
	    }
	    if((tokens[1].matches("(.*)\\S(.*)<(.*)"))){
	    	System.out.println(errorMsgs[1]);
	        return null;
	    }
	    if((inputline.matches("(.*)@(.*)\\s(.*)>"))){
	    	System.out.println(errorMsgs[1]);
	        return null;
	    }
	    if((inputline.matches("(.*)@\\W(.*)>"))){
	    	System.out.println(errorMsgs[1]);
	        return null;
	    }
	    if(inputline.matches("(.*)[.][.](.*)")){
	    	System.out.println(errorMsgs[1]);
	        return null;
	    }
	    if((inputline.matches("(.*)>\\S"))){
	    	return null;
	    }
	    if(!inputline.matches("(.*)<(.*)>(.*)")){
	    	System.out.println(errorMsgs[1]);
	        return null;
	    }
	
	    Pattern p = Pattern.compile("(<.*>)");
	    Matcher m = p.matcher(inputline);
	    if (m.find()){
	    	inputline = m.group();
	    	inputline = inputline.replace("<", "");
	    	inputline = inputline.replace(">", "");
	    	return inputline;
	    }
	    return inputline;
	}
	
	
	static String ParseData(String inputline, String[] errorMsgs, boolean ableToReceiveData) {
		
		inputline = inputline.trim();
		
		if(!(inputline.matches("DATA"))&&(!(inputline.matches("data")))){   
			if (ableToReceiveData == true){
				return null;
			}
			System.out.println(errorMsgs[0]);
			return null;
		}		
			
		return inputline;
	}
	
}