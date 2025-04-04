# File-sharing 

📁 File Sharing Application

A multithreaded chat and file-sharing application built in Java, using LAN socket connectivity, MySQL for user authentication, and a Swing-based UI. The application supports room-based chat, file sharing, and local file storage.

⚡ Features

🔹 Room-based chat with username display

🔹 File sharing with local storage support

🔹 LAN socket connectivity for seamless data transfer

🔹 Multi-user authentication using MySQL

🔹 Intuitive Swing-based UI

🚀 Installation & Setup

1️⃣ Clone the Repository

git clone https://github.com/Nishant-Singh-2004/file-sharing.git

cd file-sharing

2️⃣ Set Up the Database

Install MySQL if not already installed.

and run the sql commands file inn it .

3️⃣ Run the Application

Open the project in IntelliJ IDEA (or any Java IDE).

Compile and run the main server and client file.
create multiple clients and use the application.

To use the application First enter the username and connect to the server.
Then first from the dropdown click on create to create a chatroom and in the room name enter the name of room and click send
the server will create a room id for it.

Then on another client, from the dropdown click on /join and then enter room id and click send, now another client has been added to the room 
and then from dropdown click on regular message and start messaging.

To send files from the drop down select /file  or click on send file button and then from send file button chose the file you want to share and click send
and everyone in the room will get the file and download option.

To leave the room from the dropdown select /leave andthen click on send you will leave the chatroom.

To disconnect click on disconnect button next to your username and you will be disconnected from the server.


🛠️ Technologies Used

Java (Swing & Sockets)

MySQL (User authentication)

Multithreading (For handling multiple connections)

Networking (LAN-based communication)

