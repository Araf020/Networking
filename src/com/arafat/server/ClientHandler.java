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

//    final DataInputStream dis;
    DataInputStream dis;
    DataOutputStream dos;
//    final DataOutputStream dos;
    ObjectInputStream objectInputStream;
    ObjectOutputStream objectOutputStream;
    Socket s;
    boolean isloggedin;
    boolean isFileShared;
    int chunkSize;


    //constructor
    public ClientHandler(Socket s,int id, DataInputStream dis, DataOutputStream dos) {
        this.dis = dis;
        this.dos = dos;
        this.s = s;
        this.clientID = id;


        this.isloggedin = true;
        this.isFileShared = false;
        this.chunkSize = 1024; //default


    }
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
                    System.out.println("Server~$ ClientId: "+this.clientID+" is leaving!");
                    this.isloggedin=false;
                    this.s.close();
                    Thread.currentThread().interrupt();
                    break;
                }

                //process the message
                processMessage(received);

            } catch (IOException | InterruptedException e) {
                System.out.println("Server~$ Error in clientHandler Thread: "+e.getMessage());
                this.isloggedin=false;


//                e.printStackTrace();
                break;

            }

        }
        try
        {
            // closing resources
            this.dis.close();
            this.dos.close();

        }catch(IOException e){

            System.out.println("Server~$ error in closing stream: "+ e.getMessage());
//            e.printStackTrace();
        }
    }



    ///========================= initialize the client with id and create directory ======================================

    private void initializeClient(){
        try {
//            this.clientID = this.dis.readInt();
            System.out.println("Client ID is: "+this.clientID);
            FileHandler.createDirectory("clientDirectory/"+this.clientID);
            //initialize the message queue
//            Server.clientMessageQueue.put(this.clientID, new LinkedList<>());
//            objectInputStream = new ObjectInputStream(s.getInputStream());
//            objectOutputStream = new ObjectOutputStream(s.getOutputStream());
        }

        catch (Exception e) {
            System.out.println("Server~$ got error on receiving id: "+ e.getMessage());
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
                System.out.println("Server~$ looking for public files");
                //send the list of public files
                sendPublicFileList();
            }
            else if (lookup.equalsIgnoreCase("MF")) {
                System.out.println("Server~$ looking for owned files");
                //send the list of owned files
                sendOwnersFileList();
            }
            else {

                System.out.println(commandType+ " "+ lookup+"# not implemented yet!");
            }
        }
        else if (commandType.equalsIgnoreCase("R")){
            isFileShared = false;
            String desc = stringTokenizer.nextToken()+" ";
            while (stringTokenizer.hasMoreTokens()) {
                desc += stringTokenizer.nextToken()+" ";
            }



            //format: requestID#description
            desc = "description:"+ desc;
            System.out.println("Server~$ client with id "+this.clientID+" says: "+ desc);

            //send the message to all clients
            broadCastToAll(desc);

        }

        else if (commandType.equalsIgnoreCase("V")){
            isFileShared = false;
            sendMsgFromQueue();
        }

        else if (commandType.equalsIgnoreCase("UR")){

            //Format:: [UR fileName fileSize requestID]
            String fileName = stringTokenizer.nextToken();
            int fileSize = Integer.parseInt(stringTokenizer.nextToken());
            int requestId = Integer.parseInt(stringTokenizer.nextToken());
            int fileId = generateFileID();

            if (checkBuffer(fileSize)) {
                processAndSendChunkSize(fileSize, fileId, fileName);
                Thread.sleep(1000);
                receiveFile(fileSize, fileId);
                notifyClient(requestId, fileId);
            }
            else {
                System.out.println("Server~$ buffer is full");
                this.dos.writeUTF("BUFFER_OVER_FLOW");
            }
        }

        else if (commandType.equalsIgnoreCase("U")){

            isFileShared = true;
            // command format: U fileName fileSize visibility
            String fileName = stringTokenizer.nextToken();
            int fileSize = Integer.parseInt(stringTokenizer.nextToken());
            String visibility = stringTokenizer.nextToken();
            int fileId = generateFileID();

            if (checkBuffer(fileSize)) {


                processAndRespond(fileSize, visibility, fileId, fileName);
                Thread.sleep(2000);
                receiveFile(fileSize, fileId);
                Server.CURRENT_BUFFER_SIZE -= fileSize;
            }

            else {
                System.out.println("Server~$ buffer is full");
                this.dos.writeUTF("BUFFER_OVER_FLOW");
            }

        }
        else if (commandType.equalsIgnoreCase("D")){
            isFileShared = true;
            // command format: D fileID
            int fileID = Integer.parseInt(stringTokenizer.nextToken());
            System.out.println("Server~$client asked for file: "+ fileID);
            //send the file
            sendFileToClient(fileID);

        }

        else {
            System.out.println(commandType+"# Not implemented yet!");
        }

    }

    private boolean checkBuffer(int fileSize) {
        Server.CURRENT_BUFFER_SIZE += fileSize;
        return Server.CURRENT_BUFFER_SIZE <= Server.MAX_BUFFER_SIZE;
    }


    private void notifyClient(int requestId,int fileid) {


        if (Server.clientMessageQueue.get(requestId) == null){
            System.out.println("invalid requestId");
            return;
        }
        Server.clientMessageQueue.get(requestId).add(new Message("someone uploaded the file you requested!\nFileId: "+fileid+"\nto download the file type [D fileId]", requestId));

        //testing
//        showMessageOfClient(requestId);
    }

    private void processAndSendChunkSize(int fileSize, int fileId, String fileName) throws IOException {
        savetoFileRecords(fileSize,"public",fileId, fileName);
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
    private void receiveFile(int filesize, int fileId) throws IOException {

        String fileName = getFileNameByFileId(fileId);
        String filePath = "clientDirectory/"+this.clientID+"/"+fileName;

        FileHandler.receiveFileInServer(fileName,filePath,filesize,this.chunkSize,this.dis, this.dos);

        s.setSoTimeout(100000);
    }


//    private void receiveFile(int filesize, String fileName) throws IOException {
//
////        String fileName = getFileNameFromRecords(this.clientID);
//        String filePath = "clientDirectory/"+this.clientID+"/"+fileName;
//
//        //delay of 10 seconds
////        this.s.setSoTimeout(10000);
//
//        FileHandler.receiveFile(fileName,filePath,filesize,this.chunkSize,this.dis, this.dos,this.s);
//
//        //file receive done so wait longer otherwise it will close the socket
////        s.setSoTimeout(100000);
//    }


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
        FileHandler.sendFileFromServer(fileName,filePath,file_size,Server.MAX_CHUNK_SIZE,this.dos);

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

    private void processAndRespond(int fileSize, String visibility, String fileName) throws IOException {

        //saving to file records
        int fileID = generateFileID();
        savetoFileRecords(fileSize,visibility,fileID, fileName);
        System.out.println("filesize :"+fileSize+" visibility: "+ visibility);
        //send the chunk size
        sendChunkSize();
    }
    private void processAndRespond(int fileSize, String visibility,int fileId, String fileName) throws IOException {

        //saving to file records

        savetoFileRecords(fileSize,visibility,fileId, fileName);
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


    private void savetoFileRecords( int fileSize, String visibility, int fileID, String fileName){


//        String fileName = this.clientID + getDate() + "__" + fileID; //i.e 1705020_2326
        fileName = this.clientID+"_"+fileName+"_"+fileID;
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
        String msg= "\n-------------------------------------------------------\n";

        if (Server.clientMessageQueue.get(this.clientID).isEmpty()) {
            System.out.println("----Queue is empty--\n");
        }
//        showMessageOfClient(this.clientID);

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
                if (this.clientID != clientHandler.clientID) {
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

        String fileList = "\tFileName\t\t\t\t\t\t\townerID\t\tFileID\n";
        fileList+= "----------------------------------------------------------------------------------\n";
        for(SharedFile file:Server.sharedFiles){
            if(file.getVisibility().equalsIgnoreCase("public")){
                fileList += "\t"+file.getFileName()+"\t"+file.getOwnerId()+"\t\t"+ file.getFileID()+"\n";
            }
        }
        return fileList;
    }

    private String getUploadedFileList(){
        String fileList = "\tfileName\t\t\tfileID\tvisibility\n";
        fileList+= "----------------------------------------------------------------------------------\n";

        for(SharedFile file:Server.sharedFiles){
            if(file.getOwnerId() == this.clientID){
                fileList += "\t"+file.getFileName()+"\t\t"+file.getFileID()+"\t\t"+file.getVisibility()+"\n";
            }
        }
        return fileList;
    }



    public int getClientID() {
        return clientID;
    }
    boolean isloggedin() {
        return this.isloggedin;
    }
    public void setLoggInStatus(boolean status){
        this.isloggedin = status;
    }

    public void setSocket(Socket s){
        this.s = s;

    }

    public void setDis(DataInputStream dis) {
        this.dis = dis;
    }

    public void setDos(DataOutputStream dos) {
        this.dos = dos;
    }

    private void showMessageOfClient(int id){
        Queue<Message> queue = Server.clientMessageQueue.get(id);
        for(Message message:queue){
            System.out.println("message FOR ID: "+id+" #\n" +message.getMessage());
        }
    }
}
