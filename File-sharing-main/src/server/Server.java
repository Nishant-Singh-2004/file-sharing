package server;

import utils.DatabaseConnection;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;

public class Server {

    private ServerSocket server;
    private final int listenPort;
    private boolean running = true;
    private List<ClientHandler> clients = new ArrayList<>();
    private List<Room> rooms = new ArrayList<>();
    private Connection connection;

    public Server(final int listen_port) {
        listenPort = listen_port;
    }

    public Room getRoom(int roomId) {
        for (Room room : rooms) {
            if (room.getId()==roomId) {
                return room;
            }
        }
        return null;
    }

    public void joinRoom(ClientHandler client, int roomId) {
        Room room = getRoom(roomId);
        if (room != null) {
            room.addClient(client);
        }
    }

    public void createRoom(ClientHandler client, String roomName, boolean isPrivate, int roomId) {
        Room room = new Room(roomName,isPrivate,roomId);
        room.addClient(client);
        rooms.add(room);
    }

    public void deleteRoom(ClientHandler client, int roomId) {
        Room room = getRoom(roomId);
        if (room != null) {
            room.removeClient(client);
            if (room.isEmpty()) {
                rooms.remove(room);
            }
        }
    }

    public void init() {
        try {
            server = new ServerSocket(listenPort);
            System.out.println("Server started on port " + listenPort);

            connection= DatabaseConnection.getConnection();
            if (connection == null) {
                System.out.println("Database connection failed");
                return;
            }
            System.out.println("Database connection successful");

            while (running) {
                try {
                    Socket clientSocket = waitForConnection();
                    System.out.println("New client connected: " + clientSocket.getInetAddress().getHostAddress());

                    ClientHandler handler = new ClientHandler(this, clientSocket);
                    clients.add(handler);
                    handler.start();
                } catch (IOException ioe) {
                    ioe.printStackTrace();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            closeConnection();
        }
    }

    private Socket waitForConnection() throws IOException {
        return server.accept();
    }

    public void removeClient(ClientHandler client) {
        clients.remove(client);
    }

    public void broadcast(String message, ClientHandler sender) {
        for (ClientHandler client : clients) {
            if (client != sender) {
                client.sendMessage(message);
            }
        }
    }

    public void closeConnection() {
        try {
            running = false;
            if (server != null && !server.isClosed()) {
                server.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        Server server = new Server(8080);
        server.init();
    }

    public int generateRoomId() {
        int roomId = 0;
        for (Room room : rooms) {
            if (room.getId() > roomId) {
                roomId = room.getId();
            }
        }
        return roomId + 1;
    }
}