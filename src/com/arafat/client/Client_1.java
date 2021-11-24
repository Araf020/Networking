package com.arafat.client;

/**
 *  References:
 *  http://docs.oracle.com/javase/7/docs/technotes/guides/security/crypto/CryptoSpec.html
 *  http://www.javamex.com/tutorials/cryptography/rsa_encryption.shtml
 *
 */

//Client class
import com.arafat.filemanager.FileHandler;
import com.arafat.message.Message;


import java.io.*;
import java.net.Socket;
import java.security.*;
import java.util.Date;
import java.util.Random;
import java.util.Scanner;
import java.util.StringTokenizer;


public class Client_1 {

    private DataOutputStream sOutput;
    private DataInputStream sInput;
    private ObjectInputStream objectInputStream;
    private ObjectOutputStream objectOutputStream;

    private String name;
    private  int clientID;
    private Socket socket;
    private String server;
    private int port;
    private boolean isUpload;
    private boolean isDownload;
    private boolean loginStatus;
    private  int chunkSize;
    private  int totalfileSize;
    private Thread sThread;
    private Thread lThread;



    // ===== THE CONSTRUCTOR ==========

    public Client_1 (String server, int port){
        this.server = server;
        this.port = port;
        this.isUpload = false;
        this.loginStatus = false;
        this.chunkSize = 512;

    }

    public void setName(String name){
        this.name = name;
    }
    public String getName(){
        return this.name;
    }

    public boolean isLoginStatus() {
        return this.loginStatus;
    }

    public void setLoginStatus(boolean loginStatus) {
        this.loginStatus = loginStatus;
    }
    // ===== THE MAIN METHOD ==========

    public static void main(String[] args) throws IOException, NoSuchAlgorithmException {

        String serverAddress;

        int portNumber = 1234;
        if(args.length < 1){
            System.out.println("=============================================================");
            System.out.println("# 															 ");
            System.out.println("# Usage: $ java Client [sever ip]							 ");
            System.out.println("# 															 ");
            System.out.println("# e.g. $ java Client 192.168.1.1																 ");
            System.out.println("# 							 								 ");
            System.out.println("# NO ARGUMENT REQUIRED IF SERVER RUNNING ON LOCALHOST		 ");
            System.out.println("# 															 ");
            System.out.println("=============================================================");

            serverAddress = "localhost";
        }
        else{
            serverAddress = args[0];
        }
        Client_1 client = new Client_1(serverAddress, portNumber);

        System.out.println("Enter ID:");
        Scanner sc = new Scanner(System.in);
        client.setClientID(sc.nextInt());
        client.showPrompt();
        client.start();



    }


    /*
     * the start method: establishes a socket connection with the server.
     *
     *
     */


    void start() throws IOException{
        socket = new Socket(server, port);
        System.out.println("connection accepted " + socket.getInetAddress() + " :"  + socket.getPort());

        //for now. this should be reimplemented...
        this.loginStatus = true;
        InputStream inputStream = socket.getInputStream();
        OutputStream outputStream = socket.getOutputStream();
        sInput = new DataInputStream(inputStream);
        sOutput = new DataOutputStream(outputStream);

//        objectInputStream = new ObjectInputStream(inputStream);
//        objectOutputStream = new ObjectOutputStream(outputStream);


        sThread = new sendToServer();
        lThread = new listenFromServer();

        sThread.start();
        lThread.start();
    }

    public int getClientID() {
        return this.clientID;
    }

    public void setClientID(int clientID) {
        this.clientID = clientID;
    }


    public  void sendID(DataOutputStream dos, int clientID){
        try {
            dos.writeInt(clientID);
        } catch (IOException e) {
            System.out.println("Error writing ID.."+ e.getMessage());
//            e.printStackTrace();
        }
    }

    /*
     *
     *  listenFromServer method Will receive the Message from server and call the decryption method.
     */

    class listenFromServer extends Thread {

