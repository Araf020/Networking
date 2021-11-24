package com.arafat.server;

import com.arafat.filemanager.FileHandler;
import com.arafat.filemanager.SharedFile;
import com.arafat.message.Message;

import java.io.*;
import java.net.Socket;
import java.util.*;

public class ClientHandler implements Runnable
{
    Scanner scn = new Scanner(System.in);
    private String name;
    private int clientID;

    final DataInputStream dis;
    final DataOutputStream dos;
    ObjectInputStream objectInputStream;
    ObjectOutputStream objectOutputStream;
    Socket s;
    boolean isloggedin;
    boolean isFileShared;
    int chunkSize;


    //constructor
    public ClientHandler(Socket s, DataInputStream dis, DataOutputStream dos) {
        this.dis = dis;
        this.dos = dos;
        this.s = s;


        this.isloggedin = true;
        this.isFileShared = false;
        this.chunkSize = 1024; //default


    }

    // constructor
    public ClientHandler(Socket s, String name,
                         DataInputStream dis, DataOutputStream dos) {
        this.dis = dis;
        this.dos = dos;
        this.name = name;
        this.s = s;
        this.isloggedin=true;
    }

    @Override
    public void run() {

        String received;
        //get the id and create directory
        // initialize the message queue
        // for this client
        initializeClient();

        // now start handling other messages from the client
        while (true)
        {
            try
            {
                // receive the string
                received = dis.readUTF();

                System.out.println(received);

                if(received.equals("logout")){
                    this.isloggedin=false;
                    this.s.close();
                    break;
                }

                //process the message
                processMessage(received);

            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }

        }
        try
        {
            // closing resources
            this.dis.close();
            this.dos.close();

        }catch(IOException e){

            System.out.println("error in closing stream: "+ e.getMessage());
//            e.printStackTrace();
        }
    }



    ///========================= initialize the client with id and create directory ======================================

    private void initializeClient(){
        try {
            this.clientID = this.dis.readInt();
            System.out.println("Client ID is: "+this.clientID);
            FileHandler.createDirectory("clientDirectory/"+this.clientID);
            //initialize the message queue
            Server.clientMessageQueue.put(this.clientID, new LinkedList<>());
//            objectInputStream = new ObjectInputStream(s.getInputStream());
//            objectOutputStream = new ObjectOutputStream(s.getOutputStream());
        }

        catch (IOException e) {
            System.out.println("got error on receiving id: "+ e.getMessage());
            e.printStackTrace();
        }
    }

    //====================================process the message ============================================

    private void processMessage(String received) throws IOException, InterruptedException {
        //tokenize the message
        StringTokenizer stringTokenizer = new StringTokenizer(received, " ");
        String commandType = stringTokenizer.nextToken();

        if(commandType.equalsIgnoreCase("L")){
            isFileShared = false;
            String lookup = stringTokenizer.nextToken();

            if (lookup.equalsIgnoreCase("C")) {
                //send the list of clients

                sendClientList();
            }
            else if (lookup.equalsIgnoreCase("PF")) {
                System.out.println("looking for public files");
                //send the list of public files
                sendPublicFileList();
            }
            else if (lookup.equalsIgnoreCase("MF")) {
                System.out.println("looking for owned files");
                //send the list of owned files
                sendOwnersFileList();
            }
            else {

                System.out.println(commandType+ " "+ lookup+"# not implemented yet!");
            }
        }
        else if (commandType.equalsIgnoreCase("R")){
            isFileShared = false;
            String desc = stringTokenizer.nextToken();

            //format: requestID#description
            desc = "description:"+ desc;
            System.out.println("client with id "+this.clientID+" says: "+ desc);

            //send the message to all clients
            broadCastToAll(desc);

        }

        else if (commandType.equalsIgnoreCase("V")){
            isFileShared = false;
            sendMsgFromQueue();
        }

        else if (commandType.equalsIgnoreCase("UR")){

            //Format:: [UR fileSize requestID]

            int fileSize = Integer.parseInt(stringTokenizer.nextToken());
            int requestId = Integer.parseInt(stringTokenizer.nextToken());
            int fileId = generateFileID();

            processAndSendChunkSize(fileSize, fileId);
            Thread.sleep(1000);
            receiveFile(fileSize);

            notifyClient(requestId,fileId);
        }

        else if (commandType.equalsIgnoreCase("U")){

            isFileShared = true;
            // command format: U fileSize visibility
            int fileSize = Integer.parseInt(stringTokenizer.nextToken());
            String visibility = stringTokenizer.nextToken();
            int fileId = generateFileID();
            processAndRespond(fileSize, visibility,fileId);
            Thread.sleep(2000);
            receiveFile(fileSize);


        }
        else if (commandType.equalsIgnoreCase("D")){
            isFileShared = true;
            // command format: D fileID
            int fileID = Integer.parseInt(stringTokenizer.nextToken());
            System.out.println("client says: "+ fileID);
            //send the file
            sendFileToClient(fileID);

        }

        else {
            System.out.println(commandType+"# Not implemented yet!");
        }

    }


