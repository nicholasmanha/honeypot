package honeypot;

import java.io.*;
import java.net.*;
import java.util.logging.*;

class Server {
	private static int port;
	private static String message;

	public static void main(String[] args) {

		if (args.length <= 0) {
			System.err.println("ERROR: no service given");
			System.exit(0);
		}
		switch (args[0]) {
		case "SSH":
			port = 22;
			message = "ssh";
			break;
		case "HTTP":
			port = 80;
			message = "HTTP/1.1 200 OK\n";
			break;
		default:
			System.err.println("ERROR: invalid service given");
			System.exit(0);
		}

		ServerSocket server = null;

		try {
			Server serverObj = new Server();
			// port
			server = new ServerSocket(port);
			server.setReuseAddress(true);

			// running infinite loop for getting
			// client request
			while (true) {

				// socket object to receive incoming client
				// requests
				Socket client = server.accept();

				// Console display for new client
				System.out.println("New client connected " + client.getInetAddress().getHostAddress());

				// create new clientHandler for a new connection
				ClientHandler clientSock = serverObj.new ClientHandler(client);

				// new thread for clientHandler
				new Thread(clientSock).start();
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (server != null) {
				try {
					server.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	// ClientHandler class
	private class ClientHandler implements Runnable {
		Logger logger = Logger.getLogger(Server.class.getName());

		private final Socket clientSocket;

		// Constructor
		public ClientHandler(Socket socket) {
			this.clientSocket = socket;
		}

		public void run() {
			// file handler for logger output
			FileHandler fh;

			PrintWriter out = null;
			BufferedReader in = null;

			// inet for logging purposes
			InetAddress inet = clientSocket.getInetAddress();

			try {
				// use output.txt for logging output, doesn't overwrite logs
				fh = new FileHandler("output.txt", true);

				// log setup
				logger.addHandler(fh);
				SimpleFormatter formatter = new SimpleFormatter();
				fh.setFormatter(formatter);
				logger.setUseParentHandlers(false); // disable logging to terminal

				logger.info("Client connected: " + inet.getHostAddress() + ", reachable: " + inet.isReachable(5000)
						+ ", port: " + clientSocket.getPort());

				// get the outputstream of client
				out = new PrintWriter(clientSocket.getOutputStream(), true);

				// get the inputstream of client
				in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

				// simulate the given service (from args)
				out.println(message);
				logger.info("Sent " + message + " to " + inet.getHostAddress());

				String line;
				while ((line = in.readLine()) != null) {

					logger.info("Client " + inet.getHostAddress() + " said: " + line);
					// writing the received message from
					// client
					System.out.printf(" Sent from the client: %s\n", line);
				}
			} catch (IOException e) {
				logger.severe("Connection Error: " + e + " from " + inet.getHostAddress());
				e.printStackTrace();
			} finally {
				logger.info(inet.getHostAddress() + " has left");
				try {
					if (out != null) {
						out.close();
					}
					if (in != null) {
						in.close();
						clientSocket.close();
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
}
