#!/bin/bash

# Compile server and client
echo "Compiling Java code..."
javac -cp "../lib/gson-2.8.9.jar" Server.java TestClient.java

# Run server in the background and redirect output to a log file
echo "Starting server..."
nohup java -cp ".:../lib/gson-2.8.9.jar" Server > server_output.log 2>&1 &
SERVER_PID=$!
sleep 5  # Give the server some time to start

# Check if the server is running
if ps -p $SERVER_PID > /dev/null
then
    echo "Server started successfully, PID: $SERVER_PID"
else
    echo "Server failed to start. Check server_output.log for details."
    exit 1
fi

# Run the test client
echo "Running test client..."
java -cp ".:../lib/gson-2.8.9.jar" TestClient

# Stop the server if it is still running
if ps -p $SERVER_PID > /dev/null
then
    echo "Stopping server..."
    kill $SERVER_PID
else
    echo "Server process has already exited."
fi
