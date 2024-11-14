import java.io.*;
import java.net.*;
import java.util.concurrent.*;

public class TestClient {
    private static final int NUM_CLIENTS = 2;  // Number of clients to simulate
    private static final String HOST_NAME = "localhost";
    private static final int PORT_NUMBER = 12345;

    public static void main(String[] args) {
        ExecutorService executorService = Executors.newFixedThreadPool(NUM_CLIENTS);

        for (int i = 0; i < NUM_CLIENTS; i++) {
            int clientNumber = i + 1;
            executorService.execute(() -> {
                try (Socket socket = new Socket(HOST_NAME, PORT_NUMBER);
                     PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                     BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

                    System.out.println("Client " + clientNumber + " connected to server.");
                                String response;
                    while ((response = in.readLine()) != null) {
                        System.out.println("Client " + clientNumber + " received response: " + response);

                        // Stop reading if it's a non-JSON prompt message or after an expected response
                        if (response.contains("Enter command") || response.contains("Thread ")) {
                            break;
                        }
                    }

                    // Test index command
                    sendCommandAndPrintResponse(out, in, "index", clientNumber);

                    // Test get command with a valid file (assuming file1.txt exists on the server)
                    sendCommandAndPrintResponse(out, in, "get file1.txt", clientNumber);

                    // Test get command with an invalid file
                    sendCommandAndPrintResponse(out, in, "get non_existing_file.txt", clientNumber);

                    // // Test exit command
                    sendCommandAndPrintResponse(out, in, "exit", clientNumber);

                } catch (IOException e) {
                    System.err.println("Client " + clientNumber + " error: " + e.getMessage());
                }
            });
        }

        executorService.shutdown();
        try {
            executorService.awaitTermination(1, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            System.err.println("Executor service interrupted: " + e.getMessage());
            Thread.currentThread().interrupt();
        }

        System.out.println("All clients have finished their tasks.");
    }

    private static void sendCommandAndPrintResponse(PrintWriter out, BufferedReader in, String command, int clientNumber) {
        try {
            System.out.println("Client " + clientNumber + " sending command: " + command);
            out.println(command);
            out.flush();

            // Read server's response
            String response;
            while ((response = in.readLine()) != null) {
                System.out.println("Client " + clientNumber + " received response: " + response);

                // Stop reading if it's a non-JSON prompt message or after an expected response
                if (response.contains("Enter command") || response.contains("Thread ")) {
                    break;
                }
            }
        } catch (IOException e) {
            System.err.println("Client " + clientNumber + " communication error: " + e.getMessage());
        }
    }
}
