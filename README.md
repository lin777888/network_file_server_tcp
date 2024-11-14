
# Key Features

1. **Multi-Threaded Client Handling**:
   - The server uses a cached thread pool to handle multiple clients concurrently, enabling each client connection to run independently.

2. **JSON-Based Responses**:
   - All responses to the client are formatted in JSON using the `Gson` library, making the communication structure consistent and easy to parse.

3. **Command Support**:
   - **`index`**: Lists all `.txt` files in the specified directory.
   - **`get <filename>`**: Retrieves the content of a specified `.txt` file.
   - **`exit`**: Closes the client connection.
   - **Error Handling**: Sends a JSON response with an error message if an unrecognized command is received.

4. **Thread Identification**:
   - Each client connection is assigned a unique thread number for easy tracking and debugging. Responses include this thread number, making it easier to identify specific client interactions.

5. **Graceful Connection Handling**:
   - The server closes each client connection safely after the `exit` command, ensuring proper resource cleanup.
   - Additional exception handling is in place to manage unexpected disconnections.

6. **Error Logging**:
   - Detailed error messages are printed to the console for quick troubleshooting, especially useful for tracking client connection issues or file access errors.

7. **Extensibility**:
   - The structure is flexible, allowing you to add new commands or extend functionalities with minimal code changes.

---

# Instructions to Compile and Run Server and Client

### Step 1: Navigate to the `src` Folder

```bash
cd src
```

### Step 2: Compile `Server` and `Client` Classes

```bash
javac -cp "../lib/gson-2.8.9.jar" Server.java Client.java
```

### Step 3: Run `Server` and `Client`

- **Start the Server:**

  ```bash
  java -cp ".:../lib/gson-2.8.9.jar" Server
  ```

- **Start the Client (connects to `localhost` on port `12345`):**

  ```bash
  java -cp ".:../lib/gson-2.8.9.jar" Client localhost 12345
  ```

**Note:** Ensure that `gson-2.8.9.jar` is located in the `lib` folder at the same level as the `src` folder.

---

By following these instructions, you can compile and run the server and client applications and leverage these powerful features for efficient file management over a TCP connection.
