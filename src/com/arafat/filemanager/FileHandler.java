package com.arafat.filemanager;

import com.arafat.server.Server;

import java.io.*;
import java.net.Socket;
import java.net.SocketTimeoutException;

public class FileHandler {

    public static void createDirectory(String path) {
        File file = new File(path);
        if (!file.exists()) {
            if (file.mkdir()) {
                System.out.println("Directory is created at "+path);
            } else {
                System.out.println("Failed to create directory!");
            }
        }
        else System.out.println("\nDirectory already exists!");
    }

    public static void deleteFile(String filePath) {
        File file = new File(filePath);
        if (file.exists()) {
            if (file.delete()) {
                System.out.println("File deleted..");
            }
        }
    }

    public static void sendFileFromServer(String fileName, String filePath, int filesize, int chunkSize, DataOutputStream dos) throws IOException {
        File file = new File(filePath);
        FileInputStream fis = new FileInputStream(file);
        BufferedInputStream bis = new BufferedInputStream(fis);
        int bytesRead;
        int currentTotal = 0;
        byte[] buffer = new byte[chunkSize];
        try {


            while ((bytesRead = bis.read(buffer, 0, buffer.length)) > 0) {
                currentTotal += bytesRead;
                System.out.println("Sent: " + roundTo((currentTotal *1.0/1000000.0)) + " MB");
                dos.write(buffer, 0, bytesRead);
                dos.flush();

                if (checkSum(filesize, currentTotal)) {
                    System.out.println("File " + fileName + " sent successfully");
                    break;
                }
            }
        }
        catch (Exception e ){
            System.out.println("File transfer failed");
            handleInterruptedException(filePath);
            removeFromFileRecords(fileName);
        }

        fis.close();
        bis.close();
    }



    public static void sendFileFromClient(String fileName, String filePath, int chunkSize, DataOutputStream dos, DataInputStream dis, Socket socket) throws IOException {

        System.out.println("source: "+ filePath);

        File file = new File(filePath);
        FileInputStream fis = new FileInputStream(file);
        BufferedInputStream bis = new BufferedInputStream(fis);
        int bytesRead;
        int currentTotal = 0;
        byte[] buffer = new byte[chunkSize];
        int timeOut = 5000;
        //set timeOut
        socket.setSoTimeout(timeOut);

        try {
            while ((bytesRead = bis.read(buffer, 0, buffer.length)) > 0) {
                currentTotal += bytesRead;

                System.out.println("Sent: " + roundTo(currentTotal/1000000.0) + " MB");
                dos.write(buffer, 0, bytesRead);
                dos.flush();
                //get acknowledgement
                getAcknowledgement(dis);

            }
        }
        catch (SocketTimeoutException e ){
            System.out.println("File transfer failed cause of socket timeOut");
            System.out.println("Error in file Transfer: "+ e.getMessage());
//            socket.close();
            //send a failure message to server
            dos.writeUTF("timeout");

        }
        catch (InterruptedIOException e){
            System.out.println("File transfer failed caused by INTR IO");
            System.out.println("Error in file Transfer: "+ e.getMessage());
//            socket.close();
            //send a failure message to server
//            dos.writeUTF("");

        }
        //wait for more time
        socket.setSoTimeout(100000);

        System.out.println("File " + fileName + " sent successfully");
        fis.close();
        bis.close();
    }

