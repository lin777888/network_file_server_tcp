import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.*;
import java.net.*;
import java.util.*;

public class Client {
    // Volatile flag to control the running state of the client
    private static volatile boolean isRunning = true;

    // Method to check if a given string is a valid JSON format
    private static boolean isJson(String line) {
        try {
            JsonParser.parseString(line).getAsJsonObject();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static void main(String[] args) throws IOException {
        // Check for correct command-line arguments: host name and port number
        if (args.length != 2) {
            System.err.println("Usage: java Client <host name> <port number>");
            System.exit(1);
        }

        String hostName = args[0];
        int portNumber = Integer.parseInt(args[1]);

        try (
            // Establish a socket connection to the specified host and port
            Socket socket = new Socket(hostName, portNumber);
            // Output stream to send commands to the server
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            // Input stream to receive responses from the server
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            // Scanner to read user input from the console
            Scanner scanner = new Scanner(System.in)
        ) {
            // Thread for handling user input
            Thread inputThread = new Thread(() -> {
                while (isRunning) {
                    if (!scanner.hasNextLine()) {
                        break;
                    }
                    String command = scanner.nextLine();
                    out.println(command);
                    if ("exit".equalsIgnoreCase(command)) {
                        isRunning = false;
                        break;
                    }
                }
                // Close the scanner when input thread is finished
                scanner.close();
            });

            // Thread for receiving responses from the server
            Thread responseThread = new Thread(() -> {
                try {
                    String line;
                    // Continuously read responses from the server
                    while (isRunning && (line = in.readLine()) != null) {
                        if (!isJson(line)) {
                            System.out.println("Server: " + line);
                            System.out.print("Enter next command: ");
                            continue;
                        }

                        try {
                            // Parse the JSON response
                            JsonObject jsonResponse = JsonParser.parseString(line).getAsJsonObject();
                            String command = jsonResponse.get("command").getAsString();
                            String status = jsonResponse.has("status") ? jsonResponse.get("status").getAsString() : "";

                            // Process the response based on the command type
                            switch (command.toLowerCase()) {
                                case "index":
                                    if ("success".equalsIgnoreCase(status)) {
                                        System.out.println("Available files:");
                                        for (var file : jsonResponse.getAsJsonArray("data")) {
                                            System.out.println(file.getAsString());
                                        }
                                    } else {
                                        System.out.println("No files available.");
                                    }
                                    break;
                                case "get":
                                    if ("success".equalsIgnoreCase(status)) {
                                        System.out.println("File content:");
                                        System.out.println(jsonResponse.get("data").getAsString());
                                    } else {
                                        System.out.println("Failed to retrieve file content.");
                                    }
                                    break;
                                case "exit":
                                    if ("success".equalsIgnoreCase(status)) {
                                        System.out.println("Server acknowledged exit. Closing connection...");
                                        isRunning = false;
                                        System.exit(0);
                                    }
                                    break;
                                default:
                                    System.out.println("Unknown command received.");
                                    break;
                            }
                        } catch (Exception e) {
                            System.out.println("Error parsing JSON response from server: " + line);
                        }
                    }
                } catch (IOException e) {
                    System.err.println("Error reading response from server: " + e.getMessage());
                }
            });

            inputThread.start();
            responseThread.start();

            // Wait for both threads to finish
            inputThread.join();
            responseThread.join();

        } catch (UnknownHostException e) {
            System.err.println("Don't know about host " + hostName);
            System.exit(1);
        } catch (IOException e) {
            System.err.println("Couldn't get I/O for the connection to " + hostName);
            System.exit(1);
        } catch (InterruptedException e) {
            System.err.println("Client interrupted: " + e.getMessage());
            Thread.currentThread().interrupt();
            System.exit(1);
        }
    }
}
