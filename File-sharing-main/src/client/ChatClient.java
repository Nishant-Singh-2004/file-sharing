package client;

import javax.swing.filechooser.FileNameExtensionFilter;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Base64;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ChatClient extends JFrame {
    private static final String SERVER_ADDRESS = "localhost";
    private static final int SERVER_PORT = 8080;

    private JButton sendFileButton;
    private JFileChooser fileChooser;
    private File selectedFile;
    private static final int MAX_FILE_SIZE = 5 * 1024 * 1024;

    private StringBuilder fileBuffer = new StringBuilder();
    private String incomingFileName = "";
    private int incomingFileSize = 0;
    private int expectedChunks = 0;
    private int receivedChunks = 0;

    private JTextArea chatArea;
    private JTextField messageField;
    private JTextField usernameField;
    private JButton connectButton;
    private JButton sendButton;
    private JComboBox<String> commandBox;
    private JTextField roomIdField;
    private JTextField roomNameField;
    private JCheckBox privateRoomCheckbox;

    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private ExecutorService executorService;
    private boolean connected = false;

    public ChatClient() {
        setTitle("Chat Client");
        setSize(600, 500);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        initializeComponents();
        addListeners();
        executorService = Executors.newSingleThreadExecutor();

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                disconnect();
                executorService.shutdown();
            }
        });
    }

    private void initializeComponents() {
        // Chat area
        chatArea = new JTextArea();
        chatArea.setEditable(false);
        chatArea.setLineWrap(true);
        JScrollPane scrollPane = new JScrollPane(chatArea);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);

        // Login panel
        JPanel loginPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        usernameField = new JTextField(15);
        connectButton = new JButton("Connect");
        loginPanel.add(new JLabel("Username:"));
        loginPanel.add(usernameField);
        loginPanel.add(connectButton);

        // Room controls panel
        JPanel roomPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        String[] commands = {"Regular Message", "/create", "/join", "/leave", "/file"};
        commandBox = new JComboBox<>(commands);
        roomIdField = new JTextField(5);
        roomNameField = new JTextField(10);
        privateRoomCheckbox = new JCheckBox("Private");

        roomPanel.add(new JLabel("Command:"));
        roomPanel.add(commandBox);
        roomPanel.add(new JLabel("Room ID:"));
        roomPanel.add(roomIdField);
        roomPanel.add(new JLabel("Room Name:"));
        roomPanel.add(roomNameField);
        roomPanel.add(privateRoomCheckbox);

        // Message panel
        JPanel messagePanel = new JPanel(new BorderLayout());
        messageField = new JTextField();
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        sendButton = new JButton("Send");
        sendFileButton = new JButton("Send File");
        buttonPanel.add(sendFileButton);
        buttonPanel.add(sendButton);
        messagePanel.add(messageField, BorderLayout.CENTER);
        messagePanel.add(buttonPanel, BorderLayout.EAST);

        fileChooser = new JFileChooser();
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fileChooser.setMultiSelectionEnabled(false);
        fileChooser.setFileFilter(new FileNameExtensionFilter(
                "All Files", "txt", "pdf", "doc", "jpg", "png", "gif"));


        // Main layout
        setLayout(new BorderLayout());
        add(loginPanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
        add(roomPanel, BorderLayout.SOUTH);
        add(messagePanel, BorderLayout.SOUTH);

        // Initial component states
        sendButton.setEnabled(false);
        messageField.setEnabled(false);
        commandBox.setEnabled(false);
        roomIdField.setEnabled(false);
        roomNameField.setEnabled(false);
        privateRoomCheckbox.setEnabled(false);
        sendFileButton.setEnabled(false);

        // Re-layout after adding room panel
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.add(roomPanel, BorderLayout.NORTH);
        bottomPanel.add(messagePanel, BorderLayout.SOUTH);
        add(bottomPanel, BorderLayout.SOUTH);
    }

    private void addListeners() {
        connectButton.addActionListener(e -> {
            if (!connected) {
                connect();
            } else {
                disconnect();
            }
        });

        sendFileButton.addActionListener(e -> selectAndSendFile());

        sendButton.addActionListener(e -> sendMessage());

        messageField.addActionListener(e -> sendMessage());

        commandBox.addActionListener(e -> updateUIBasedOnCommand());
    }

    private void updateUIBasedOnCommand() {
        String selectedCommand = (String) commandBox.getSelectedItem();
        roomIdField.setEnabled(false);
        roomNameField.setEnabled(false);
        privateRoomCheckbox.setEnabled(false);
        messageField.setEnabled(true);
        sendFileButton.setEnabled(true);

        switch (selectedCommand) {
            case "/create":
                roomNameField.setEnabled(true);
                privateRoomCheckbox.setEnabled(true);
                break;
            case "/join":
                roomIdField.setEnabled(true);
                break;
            case "/leave":
                // No additional fields needed
                break;
            case "/file":
                messageField.setEnabled(false);
                break;
        }
    }

    private void connect() {
        String username = usernameField.getText().trim();
        if (username.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter a username", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            // Send username as first message
            out.println("USER:"+username);

            // Start message receiver thread
            executorService.submit(this::receiveMessages);

            // Update UI state
            connected = true;
            connectButton.setText("Disconnect");
            usernameField.setEnabled(false);
            sendButton.setEnabled(true);
            messageField.setEnabled(true);
            sendFileButton.setEnabled(true);
            commandBox.setEnabled(true);
            updateUIBasedOnCommand();

            log("Connected to server as " + username);
        } catch (IOException e) {
            log("Connection error: " + e.getMessage());
            JOptionPane.showMessageDialog(this, "Failed to connect to server: " + e.getMessage(), "Connection Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void disconnect() {
        if (connected) {
            try {
                if (socket != null && !socket.isClosed()) {
                    socket.close();
                }

                connected = false;
                connectButton.setText("Connect");
                usernameField.setEnabled(true);
                sendButton.setEnabled(false);
                messageField.setEnabled(false);
                commandBox.setEnabled(false);
                roomIdField.setEnabled(false);
                roomNameField.setEnabled(false);
                privateRoomCheckbox.setEnabled(false);
                sendFileButton.setEnabled(false);

                log("Disconnected from server");
            } catch (IOException e) {
                log("Error during disconnect: " + e.getMessage());
            }
        }
    }

    private void selectAndSendFile() {
        int returnVal = fileChooser.showOpenDialog(this);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            selectedFile = fileChooser.getSelectedFile();
            if (selectedFile.length() > MAX_FILE_SIZE) {
                JOptionPane.showMessageDialog(this,
                        "File is too large. Maximum size is 5MB.",
                        "File Too Large", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // If file command is selected, send immediately
            if ("/file".equals(commandBox.getSelectedItem())) {
                sendFile();
            } else {
                log("File selected: " + selectedFile.getName() +
                        " (" + (selectedFile.length() / 1024) + " KB)");
                log("Use /file command to send the file");
                commandBox.setSelectedItem("/file");
            }
        }
    }

    private void sendFile() {
        if (!connected || out == null || selectedFile == null) return;

        try {
            // Read file as bytes
            byte[] fileBytes = Files.readAllBytes(selectedFile.toPath());

            // Encode file as Base64 string
            String encodedFile = Base64.getEncoder().encodeToString(fileBytes);

            // Send file metadata first
            out.println("/file " + selectedFile.getName() + " " + fileBytes.length);

            // Send file content in chunks to avoid buffer limitations
            int chunkSize = 1024; // 1KB chunks
            int chunks = (int) Math.ceil(encodedFile.length() / (double) chunkSize);

            for (int i = 0; i < chunks; i++) {
                int start = i * chunkSize;
                int end = Math.min(start + chunkSize, encodedFile.length());
                String chunk = encodedFile.substring(start, end);
                out.println("/chunk " + i + " " + chunks + " " + chunk);
            }

            out.println("/endfile");
            log("File sent: " + selectedFile.getName());
            selectedFile = null;

        } catch (IOException e) {
            log("Error sending file: " + e.getMessage());
            JOptionPane.showMessageDialog(this,
                    "Failed to send file: " + e.getMessage(),
                    "File Send Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void sendMessage() {
        if (!connected || out == null) return;

        String message = messageField.getText().trim();

        String command = (String) commandBox.getSelectedItem();

        if ("Regular Message".equals(command)) {
            if (message.isEmpty()) return;
            out.println(message);
        } else if ("/create".equals(command)) {
            String roomName = roomNameField.getText().trim();
            if (roomName.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Please enter a room name", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            String roomType = privateRoomCheckbox.isSelected() ? "private" : "public";
            out.println("/create " + roomType + " " + roomName);
        } else if ("/join".equals(command)) {
            String roomId = roomIdField.getText().trim();
            if (roomId.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Please enter a room ID", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            out.println("/join " + roomId);
        } else if ("/leave".equals(command)) {
            out.println("/leave");
        }

        messageField.setText("");
    }

    private void receiveMessages() {
        try {
            String message;
            while (connected && (message = in.readLine()) != null) {
                if (message.startsWith("/file ")) {
                    // New file transfer starting
                    String[] parts = message.split(" ", 3);
                    incomingFileName = parts[1];
                    incomingFileSize = Integer.parseInt(parts[2]);
                    fileBuffer = new StringBuilder();
                    receivedChunks = 0;
                    expectedChunks = 0;
                    final String notification = "Receiving file: " + incomingFileName +
                            " (" + (incomingFileSize / 1024) + " KB)";
                    SwingUtilities.invokeLater(() -> log(notification));
                } else if (message.startsWith("/chunk ")) {
                    // File chunk received
                    String[] parts = message.split(" ", 4);
                    int chunkIndex = Integer.parseInt(parts[1]);
                    expectedChunks = Integer.parseInt(parts[2]);
                    String chunkData = parts[3];
                    fileBuffer.append(chunkData);
                    receivedChunks++;

                    final int progress = (int)((receivedChunks / (double)expectedChunks) * 100);
                    if (progress % 20 == 0) {
                        SwingUtilities.invokeLater(() -> log("File download progress: " + progress + "%"));
                    }
                } else if (message.equals("/endfile")) {
                    // File transfer complete
                    if (receivedChunks == expectedChunks) {
                        saveReceivedFile();
                    } else {
                        final String error = "File transfer incomplete. Expected " +
                                expectedChunks + " chunks but got " + receivedChunks;
                        SwingUtilities.invokeLater(() -> log(error));
                    }
                } else {
                    // Regular message
                    final String receivedMessage = message;
                    SwingUtilities.invokeLater(() -> log(receivedMessage));
                }
            }
        } catch (IOException e) {
            if (connected) {
                SwingUtilities.invokeLater(() -> {
                    log("Connection lost: " + e.getMessage());
                    disconnect();
                });
            }
        }
    }

    private void saveReceivedFile() {
        try {
            // Let user choose save location
            JFileChooser saveChooser = new JFileChooser();
            saveChooser.setSelectedFile(new File(incomingFileName));
            int returnVal = saveChooser.showSaveDialog(this);

            if (returnVal == JFileChooser.APPROVE_OPTION) {
                File saveFile = saveChooser.getSelectedFile();

                // Decode Base64 to binary
                byte[] fileData = Base64.getDecoder().decode(fileBuffer.toString());

                // Write to file
                Files.write(saveFile.toPath(), fileData);

                SwingUtilities.invokeLater(() -> log("File saved as: " + saveFile.getAbsolutePath()));
            } else {
                SwingUtilities.invokeLater(() -> log("File save cancelled"));
            }
        } catch (Exception e) {
            SwingUtilities.invokeLater(() -> log("Error saving file: " + e.getMessage()));
        }

        // Clear file buffer
        fileBuffer = new StringBuilder();
        incomingFileName = "";
        incomingFileSize = 0;
    }

    private void log(String message) {
        chatArea.append(message + "\n");
        // Auto-scroll to bottom
        chatArea.setCaretPosition(chatArea.getDocument().getLength());
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new ChatClient().setVisible(true);
        });
    }
}