        public void run(){
            while(true){
                try
                {
//                    System.out.println("isUpload: "+ isUpload);

                    if (isDownload){
                        System.out.println(".");

                        downloadFile();
                        isDownload = false;

                        //dummy
                        //sInput.readUTF();

                    }
                    else if (isUpload){
                        System.out.print("");
                    }

                    else {
//                        System.out.println("reading from dis...");
                            System.out.println(">>");
                            String message = sInput.readUTF();
                            //show the message or get the chunkSize
                            showMessageOrSetChunk(message);
                    }
                }
                 catch (Exception e){

                    System.out.println("reading from server: " + e.getMessage());
                    System.out.println("connection closed");
                    break;

                }
            }
        }
    }


    private void downloadFile() throws IOException {

        String fileName = getDate();
        String filePath = "receive/"+getClientID()+"/" + fileName;
        FileHandler.receiveFile(fileName,filePath,this.totalfileSize,this.chunkSize,sInput);
//        sInput.reset();

    }



    /*
     * sendToServer Class. Extends the thread class. Runs continuously.
     */


    class sendToServer extends Thread {

        public void run(){
            System.out.println("sending id..");
            sendID(sOutput, getClientID());
            FileHandler.createDirectory("receive/"+getClientID());
            Scanner sc = new Scanner(System.in);
            while(true){
                try{

                    //read the message from the user

                    String input = sc.nextLine();
                    System.out.println(input);

                    if (input.equalsIgnoreCase("help"))
                        showPrompt();
                    else  sendMsg(input);   //now send your query/file to the server



                } catch (Exception e){
                    e.printStackTrace();
                    System.out.println("No Message sent to server");
                    //exit program

                    System.exit(0);

                }
            }
        }
    }




    private void showPrompt(){

        System.out.println();
        System.out.println("\t\t=========================================================================");
        System.out.println("\t\t==============            Command GuideLine               ===============");
        System.out.println("\t\t=========================================================================");

        System.out.println("\t\tView messages(Command: V)");
        System.out.println("\t\tLook up clientList(Command: L C) ");
        System.out.println("\t\tLook up public files(Command: L PF)");
        System.out.println("\t\tLook up your file (Command: L MF)");
        System.out.println("\t\tRequest for a file(command: R enter_the_description)");
        System.out.println("\t\tUpload a file(Command: U filename filePath visibility)");
        System.out.println("\t\tUpload against a request(Command: UR filename filePath requestID)");
        System.out.println("\t\tDownload a file(Command: D fileID)");
        System.out.println("\t\tLOGOUT (command: logout)");
        System.out.println("\t\tHELP (command: help)");

        System.out.println("\t\t====================            <><><><><><><><><><>            ===============");
        System.out.println();
    }


    private void showMessageOrSetChunk(String s){
        StringTokenizer stringTokenizer = new StringTokenizer(s, " ");
        String firstToken = stringTokenizer.nextToken();
        if (firstToken.equalsIgnoreCase("chunk")){
            System.out.println("chunk received");
            int cSize = Integer.parseInt(stringTokenizer.nextToken());
            chunkSize = cSize;
            System.out.println("chunk size: "+ cSize);

            if (stringTokenizer.hasMoreTokens()){
                stringTokenizer.nextToken();
                this.totalfileSize = Integer.parseInt(stringTokenizer.nextToken());
                System.out.println("file size to be downloaded--: "+ totalfileSize);
            }
        }
        else if (firstToken.equalsIgnoreCase("ack")){
            System.out.print("");
        }
        else {
            System.out.println("received message: \n" + s);
            System.out.println();
        }
    }



