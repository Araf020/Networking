package com.arafat.client;

// Java implementation for multithreaded chat client
// Save file as Client.java

import com.arafat.filemanager.FileHandler;

import java.io.*;
import java.net.*;
import java.util.Date;
import java.util.Scanner;
import java.util.StringTokenizer;

public class Client
{
    final static int ServerPort = 1234;


    public static void main(String args[]) throws UnknownHostException, IOException
    {
        Scanner scn = new Scanner(System.in);
        String command = "";
        int chunkSize = 1024; // default chunk size
        int clientID;



        // getting localhost ip
        InetAddress ip = InetAddress.getByName("localhost");

        // establish the connection
        Socket s = new Socket(ip, ServerPort);

        // obtaining input and out streams
        DataInputStream dis = new DataInputStream(s.getInputStream());
        DataOutputStream dos = new DataOutputStream(s.getOutputStream());

        //send the id;
        System.out.println("Enter your ID: ");
        clientID = scn.nextInt();
//        sendID(dos, clientID);




        // sendMessage thread
        Thread sendMessage = new Thread(new Runnable()
        {
            @Override
            public void run() {
                //first send the ID
                sendID(dos, clientID);
                String state = "text";
                while (true) {

                    // read the message to deliver.
                    showPrompt();
                    String command = scn.nextLine();
                    System.out.println(command);

//                    try {
//                        state = sendMsg(dos, command, state);
//                    } catch (IOException e) {
//                        e.printStackTrace();
//                    }

                    System.out.println(state);

                }
            }
        });

        // readMessage thread
        Thread readMessage = new Thread(new Runnable()
        {
            @Override
            public void run() {

                while (true) {
                    try {
                        // read the message sent to this client
                        String msg = dis.readUTF();
                        System.out.println(msg);
                    } catch (IOException e) {

                        e.printStackTrace();
                    }
                }
            }
        });




        Thread sendFile = new Thread(new Runnable(){
            @Override
            public void run() {
                while (true) {
                    try {
                        //enter file name and path
                        String fileName = scn.nextLine();
                        String filePath = scn.nextLine();
                        int fileSize = FileHandler.getFileSize(filePath);
                        FileHandler.sendFileFromServer(fileName, filePath,12000, chunkSize, dos);
                    } catch (IOException e) {

                        e.printStackTrace();
                    }
                }
            }
        });

        Thread recieveFile = new Thread(new Runnable(){
            @Override
            public void run() {
                while (true) {
                    try {
                        //recieveFile
//                        System.out.println("recieving file..");
                        String fileName = getDate();
                        String fileLocation = "receive/"+fileName;
                        int filesize  = 1024;
                        int chunksize = 1024;
                        FileHandler.receiveFileInServer(fileName,fileLocation, filesize,chunkSize,dis,dos);
                    } catch (IOException e) {

                        e.printStackTrace();
                    }
                }
            }
        });




        //start the threads

        sendMessage.start();
        recieveFile.start();
//        readMessage.start();

    }



    private  static  void showPrompt(){

        System.out.println("0. view messages(Command: V)");
        System.out.println("1. look up clientList(Command: L C) ");
        System.out.println("2. look up public files(Command: L PF)");
        System.out.println("3. request for a file(command: R enter_the_description)");
        System.out.println("4. upload a file(Command: U filename filePath)");
        System.out.println("5. Look up your file (Command: L MF)");

        System.out.println("Enter command:");
    }


    private static String sendMsg(DataOutputStream dos, String msg, String state) throws IOException {

        StringTokenizer stringTokenizer = new StringTokenizer(msg);
        String commandType = stringTokenizer.nextToken();

        if (commandType.equalsIgnoreCase("V")){
            dos.writeUTF("V");
            state = "text";
        }
        else if (commandType.equalsIgnoreCase("R")){
            dos.writeUTF(stringTokenizer.nextToken());
            state = "text";
        }
        else if (commandType.equalsIgnoreCase("U")){

            //send file
            state = "file";
        }
        else if (commandType.equalsIgnoreCase("D")){
            //download a file
        }

        else if (commandType.equalsIgnoreCase("L")){
            //look up
            if (stringTokenizer.nextToken().equalsIgnoreCase("C")){
                //look up clientList
            }
            else if (stringTokenizer.nextToken().equalsIgnoreCase("PF")){
                //look up public files
            }
            else if (stringTokenizer.nextToken().equalsIgnoreCase("MF")){
                //look up your file
            }
        }

        else
            System.out.println("Invalid command");



        return  state;
    }


    public static String getDate(){
        return new Date().toString();
    }

    public static void sendID(DataOutputStream dos, int clientID){
        try {
            dos.writeInt(clientID);
        } catch (IOException e) {
            System.out.println("Error writing ID.."+ e.getMessage());
//            e.printStackTrace();
        }
    }


}

