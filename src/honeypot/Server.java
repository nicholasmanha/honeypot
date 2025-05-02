package honeypot;

import java.io.*;
import java.net.*;
import java.util.HexFormat;
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
			message = "SSH-2.0-OpenSSH_9.9p1 Debian-3";
			break;
		case "HTTP":
			port = 80;
			message = "HTTP/1.1 200 OK\r\n"
					+ "Server: nginx/1.18.0 (Ubuntu)\r\n"
					+ "Date: Thu, 01 May 2025 21:23:17 GMT\r\n"
					+ "Content-Type: text/html\r\n"
					+ "Content-Length: 5124\r\n"
					+ "Last-Modified: Wed, 22 Mar 2023 14:54:48 GMT\r\n"
					+ "Connection: keep-alive\r\n"
					+ "ETag: \"641b16b8-1404\"\r\n"
					+ "Referrer-Policy: strict-origin-when-cross-origin\r\n"
					+ "X-Content-Type-Options: nosniff\r\n"
					+ "Feature-Policy: accelerometer 'none'; camera 'none'; geolocation 'none'; gyroscope 'none'; magnetometer 'none'; microphone 'none'; payment 'none'; usb 'none'\r\n"
					+ "Content-Security-Policy: default-src 'self'; script-src cdnjs.cloudflare.com 'self'; style-src cdnjs.cloudflare.com 'self' fonts.googleapis.com 'unsafe-inline'; font-src fonts.googleapis.com fonts.gstatic.com cdnjs.cloudflare.com; frame-ancestors 'none'; report-uri https://scotthelme.report-uri.com/r/d/csp/enforce\r\n"
					+ "Accept-Ranges: bytes\r\n" + "\r\n" + "<h1>Welcome to Honeypot</h1>";
			break;
		case "telnet":
			port = 23;
			message = "fffb03fffb01fffd18fffd1ffffd24fffd27fffd000d0a";
			break;
		case "ftp":
			port = 21;
			message = "220 ftp.scene.org FTP server (SceneOrgFTPD-2.4.4) ready.\r\n";
			break;
		case "smtp":
			port = 25;
			message = "220 zim.gshapiro.net ESMTP Sendmail 8.18.1.10/8.18.1.10; Fri, 2 May 2025 00:33:23 GMT";
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
				if(port != 23) {
					out.println(message);
				}
				else {
					OutputStream outputStream = clientSocket.getOutputStream();
					
					// convert string of hex into array of bytes
					byte[] telnetResponse = HexFormat.of().parseHex(message);
					
					outputStream.write(telnetResponse);
				}
				
				logger.info("Sent " + message + " to " + inet.getHostAddress());

				String line;
				while ((line = in.readLine()) != null) {

					logger.info("Client " + inet.getHostAddress() + " said: " + line);
					// writing the received message from
					// client
					System.out.printf(" Sent from the client: %s\n", line);
					out.println(message);
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