    private static void sendAcknowledgement(DataOutputStream dos){
        try {
            System.out.println("Sending ACK");
            dos.writeUTF("ACK");
            dos.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void getAcknowledgement(DataInputStream dis) throws IOException {
        System.out.println("reading ack...");
        String ack = dis.readUTF();


        if(!ack.equals("ACK")){
            System.out.println("File transfer failed");
        }
        else System.out.println("chunk transfer successful");
    }

    private static boolean getAcknowledgement(DataInputStream dis, long timeOut) throws IOException {
        System.out.println("reading ack...");
        long startTime = System.currentTimeMillis();

        String ack ="";
        if(dis.available()>0)
             ack = dis.readUTF();
        else {
           long endTime = System.currentTimeMillis();
           if(endTime-startTime>timeOut)
           {
               System.out.println("File transfer failed");
               return false;
           }
        }

        if(!ack.equals("ACK")){
            System.out.println("File transfer failed");
            return false;
        }
        else {
            System.out.println("chunk transfer successful");
            return true;
        }
    }

    public static void receiveFile(String fileName, String filePath,int fileSize, int chunkSize, DataInputStream dis) throws IOException {
        FileOutputStream fos = new FileOutputStream(filePath);
        BufferedOutputStream bos = new BufferedOutputStream(fos);
        int bytesRead;
        int currentTotal = 0;
        byte[] buffer = new byte[chunkSize];
        while ((bytesRead = dis.read(buffer, 0, buffer.length)) > 0) {
            currentTotal += bytesRead;
            System.out.println("--------"+ roundTo((currentTotal * 1.0 / fileSize)*100.0) + "% done");
//            System.out.println("Receiving " + currentTotal + " bytes");
            bos.write(buffer, 0, bytesRead);
            bos.flush();

            if (currentTotal == fileSize) {
                System.out.println("File " + fileName + " received successfully");
                break;
            }
        }

//        System.out.println("File " + fileName + " received successfully");
        fos.close();
        bos.close();

//        System.out.println("exiting from downloadMethod..");
    }

    public static void receiveFileInServer(String fileName, String filePath, int fileSize, int chunkSize, DataInputStream dis, DataOutputStream dos) throws IOException {

        System.out.println("destination: "+ filePath);
        FileOutputStream fos = new FileOutputStream(filePath);
        BufferedOutputStream bos = new BufferedOutputStream(fos);
        int bytesRead;
        int currentTotal = 0;
        byte[] buffer = new byte[chunkSize];

        try {


            while ((bytesRead = dis.read(buffer, 0, buffer.length)) > 0) {

                currentTotal += bytesRead;
                System.out.println(roundTo((currentTotal * 1.0 /fileSize)*100.0) + " % Received");
                bos.write(buffer, 0, bytesRead);
                bos.flush();

                //make a delay to test timeout
//                if (currentTotal>5000){
//                    System.out.println("time delay of 33s is applied ");
//                    timeDelay(7000);
//                    getTimeOutMessage(dis, fileName, filePath);
//                    break;
//                }

                //send acknowledgement
                sendAcknowledgement(dos);
                if (checkSum(fileSize, currentTotal)) {
                    System.out.println("File " + fileName + " received successfully");
                    break;
                }

            }
        }
        catch (Exception e ){

            System.out.println("File transfer failed");
            System.out.println("Error in file Transfer: "+ e.getMessage());
            handleInterruptedException(filePath);
            removeFromFileRecords(fileName);
        }
        fos.close();
        bos.close();
    }

    private static void getTimeOutMessage(DataInputStream dis,String fileName, String filePath) throws IOException {

        System.out.println("gettinhg time out Message..");
        String message = dis.readUTF();
        if (message.equalsIgnoreCase("timeout")) {
            removeFromFileRecords(fileName);
            handleInterruptedException(filePath);

        }

    }


    public static void receiveFile(String fileName, String filePath, int fileSize, int chunkSize, DataInputStream dis, DataOutputStream dos, Socket socket) throws IOException {

        System.out.println("destination: "+ filePath);
        FileOutputStream fos = new FileOutputStream(filePath);
        BufferedOutputStream bos = new BufferedOutputStream(fos);
        int bytesRead;
        int currentTotal = 0;
        byte[] buffer = new byte[chunkSize];
        int timeOut = 30000; //30 seconds

//        //set timeOut
//        socket.setSoTimeout(timeOut);

        try {


            while ((bytesRead = dis.read(buffer, 0, buffer.length)) > 0) {

                currentTotal += bytesRead;
//                System.out.println("Receiving " + currentTotal + " bytes");
                System.out.println(roundTo((currentTotal * 1.0 /fileSize)*100.0) + " % Received");

                bos.write(buffer, 0, bytesRead);
                bos.flush();


                //send acknowledgement
                sendAcknowledgement(dos);

//            System.out.println("now currentot: "+ currentTotal);

                if (checkSum(fileSize, currentTotal)) {
                    System.out.println("File " + fileName + " received successfully");
                    break;
                }

            }
        }
        catch (SocketTimeoutException  e ){

            System.out.println("File transfer failed cause of socket timeOut");
            System.out.println("Error in file Transfer: "+ e.getMessage());
//            socket.close();
            handleInterruptedException(filePath);
            removeFromFileRecords(fileName);
        }
        catch (InterruptedIOException e){
            System.out.println("File transfer failed cause of interruption");
            socket.close();
            handleInterruptedException(filePath);
            removeFromFileRecords(fileName);
        }

        //now extend the timeOut to 100 seconds
//        socket.setSoTimeout(timeOut+70000);

//        System.out.println("File " + fileName + " received successfully");
        fos.close();
        bos.close();
    }


    public static int getFileSize(String filePath) {
        System.out.println("Getting file size from "+ filePath);
        File file = new File(filePath);
        System.out.println("exists:"+file.exists());
        return (int) file.length();
    }

    private static boolean checkSum(int fileSize, int current){
        return fileSize == current;
    }


    private static void handleInterruptedException(String filePath) {
        deleteFile(filePath);
    }

    private static void timeDelay(int time) throws InterruptedException {
        Thread.sleep(time);
    }

    private static double roundTo(double number){
        return  (Math.round(number * 10000.0) / 10000.0);
    }

    private static void removeFromFileRecords(String fileName){

        //removing from file records
        Server.sharedFiles.removeIf(file -> file.getFileName().equals(fileName));
        System.out.println(fileName+" has been removed from file records!");

    }
}