    private void uploadFile(String fileName, String filePath, int chunkS) throws IOException {

        System.out.println("file uploading...");
        FileHandler.sendFile(fileName, filePath,chunkS,sOutput,sInput);
        System.out.println("<><><><>file upload completed<><><><>");
        this.isUpload = false;


    }

//=====================================================================================================
//===================================send method==========================================================
    //=================================================================================================
    private void sendMsg(String msg) throws IOException, InterruptedException {

        StringTokenizer stringTokenizer = new StringTokenizer(msg, " ");
        String commandType = "";
        if (stringTokenizer.hasMoreTokens())
            commandType = stringTokenizer.nextToken();


        if (commandType.equalsIgnoreCase("V")){
            sOutput.writeUTF("V");
            this.isUpload = false;
        }
        else if (commandType.equalsIgnoreCase("R")){
            sOutput.writeUTF(msg);
            this.isUpload = false;
        }
        else if (commandType.equalsIgnoreCase("UR")){
            this.isUpload = true;

            String fileName = stringTokenizer.nextToken();
            String filePath = stringTokenizer.nextToken();
            String requestId = stringTokenizer.nextToken();

            int fileSize = FileHandler.getFileSize(filePath+"/"+fileName);

            filePath = filePath + "/" + fileName;
            System.out.println("file size: " + fileSize);

            String sendTo = "UR " +fileSize + " "+requestId;
            sOutput.writeUTF(sendTo);
            System.out.println("waiting for server response..");

            Thread.sleep(2000);
            System.out.println("thread awaken");
//            System.out.println("now chunk:"+chunkSize);
            uploadFile(fileName,filePath,chunkSize);
            this.isUpload = false;
        }

        else if (commandType.equalsIgnoreCase("U")){

            this.isUpload = true;

            String fileName = stringTokenizer.nextToken();
            String filePath = stringTokenizer.nextToken();
            String visibility = stringTokenizer.nextToken();

            int fileSize = FileHandler.getFileSize(filePath+"/"+fileName);

            filePath = filePath + "/" + fileName;
            System.out.println("file size: " + fileSize);

            String sendTo = "U " +fileSize + " "+visibility;
            sOutput.writeUTF(sendTo);
            System.out.println("waiting for server response..");

            Thread.sleep(2000);
            System.out.println("thread awaken");
//            System.out.println("now chunk:"+chunkSize);
            uploadFile(fileName,filePath,chunkSize);
            this.isUpload = false;

        }

        else if (commandType.equalsIgnoreCase("D")){

            //send the download request to server
            sOutput.writeUTF(msg);
            this.isDownload = true;
            Thread.sleep(2000);
        }


        else if (commandType.equalsIgnoreCase("L")){
            this.isUpload = false;
            String token = stringTokenizer.nextToken();
            //look up
            if (token.equalsIgnoreCase("C")){
                System.out.println("sending your command to server");
                this.sOutput.writeUTF(msg);
                System.out.println("command sent!");
            }

            else if (token.equalsIgnoreCase("PF")){
                //look up public files
//                System.out.println("not implemented yet!");
                this.sOutput.writeUTF(msg);
            }
            else if (token.equalsIgnoreCase("MF")){
                //look up your file
                this.sOutput.writeUTF(msg);
            }
        }
        else if (commandType.equalsIgnoreCase("logout")){
            this.isUpload = false;
            this.sOutput.writeUTF("logout");
        }

        else
            System.out.println("Invalid command");


    }

////////////============================================////////////////////////
    public void closeSocket() {
        try{
            if(sInput !=null) sInput.close();
            if(sOutput !=null) sOutput.close();
            if(socket !=null) socket.close();
        }catch (IOException ioe){
            System.out.println("Error in Disconnect methd");
        }
    }


    /*
    *
    * */

    private  void  send(String msg){
        StringTokenizer stringTokenizer = new StringTokenizer(msg,":");
        String msgContent = stringTokenizer.nextToken();
        String msgType = stringTokenizer.nextToken();
        String msgRecvr = stringTokenizer.nextToken();

        if(msgType.equalsIgnoreCase("sms")){
            try {
//                String fmsg = msgContent + ":" +msgRecvr;
//                this.sOutput.writeObject(new DataPack(encryptMessage(msgContent),encryptAESKey(),msgType,msgRecvr));

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        else {
            System.out.println("sending file..");
//            byte[] toSend = FileManager.getFileBytes(msgContent);
            try {

//                this.sOutput.writeObject(new DataPack(encryptMessage(toSend), encryptAESKey(), msgType, msgRecvr));
            }
            catch (Exception e){
                System.out.println("can't send the message: "+e.getMessage());

//                this.sOutput.writeObject(new DataPack(encryptMessage(toSend), encryptAESKey(), msgType, msgRecvr));
            }
        }


    }

    public int getRandomInteger(){
        Random rand = new Random();
        return rand.nextInt(100);
    }

    public String getDate(){
        return new Date().toString();
    }




}




