package server;

import utils.DatabaseConnection;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class ClientHandler extends Thread {
    private final Server server;
    private final Socket clientSocket;
    private PrintWriter out;
    private BufferedReader in;
    private String username;
    private int currentRoomId = -1;

    public ClientHandler(Server server, Socket socket) {
        this.server = server;
        this.clientSocket = socket;
    }

    @Override
    public void run() {
        try {
            // Initialize input and output streams
            out = new PrintWriter(clientSocket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

            // First message should be the username
            String command = in.readLine();
            username = command.split(":")[1];
            System.out.println("User connected: " + username);
            processCommand(command);

            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                processCommand(inputLine);
            }
        } catch (IOException e) {
            System.out.println("Error handling client: " + e.getMessage());
        } finally {
            cleanup();
        }
    }

    private void processCommand(String message) {
        if (message.startsWith("/")) {
            if (message.startsWith("/file ")) {
                // File transfer initiated
                String[] parts = message.split(" ", 3);
                String fileName = parts[1];
                int fileSize = Integer.parseInt(parts[2]);

                // Notify the user that file transfer is starting
                if (currentRoomId != -1) {
                    Room room = server.getRoom(currentRoomId);
                    if (room != null) {
                        room.broadcastToRoom(username + " is sending a file: " + fileName +
                                " (" + (fileSize / 1024) + " KB)", this);
                        // Forward file command to all users in the room
                        room.broadcastToRoom(message, this);
                    }
                } else {
                    server.broadcast(username + " is sending a file: " + fileName +
                            " (" + (fileSize / 1024) + " KB)", this);
                    server.broadcast(message, null);
                }
                return;
            } else if (message.startsWith("/chunk ") || message.equals("/endfile")) {
                // Forward file chunks to appropriate recipients
                if (currentRoomId != -1) {
                    Room room = server.getRoom(currentRoomId);
                    if (room != null) {
                        room.broadcastToRoom(message, null);
                    }
                } else {
                    server.broadcast(message, null);
                }
                return;
            }

            // Handle other commands
            String[] parts = message.split(" ", 3);
            switch (parts[0]) {
                case "/create":
                    if (parts.length >= 3) {
                        boolean isPrivate = parts[1].equalsIgnoreCase("private");
                        String roomName = parts[2];
                        int roomId = server.generateRoomId();
                        server.createRoom(this, roomName, isPrivate, roomId);
                        currentRoomId = roomId;
                        sendMessage("Room created with ID: " + roomId);
                    }
                    break;
                case "/join":
                    if (parts.length >= 2) {
                        try {
                            int roomId = Integer.parseInt(parts[1]);
                            server.joinRoom(this, roomId);
                            currentRoomId = roomId;
                            sendMessage("Joined room: " + roomId);
                        } catch (NumberFormatException e) {
                            sendMessage("Invalid room ID format");
                        }
                    }
                    break;
                case "/leave":
                    if (currentRoomId != -1) {
                        server.deleteRoom(this, currentRoomId);
                        sendMessage("Left room: " + currentRoomId);
                        currentRoomId = -1;
                    } else {
                        sendMessage("You are not in any room");
                    }
                    break;
                default:
                    sendMessage("Unknown command: " + parts[0]);
            }
        } else {
            // Regular message - send to current room or broadcast to all
            if(message.trim().isEmpty()){
                return;
            }
            if(message.startsWith("USER:")){
                try{
                    Connection con = DatabaseConnection.getConnection();

                    PreparedStatement st = con.prepareStatement("insert into user(username) values(?)");
                    st.setString(1,username);
                    if(st.executeUpdate()>0){
                        System.out.println("user registered : "+ username);
                    }
                    return;
                } catch (SQLException e){
                    System.out.println("Error registering user.");
                    throw new RuntimeException(e);
                }
            }
            if (currentRoomId != -1) {
                Room room = server.getRoom(currentRoomId);
                if (room != null) {
                    room.broadcastToRoom(username + ": " + message, this);
                }
            } else {
                server.broadcast(username + ": " + message, this);
            }
        }
    }

    public void sendMessage(String message) {
        out.println(message);
    }

    private void cleanup() {
        try {
            if (currentRoomId != -1) {
                server.deleteRoom(this, currentRoomId);
            }
            server.removeClient(this);

            if (in != null) in.close();
            if (out != null) out.close();
            if (clientSocket != null && !clientSocket.isClosed()) {
                clientSocket.close();
            }

            System.out.println("Connection with " + username + " closed");
        } catch (IOException e) {
            System.out.println("Error during cleanup: " + e.getMessage());
        }
    }

    public String getUsername() {
        return username;
    }
}