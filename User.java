//package broadcast;

import java.io.DataInputStream;
import java.io.PrintStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.net.Socket;
import java.net.UnknownHostException;


public class User extends Thread {

	// The user socket
	private static Socket userSocket = null;
	// The output stream
	private static PrintStream output_stream = null;
	// The input stream
	private static BufferedReader input_stream = null;

	private static BufferedReader inputLine = null;
	private static boolean closed = false;

	public static void main(String[] args) {

		// The default port.
		int portNumber = 8000;
		// The default host.
		String host = "localhost";

		if (args.length < 2) {
			System.out
			.println("Usage: java User <host> <portNumber>\n"
					+ "Now using host=" + host + ", portNumber=" + portNumber);
		} else {
			host = args[0];
			portNumber = Integer.valueOf(args[1]).intValue();
		}

		/*
		 * Open a socket on a given host and port. Open input and output streams.
		 */
		try {
			userSocket = new Socket(host, portNumber);
			inputLine = new BufferedReader(new InputStreamReader(System.in));
			output_stream = new PrintStream(userSocket.getOutputStream());
			input_stream = new BufferedReader(new InputStreamReader(userSocket.getInputStream()));
		} catch (UnknownHostException e) {
			System.err.println("Don't know about host " + host);
		} catch (IOException e) {
			System.err.println("Couldn't get I/O for the connection to the host "
					+ host);
		}

		/*
		 * If everything has been initialized then we want to write some data to the
		 * socket we have opened a connection to on port portNumber.
		 */
		if (userSocket != null && output_stream != null && input_stream != null) {
			try {                
				/* Create a thread to read from the server. */
				new Thread(new User()).start();

				// Get user name and join the social net
				String username = inputLine.readLine().trim();
				output_stream.println("#join " + username);

				// While the connection is still open
				while (!closed) {
					String userMessage = new String();
					String userInput = inputLine.readLine().trim();
					
					// Read user input and send protocol message to server
					if (userInput.equals("Exit")) {
						output_stream.println("#Bye");
					} else if (userInput.startsWith("@connect")) {
						output_stream.println("#friendme " + userInput.split("\\s", 2)[1]);
					} else if (userInput.startsWith("@friend")) {
						output_stream.println("#friends " + userInput.split("\\s", 2)[1]);
					} else if (userInput.startsWith("@deny")) {
						output_stream.println("#DenyFriendRequest " + userInput.split("\\s", 2)[1]);
					} else if (userInput.startsWith("@disconnect")) {
						output_stream.println("#unfriend " + userInput.split("\\s", 2)[1]);
					} else {
						output_stream.println("#status " + userInput);
					}

				}
				/*
				 * Close the output stream, close the input stream, close the socket.
				 */
				input_stream.close();
				output_stream.close();
				userSocket.close();
			} catch (IOException e) {
				System.err.println("IOException:  " + e);
			}
		}
	}

	/*
	 * Create a thread to read from the server.
	 */
	public void run() {
		/*
		 * Keep on reading from the socket till we receive a Bye from the
		 * server. Once we received that then we want to break.
		 */
		String responseLine;
		
		try {
			while ((responseLine = input_stream.readLine()) != null) {

				// Display on console based on what protocol message we get from server.
				if (responseLine.startsWith("#welcome")) {
					System.out.println("The connection has been established");
				} else if (responseLine.startsWith("#busy")) {
					System.out.println("Try later");
					break;
				} else if (responseLine.startsWith("#Bye")) {
					System.out.println("Connection close");
				} else if (responseLine.startsWith("#newuser")) {
					System.out.println(responseLine.split("\\s", 2)[1] + " has joined");
				} else if (responseLine.startsWith("#statusPosted")) {
					System.out.println("Your status has been posted succesfully!");
				} else if (responseLine.startsWith("#newStatus")) {
					String[] words = responseLine.split("\\s", 3);
					System.out.println("[" + words[1] + "] " + words[2]);
				} else if (responseLine.startsWith("#Leave")) {
					System.out.println(responseLine.split("\\s", 2)[1] + " has left");
				} else if (responseLine.startsWith("#friendme")) {
					String reqname = responseLine.split("\\s", 2)[1];
					System.out.println("Reply @friend or @deny with name for " + reqname);
				} else if (responseLine.startsWith("#OKfriends")) {
					String user1 = responseLine.split("\\s", 3)[1];
					String user2 = responseLine.split("\\s", 3)[2];
					System.out.println(user1 + " and " + user2 + " are now friends");
				} else if (responseLine.startsWith("#FriendRequestDenied")) {
					String denyname = responseLine.split("\\s", 2)[1];
					System.out.println(denyname + " rejected your friend request");
				} else if (responseLine.startsWith("#NotFriends")) {
					String use1 = responseLine.split("\\s", 3)[1];
					String use2 = responseLine.split("\\s", 3)[2];
					System.out.println(use1 + " and " + use2 + " are no longer friends");
				} else {System.out.println("No corresponding action");}
			}
			closed = true;
			output_stream.close();
			input_stream.close();
			userSocket.close();
		} catch (IOException e) {
			System.err.println("IOException:  " + e);
		}
	}
}



