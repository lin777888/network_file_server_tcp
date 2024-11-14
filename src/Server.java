import com.google.gson.Gson;
import java.io.*;
import java.net.*;
import java.util.concurrent.*;
import java.util.*;

public class Server {

    // Directory where files are stored. "." means the current directory.
    private static final String DIRECTORY = System.getProperty("user.dir");
    // Port number on which the server listens.
    private static final int PORT = 12345;
    // Prompt message sent to clients for user guidance, including the absolute directory path.
    private static final String PROMPT_MESSAGE = "Enter command (index, get <filename>, or exit) - Current folder: " + DIRECTORY;

    private static class ClientHandler implements Runnable {
        private final Socket clientSocket;
        private static int threadCount = 0;
        private final int threadNumber;

        public ClientHandler(Socket clientSocket) {
            this.clientSocket = clientSocket;
            synchronized (ClientHandler.class) {
                // Increment thread count to give each thread a unique ID.
                threadNumber = ++threadCount;
            }
        }

        // Sends a JSON response to the client.
        private void sendJsonResponse(PrintWriter out, Map<String, ?> response) {
            String jsonResponse = new Gson().toJson(response);
            out.println(jsonResponse);
            out.flush();
        }

        // Helper method to send plain text messages to the client.
        private void sendMessage(PrintWriter out, String message) {
            out.println(message);
            out.flush();
        }

        // Sends a list of all .txt files in the directory to the client.
        private void sendFileList(PrintWriter out, File directory) {
            File[] files = directory.listFiles((dir, name) -> name.endsWith(".txt"));
            List<String> fileList = new ArrayList<>();

            if (files != null && files.length > 0) {
                for (File file : files) {
                    fileList.add(file.getName());
                }
            }

            // Create the response map
            Map<String, Object> responseMap = new HashMap<>();
            responseMap.put("command", "index");
            responseMap.put("data", fileList);
            responseMap.put("status", files != null && files.length > 0 ? "success" : "failed");
            responseMap.put("threadNumber", threadNumber);  // Add thread number to response

            // Send the JSON response to the client
            sendJsonResponse(out, responseMap);
        }

        // Sends the content of a requested file to the client.
        private void sendFileContent(PrintWriter out, String fileName) {
            File file = new File(DIRECTORY, fileName);
            Map<String, Object> responseMap = new HashMap<>();
            responseMap.put("command", "get");
            responseMap.put("threadNumber", threadNumber);  // Add thread number to response

            // Check if file exists and is a regular file.
            if (file.exists() && file.isFile()) {
                StringBuilder fileContent = new StringBuilder();
                try (BufferedReader fileReader = new BufferedReader(new FileReader(file))) {
                    String line;
                    while ((line = fileReader.readLine()) != null) {
                        fileContent.append(line).append("\n");
                    }
                    responseMap.put("data", fileContent.toString().trim());
                    responseMap.put("status", "success");
                } catch (IOException e) {
                    System.err.println("Error reading file content: " + e.getMessage());
                    responseMap.put("status", "failed");
                }
            } else {
                // File does not exist or is not accessible.
                responseMap.put("status", "failed");
            }

            // Send the JSON response to the client
            sendJsonResponse(out, responseMap);
        }

        // Handles the "exit" command from the client.
        private void handleExitCommand(PrintWriter out) {
            // Create the response map for JSON
            Map<String, Object> responseMap = new HashMap<>();
            responseMap.put("command", "exit");
            responseMap.put("status", "success");
            responseMap.put("threadNumber", threadNumber);  // Add thread number to response

            // Send the JSON response to the client
            sendJsonResponse(out, responseMap);

            // Print a message indicating that the thread is terminating
            System.out.println("Thread is terminating.");
        }

        // Handles the invalid command from the client.      
        private void sendInvalidCommandResponse(PrintWriter out) {
            // Create response map for the invalid command
            Map<String, Object> responseMap = new HashMap<>();
            responseMap.put("command", "invalid");
            responseMap.put("status", "failed");
            responseMap.put("message", "Invalid command.");
            responseMap.put("threadNumber", threadNumber);  // Include thread number for tracking

            // Send the JSON response to the client
            sendJsonResponse(out, responseMap);
        }

        // Test Thread by sleeping 
        public static void sleep(int milliseconds) {
                try {
                    Thread.sleep(milliseconds);
                } catch (InterruptedException e) {
                    System.err.println("Sleep interrupted: " + e.getMessage());
                    Thread.currentThread().interrupt(); // Restore the interrupted status
                }
            }

        @Override
        public void run() {
            System.out.println("Handling client with thread number: " + threadNumber);
            try (
                PrintWriter out = new PrintWriter(new BufferedWriter(
                    new OutputStreamWriter(clientSocket.getOutputStream())), true);
                BufferedReader in = new BufferedReader(
                    new InputStreamReader(clientSocket.getInputStream()))
            ) {
                File directory = new File(DIRECTORY);
                sendMessage(out, "Thread " + threadNumber + ": " + PROMPT_MESSAGE);

                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    System.out.println("Received command from thread number " + threadNumber + ": " + inputLine);
                    
                    // Extract the command and argument (if applicable)
                    String[] parts = inputLine.split(" ", 2);
                    String command = parts[0].toLowerCase();
                    String argument = parts.length > 1 ? parts[1].trim() : "";

                    switch (command) {
                        case "exit" -> {
                            // Handle "exit" command to terminate the session
                            handleExitCommand(out);
                            break;
                        }
                        case "index" -> {
                            // Handle "index" command to send list of files
                            sendFileList(out, directory);
                        }
                        case "get" -> {
                            // Handle "get" command to retrieve file content
                            sendFileContent(out, argument);
                        }
                        default -> {
                            // Call the method to handle invalid commands
                            sendInvalidCommandResponse(out);
                        }
                    }

                    // Check if the exit command was received and break the loop
                    if ("exit".equalsIgnoreCase(command)) {
                        break;
                    }

                    // System.out.println("Sleeping for 5 seconds...");
                    // sleep(5000); // Sleep for 5000 milliseconds (5 seconds)
                    // System.out.println("Awake!");

                    // Send prompt message again after processing the command
                    sendMessage(out, "Thread " + threadNumber + ": " + PROMPT_MESSAGE);
                }
            } catch (IOException e) {
                System.out.println("Client connection error: " + e.getMessage());
            } finally {
                // Close the client socket and log thread completion.
                try {
                    clientSocket.close();
                } catch (IOException e) {
                    System.err.println("Error closing client socket: " + e.getMessage());
                }
                System.out.println("Thread number " + threadNumber + " has finished.");
            }
        }
    }

    public static void main(String[] args) {
        // Thread pool to manage client connections.
        ExecutorService threadPool = Executors.newCachedThreadPool();
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Server is running on port " + PORT);
            while (true) {
                // Accept client connection requests.
                Socket clientSocket = serverSocket.accept();
                System.out.println("Client connected: " + clientSocket.getInetAddress().getHostAddress());
                // Assign a new ClientHandler to the connected client.
                threadPool.execute(new ClientHandler(clientSocket));
            }
        } catch (IOException e) {
            System.err.println("Could not start server: " + e.getMessage());
        }
    }
}