    private void notifyClient(int requestId,int fileid) {

        Queue<Message> queue  = Server.clientMessageQueue.get(requestId);
        if (queue == null){
            System.out.println("invalid requestId");
            return;
        }
        queue.add(new Message("someone uploaded the file you requested!\nFileId: "+fileid+"\nto download the file type [D fileId]", requestId));

    }

    private void processAndSendChunkSize(int fileSize, int fileId) throws IOException {
        savetoFileRecords(fileSize,"public",fileId);
        System.out.println("filesize :"+fileSize+" visibility: "+ "public");
        //send the chunk size
        sendChunkSize();
    }

    private void sendOwnersFileList() throws IOException {
        //send the list of owned files
        String fileList = getUploadedFileList();
        this.dos.writeUTF(fileList);
    }

    private void sendPublicFileList() throws IOException {
        String fileList = getPublicFileList();
        this.dos.writeUTF(fileList);
    }

    //===========================================================================
    //======================= receive file ======================================
    //===========================================================================
    private void receiveFile(int filesize) throws IOException {

        String fileName = getFileNameFromRecords(this.clientID);
        String filePath = "clientDirectory/"+this.clientID+"/"+fileName;

        FileHandler.receiveFile(fileName,filePath,filesize,this.chunkSize,this.dis, this.dos);

    }

    private void receiveFile(int filesize, int fileId) throws IOException {

        String fileName = getFileNameByFileId(fileId);
        String filePath = "clientDirectory/"+this.clientID+"/"+fileName;

        FileHandler.receiveFile(fileName,filePath,filesize,this.chunkSize,this.dis, this.dos);

    }

    //===========================================================================
    //======================= send file ======================================
    //===========================================================================

    private void sendFileToClient(int fileID) throws IOException, InterruptedException {

        String fileName = getFileNameByFileId(fileID);

        String filePath = getFilePathByFileId(fileID);
        int file_size = getFileSizeByFileId(fileID);

        System.out.println("chunk "+ Server.MAX_CHUNK_SIZE);
        this.dos.writeUTF("chunk "+ Server.MAX_CHUNK_SIZE+" total "+file_size);
        this.dos.flush();
        Thread.sleep(2000);
        FileHandler.sendFile(fileName,filePath,file_size,Server.MAX_CHUNK_SIZE,this.dos);

    }

    private String getFilePathByFileId(int fileId) {

        String filePath = "";

        for(SharedFile sharedFile: Server.sharedFiles){
            if(sharedFile.getFileID() == fileId){
                filePath = sharedFile.getFilePath();
            }
        }

        return filePath;
    }

    private String getFileNameByFileId(int fileId) {

        String fileName = "";

        for (SharedFile file : Server.sharedFiles) {
            if (file.getFileID() == fileId) {
                fileName = file.getFileName();
            }

        }
        return fileName;

    }

    private void processAndRespond(int fileSize, String visibility) throws IOException {

        //saving to file records
        int fileID = generateFileID();
        savetoFileRecords(fileSize,visibility,fileID);
        System.out.println("filesize :"+fileSize+" visibility: "+ visibility);
        //send the chunk size
        sendChunkSize();
    }
    private void processAndRespond(int fileSize, String visibility,int fileId) throws IOException {

        //saving to file records

        savetoFileRecords(fileSize,visibility,fileId);
        System.out.println("filesize :"+fileSize+" visibility: "+ visibility);
        //send the chunk size
        sendChunkSize();
    }

    private void sendChunkSize() throws IOException {

        int chunk_Size = generateChunkSize(Server.MIN_CHUNK_SIZE, Server.MAX_CHUNK_SIZE);
        this.chunkSize = chunk_Size;
        //send it to the client
        this.dos.writeUTF("chunk "+chunk_Size);
        System.out.println("chunk size of "+chunk_Size+" sent!");

    }


