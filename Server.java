//package broadcast;

import java.io.DataInputStream;
import java.io.PrintStream;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.ServerSocket;
import java.util.*;



/*
 * A server that delivers status messages to other users.
 */
public class Server {

	// Create a socket for the server 
	private static ServerSocket serverSocket = null;
	// Create a socket for the server 
	private static Socket userSocket = null;
	// Maximum number of users 
	private static int maxUsersCount = 5;
	// An array of threads for users
	private static userThread[] threads = null;


	public static void main(String args[]) {

		// The default port number.
		int portNumber = 8000;
		if (args.length < 2) {
			System.out.println("Usage: java Server <portNumber>\n"
					+ "Now using port number=" + portNumber + "\n" +
					"Maximum user count=" + maxUsersCount);
		} else {
			portNumber = Integer.valueOf(args[0]).intValue();
			maxUsersCount = Integer.valueOf(args[1]).intValue();
		}

		System.out.println("Server now using port number=" + portNumber + "\n" + "Maximum user count=" + maxUsersCount);
		
		// Using it to contain all users
		userThread[] threads = new userThread[maxUsersCount];


		/*
		 * Open a server socket on the portNumber (default 8000). 
		 */
		try {
			serverSocket = new ServerSocket(portNumber);
		} catch (IOException e) {
			System.out.println(e);
		}

		/*
		 * Create a user socket for each connection and pass it to a new user
		 * thread.
		 */
		while (true) {
			try {
				userSocket = serverSocket.accept();
				int i = 0;
				for (i = 0; i < maxUsersCount; i++) {
					if (threads[i] == null) {
						threads[i] = new userThread(userSocket, threads);
						threads[i].start();
						break;
					}
				}
				if (i == maxUsersCount) {
					PrintStream output_stream = new PrintStream(userSocket.getOutputStream());
					output_stream.println("#busy");
					output_stream.close();
					userSocket.close();
				}
			} catch (IOException e) {
				System.out.println(e);
			}
		}
	}
}

/*
 * Threads
 */
class userThread extends Thread {

	private String userName = null;
	private BufferedReader input_stream = null;
	private PrintStream output_stream = null;
	private Socket userSocket = null;
	private final userThread[] threads;
	private int maxUsersCount;
	// each thread has a friends array (max friends = all other threads)
	private final userThread[] friends;

	public userThread(Socket userSocket, userThread[] threads) {
		this.userSocket = userSocket;
		this.threads = threads;
		maxUsersCount = threads.length;
		this.friends = new userThread[maxUsersCount];
	}
	

	public void run() {
		int maxUsersCount = this.maxUsersCount;
		userThread[] threads = this.threads;
		userThread[] friends = this.friends;

		/**
		 * welcome -- newuser username ]]]
		 * busy ]]]
		 * newStatus username text when status ]]]
		 * Leave username ]]]
		 */
		try {
			/*
			 * Create input and output streams for this client.
			 * Read user name.
			 */
			input_stream = new BufferedReader(new InputStreamReader(userSocket.getInputStream()));
			output_stream = new PrintStream(userSocket.getOutputStream());
			
			/* Welcome the new user. */
			// one to one
			String line = input_stream.readLine().trim();
			if (line.startsWith("#join")) {
				this.userName = line.split("\\s", 2)[1];
				output_stream.println("#welcome");
			}

			// broadcast
			synchronized (userThread.class) {
				for (int i = 0; i < maxUsersCount; i++) {
					if ((threads[i] != null)&&(threads[i] != this)) {
						threads[i].output_stream.println("#newuser " + this.userName);
					}
				}
			}


			/* Start the conversation. */
			while (true) {
				String action = input_stream.readLine().trim();
				if (action.startsWith("#Bye")) {
					output_stream.println("#Bye");
					synchronized (userThread.class) {
						for (int i = 0; i < maxUsersCount; i++) {
							if ((threads[i] != null)&&(threads[i] != this)) {
								threads[i].output_stream.println("#Leave " + this.userName);
							}
						}
					}
					break;
				} else if (action.startsWith("#status")) {
					output_stream.println("#statusPosted");
					synchronized (userThread.class) {
						for (int i = 0; i < maxUsersCount; i++) {
							if ((threads[i] != null)&&(threads[i] != this)) {
								threads[i].output_stream.println("#newStatus " + this.userName +" "+ action.split("\\s", 2)[1]);
							}
						}
					}
				} else if (action.startsWith("#friendme")) {
					String target = action.split("\\s", 2)[1];
					synchronized (userThread.class) {
						for (int i = 0; i < maxUsersCount; i++) {
							if ((threads[i] != null) && (threads[i].userName.equals(target))) {
								threads[i].output_stream.println("#friendme " + this.userName);
								break;
							}
						}
					}
				} else if (action.startsWith("#friends")) {
					String reqname = action.split("\\s", 2)[1];
					userThread req = null;
					synchronized (userThread.class) {
						for (int i = 0; i < maxUsersCount; i++) {
							if ((threads[i] != null) && (threads[i].userName.equals(reqname))) {
								req = threads[i];
								break;
							}
						}
					}
					synchronized (userThread.class){
						for (int i = 0; i < maxUsersCount; i++) {
							if (friends[i] == null) {
								friends[i] = req;
								break;
							}
						}
					}
					synchronized (userThread.class) {
						for (int i = 0; i < maxUsersCount; i++) {
							if (req.friends[i] == null) {
								req.friends[i] = this;
								break;
							}
						}
					}	
					synchronized (userThread.class) {
						for (int i = 0; i < maxUsersCount; i++) {
							if ((threads[i]!=null)) {
								threads[i].output_stream.println("#OKfriends " + this.userName + " " + req.userName);
							}
						}
					}
				} else if (action.startsWith("#DenyFriendRequest")) {
					String denytarget = action.split("\\s", 2)[1];
					synchronized (userThread.class) {
						for (int i = 0; i < maxUsersCount; i++) {
							if ((threads[i] != null) && (threads[i].userName.equals(denytarget))) {
								threads[i].output_stream.println("#FriendRequestDenied " + this.userName);
								break;
							}
						}
					}
				} else if (action.startsWith("#unfriend")) {
					String deletetarget = action.split("\\s", 2)[1];
					userThread del = null;
					synchronized (userThread.class) {
						for (int i = 0; i < maxUsersCount; i++) {
							if ((threads[i] != null) && (threads[i].userName.equals(deletetarget))) {
								del = threads[i];
								break;
							}
						}
					}
					synchronized (userThread.class) {
						for (int i = 0; i < maxUsersCount; i++) {
							if (friends[i].userName.equals(deletetarget)) {
								friends[i] = null;
								break;
							}
						}
					}
					synchronized (userThread.class) {
						for (int i = 0; i < maxUsersCount; i++) {
							if (del.friends[i].userName.equals(this.userName)) {
								del.friends[i] = null;
								break;
							}
						}
					}
					synchronized (userThread.class) {
						for (int i = 0; i < maxUsersCount; i++) {
							if (threads[i] != null) {
								threads[i].output_stream.println("#NotFriends " + this.userName + " " + del.userName);
							}
						}
					}
				}
			}

			// conversation ended.

			/*
			 * Clean up. Set the current thread variable to null so that a new user
			 * could be accepted by the server.
			 */
			synchronized (userThread.class) {
				for (int i = 0; i < maxUsersCount; i++) {
					if (threads[i] == this) {
						threads[i] = null;
					}
				}
			}
			/*
			 * Close the output stream, close the input stream, close the socket.
			 */
			input_stream.close();
			output_stream.close();
			userSocket.close();
		} catch (IOException e) {
		}
	}
}




