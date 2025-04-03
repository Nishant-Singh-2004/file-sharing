package server;

import java.util.ArrayList;
import java.util.List;

public class Room {
    private final String name;
    private final boolean isPrivate;
    private final int id;
    private final List<ClientHandler> clients = new ArrayList<>();

    public Room(String name, boolean isPrivate, int id) {
        this.name = name;
        this.isPrivate = isPrivate;
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public boolean isPrivate() {
        return isPrivate;
    }

    public int getId() {
        return id;
    }

    public List<ClientHandler> getClients() {
        return clients;
    }

    public void addClient(ClientHandler client) {
        if (!clients.contains(client)) {
            clients.add(client);
            broadcastToRoom(client.getUsername() + " has joined the room.");
        }
    }

    public void removeClient(ClientHandler client) {
        if (clients.remove(client)) {
            broadcastToRoom(client.getUsername() + " has left the room.");
        }
    }

    public boolean isEmpty() {
        return clients.isEmpty();
    }

    public void broadcastToRoom(String message) {
        for (ClientHandler client : clients) {
            client.sendMessage(message);
        }
    }

    public void broadcastToRoom(String message, ClientHandler sender) {

        boolean isFileCommand = message.startsWith("/file ") ||
                message.startsWith("/chunk ") ||
                message.equals("/endfile");

        for (ClientHandler client : clients) {
            if (isFileCommand || client != sender) {
                client.sendMessage(message);
            }
        }
    }
}