    private void savetoFileRecords( int fileSize, String visibility, int fileID){


        String fileName = this.clientID + getDate() + "__" + fileID; //i.e 1705020_2326
        String filePath = "clientDirectory/"+this.clientID+"/"+fileName;

        SharedFile sharedFile =
                new SharedFile()
                        .setFileName(fileName)
                        .setFileID(fileID)
                        .setOwnerID(this.clientID)
                        .setFileSize(fileSize)
                        .setFilePath(filePath)
                        .setVisibility(visibility);

        /*
         *save it to the file records...
         *
         */
        Server.sharedFiles.add(sharedFile);
        System.out.println(fileName+"saved to file records..");

        //save the file to the file system
//        FileHandler.receiveFile(fileName, filePath,);
    }

    private void sendMsgFromQueue() throws IOException {

        int msgID = 1;
        String msg= "You have no incoming messages!\n";

        while (!Server.clientMessageQueue.get(this.clientID).isEmpty()){
            try {
               Message m = Server.clientMessageQueue.get(this.clientID).poll();
                assert m != null;
                msg += "message-" + msgID + "#\nrequestId:" + m.getRequestId() + "\n" + m.getMessage() + "\n";
                msgID++;
            }

            catch (Exception e){

                e.printStackTrace();
                System.out.println("error in sending message from queue"+e.getMessage());
            }
        }
        System.out.println("sending the message queue to "+ this.clientID);
        this.dos.writeUTF(msg);

    }
//
//    private void sendMsgFromQueue() throws IOException {
//        String msg = "\n---------------------------------------------------\n";
//        int msgID = 1;
//        Queue<Message> messages = Server.clientMessageQueue.remove(this.clientID);
//
//        if (messages != null) {
//
//            for (Message m : messages) {
//                msg += "message-" + msgID + "#\nrequestId:" + m.getRequestId() + "\n" + m.getMessage() + "\n";
//                msgID++;
//            }
//
//        }
//
//        else {
//            msg = "\nYou have no incoming message!\n";
//        }
//
//        System.out.println("sending the message queue to "+ this.clientID);
//        this.dos.writeUTF(msg);
//    }

    private void broadCastToAll(String msg){
//        Queue<Integer> queue = new LinkedList<>();
//        queue.

        for (ClientHandler clientHandler : Server.clientHandlers) {
            try {
                if (this.clientID != clientHandler.clientID && clientHandler.isloggedin) {
                    Integer id = clientHandler.clientID;
                    Server.clientMessageQueue.get(id).add(new Message(msg, this.clientID));
                }

            }

            catch (Exception e) {
                System.out.println("got error on broadcasting message to client: "+ e.getMessage());
            }
        }
    }
    private void sendClientList() throws IOException {

        String clientList = "\nUserID\tLoginStatus\n";
        clientList+= "------------------------------------------------------\n";
        //iterate over Server.ar and send the client list
        for(ClientHandler client:Server.clientHandlers){
            clientList += client.clientID+"\t"+(client.isloggedin ? "active":"inactive")+"\n";
        }

        System.out.println("client found! sending the list to :"+ clientID);
        this.dos.writeUTF(clientList);
        System.out.println("client list sent!");

    }

    private int generateFileID(){
        Random random = new Random();
        return random.nextInt(this.clientID);
    }

    private String getDate(){
        return new Date().toString();
    }

    private int generateChunkSize(int min, int max){
        Random random = new Random();
        return random.nextInt((max - min) + 1) + min;
    }

    private int getFileSizeByFileId(int fileId){
        int fileSize = 0;
        for(SharedFile sharedFile : Server.sharedFiles){
            if(sharedFile.getFileID() == fileId){
                fileSize = sharedFile.getFileSize();
            }
        }
        return fileSize;
    }

    private String getFileNameFromRecords(int clientId){

        String fileNAme = "not_found";
        for(SharedFile file:Server.sharedFiles){
            if(file.getOwnerId() == clientId){
                fileNAme =  file.getFileName();
            }
        }
        return fileNAme;
    }

    private String getPublicFileList(){

        String fileList = "\tFileName\t\t\t\t\t\t\t\t\t\townerID\t\tFileID\n";
        fileList+= "----------------------------------------------------------------------------------\n";
        for(SharedFile file:Server.sharedFiles){
            if(file.getVisibility().equalsIgnoreCase("public")){
                fileList += "\t"+file.getFileName()+"\t"+file.getOwnerId()+"\t\t"+ file.getFileID()+"\n";
            }
        }
        return fileList;
    }

    private String getUploadedFileList(){
        String fileList = "\t\t\tfileName\t\t\t\t\t\t\t\tfileID\tvisibility\n";
        fileList+= "----------------------------------------------------------------------------------\n";

        for(SharedFile file:Server.sharedFiles){
            if(file.getOwnerId() == this.clientID){
                fileList += "\t"+file.getFileName()+"\t"+file.getFileID()+"\t\t"+file.getVisibility()+"\n";
            }
        }
        return fileList;
    }

}
