package com.arafat.server;

// Java implementation of Server side
// It contains two classes : Server and ClientHandler
// Save file as Server.java

import com.arafat.filemanager.SharedFile;
import com.arafat.message.Message;

import java.io.*;
import java.util.*;
import java.net.*;

// Server class
public class Server
{

    // Vector to store active clients
    static Vector<ClientHandler> clientHandlers = new Vector<>();
    static Vector<SharedFile> sharedFiles = new Vector<>();

    static HashMap<Integer,Queue<Message>> clientMessageQueue = new HashMap<>();

    static int MAX_BUFFER_SIZE;
    static int MIN_CHUNK_SIZE;
    static int MAX_CHUNK_SIZE;
    // counter for clients
    static int i = 0;



    public static void main(String[] args) throws IOException
    {

        Scanner sc = new Scanner(System.in);
        Socket s;


        System.out.println("Enter the maximum buffer size");
        Server.MAX_BUFFER_SIZE = sc.nextInt();
        System.out.println("Enter the minimum chunk size");
        Server.MIN_CHUNK_SIZE = sc.nextInt();
        System.out.println("Enter the maximum chunk size");
        Server.MAX_CHUNK_SIZE = sc.nextInt();

        // server is listening on port 1234
        ServerSocket ss = new ServerSocket(1234);
        System.out.println("Server started\nListening....(at "+ss.getInetAddress().getHostAddress()+" port "+ss.getLocalPort()+")");


        // running infinite loop for getting
        // client request
        while (true)
        {
            // Accept the incoming request
            s = ss.accept();

            System.out.println("New client request received : " + s);

            // obtain input and output streams
            DataInputStream dis = new DataInputStream(s.getInputStream());
            DataOutputStream dos = new DataOutputStream(s.getOutputStream());

            System.out.println("Creating a new handler for this client...");

            // Create a new handler object for handling this request.
            ClientHandler client = new ClientHandler(s,dis,dos);
            // Create a new Thread
            Thread t = new Thread(client);

            System.out.println("Adding this client to active client list");

            // add this client to active clients list
            clientHandlers.add(client);

            // start the thread.
            t.start();

            // increment i for new client.
            // i is used for naming only
            i++;

        }
    }

}

