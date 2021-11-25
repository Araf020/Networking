package com.arafat.server;


import com.arafat.filemanager.SharedFile;
import com.arafat.message.Message;

import java.io.*;
import java.util.*;
import java.net.*;

// Server class


public class Server
{

    // Vector to store active clients
    public static Vector<ClientHandler> clientHandlers = new Vector<>();
    public static Vector<SharedFile> sharedFiles = new Vector<>();

    public static HashMap<Integer,Queue<Message>> clientMessageQueue = new HashMap<>();

    public static int MAX_BUFFER_SIZE;
    public static int MIN_CHUNK_SIZE;
    public static int MAX_CHUNK_SIZE;
    public static int CURRENT_BUFFER_SIZE;
    // counter for clients
//    static int i = 0;



    public static void main(String[] args) throws IOException
    {

        Scanner sc = new Scanner(System.in);
        Socket s;

        System.out.println("Server hasn't started yet!");
        System.out.println("Enter the maximum buffer size");
        Server.MAX_BUFFER_SIZE = sc.nextInt();
        System.out.println("Enter the minimum chunk size");
        Server.MIN_CHUNK_SIZE = sc.nextInt();
        System.out.println("Enter the maximum chunk size");
        Server.MAX_CHUNK_SIZE = sc.nextInt();
        Server.CURRENT_BUFFER_SIZE = 0;

        // server is listening on port 1234
        ServerSocket ss = new ServerSocket(1234);
        System.out.println("Server started\nListening....(at "+ss.getInetAddress().getHostAddress()+" port "+ss.getLocalPort()+")");


        // running infinite loop for getting
        // client request
        while (true)
        {
            // Accept the incoming request

            s = ss.accept();


            System.out.println("Server~$ New client request received : " + s);

            // obtain input and output streams
            DataInputStream dis = new DataInputStream(s.getInputStream());
            DataOutputStream dos = new DataOutputStream(s.getOutputStream());

            System.out.println("Server~$ Creating a new handler for this client...");
            int client_ID;
            try {
                //read the client ID
                client_ID = dis.readInt();
                System.out.println("Server~$ Server~$ Client ID is " + client_ID);
            }
            catch (IOException e){
                System.out.println("Server~$ IOException while getting client id: "+ e.getMessage());
                s.close();
                continue;
            }
            /*
             * Server sends 0 if client is new
             * Server sends 1 if client is old but inactive
             * Server sends 2 if client is active
             */
//            assert client_ID != -1;
            ClientHandler client;
            if (!isClientExist(client_ID)) {
//               client = new ClientHandler(s, dis, dos);
               client = new ClientHandler(s, client_ID, dis, dos);
               client.setLoggInStatus(true);
               Server.clientMessageQueue.put(client_ID, new LinkedList<>());
                //send greetings to the client
//                dos.writeUTF("Server~$ Hi! Welcome, "+client_ID);

                //signal it as a new client
                try {
                    dos.writeInt(0);
                }
                catch (IOException e){
                    System.out.println("Server~$ IOException while sending 0 to client"+ e.getMessage());
                }

                System.out.println("Server~$ Adding this client to active client list");
                // add this client to active clients list
                clientHandlers.add(client);
                // Creating a new Thread
                Thread t = new Thread(client);
                // start the thread.
                t.start();
            }
            else {
                if (isLoggedIn(client_ID)) {
                    System.out.println("Server~$ Client of this id is already logged in!");
//                    dos.writeUTF("Server~$ this ID already loggedIn!");\

                    //signal it as an active client
                    dos.writeInt(2);

                    dis.close();
                    dos.close();
                    s.close();
                }
                else {
                    client = getClientById(client_ID);
                    client.setSocket(s);
                    client.setDis(dis);
                    client.setDos(dos);
                    client.setLoggInStatus(true);

                    //retrieve the message queue
                    Server.clientMessageQueue.put(client_ID, getMessageQueueById(client_ID));
//                    dos.writeUTF("Server~$ Welcome back, "+client_ID);

                    //signal it as an old inactive client
                    dos.writeInt(1);
                    //creating a new thread

                    Thread t = new Thread(client);
                    // start the thread.
                    t.start();
                }

            }


        }
    }

    public static boolean isClientExist(int clientID){

        return getClientById(clientID) != null;
    }
    public static boolean isLoggedIn(int clientID){

        return getClientById(clientID).isloggedin();
    }

    public static ClientHandler getClientById(int clientID){

        ClientHandler clientHandler = null;
        for(ClientHandler client : Server.clientHandlers){
            if(client.getClientID() == clientID){
                clientHandler = client;
            }
        }
        return clientHandler;
    }

    private  static Queue<Message> getMessageQueueById(int clientID){
        return Server.clientMessageQueue.get(clientID);
    }

}

