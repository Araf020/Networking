package com.arafat.filemanager;

import java.io.*;

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

    public static void sendFile(String fileName, String filePath,int filesize, int chunkSize, DataOutputStream dos) throws IOException {
        File file = new File(filePath);
        FileInputStream fis = new FileInputStream(file);
        BufferedInputStream bis = new BufferedInputStream(fis);
        int bytesRead;
        int currentTotal = 0;
        byte[] buffer = new byte[chunkSize];
        while ((bytesRead = bis.read(buffer, 0, buffer.length)) > 0) {
            currentTotal += bytesRead;
            System.out.println("Sending " + currentTotal + " bytes");
            dos.write(buffer, 0, bytesRead);
            dos.flush();

            if (checkSum(filesize, currentTotal)) {
                System.out.println("File " + fileName + " sent successfully");
                break;
            }
        }
        System.out.println("File " + fileName + " sent successfully...");
        fis.close();
        bis.close();
    }

    //overloaded method
    public static void sendFile(String fileName, String filePath, int chunkSize, DataOutputStream dos, DataInputStream dis) throws IOException {

        System.out.println("source: "+ filePath);

        File file = new File(filePath);
        FileInputStream fis = new FileInputStream(file);
        BufferedInputStream bis = new BufferedInputStream(fis);
        int bytesRead;
        int currentTotal = 0;
        byte[] buffer = new byte[chunkSize];

        while ((bytesRead = bis.read(buffer, 0, buffer.length)) > 0) {
            currentTotal += bytesRead;
            System.out.println("Sending " + currentTotal + " bytes");
            dos.write(buffer, 0, bytesRead);
            dos.flush();
            //get acknowledgement
            getAcknowledgement(dis);
//            getAcknowledgement(dis,30000);
        }
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
            System.out.println("--------"+(currentTotal/fileSize)/100 + "% done");
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

    public static void receiveFile(String fileName, String filePath, int fileSize,int chunkSize, DataInputStream dis, DataOutputStream dos) throws IOException {

        System.out.println("destination: "+ filePath);
        FileOutputStream fos = new FileOutputStream(filePath);
        BufferedOutputStream bos = new BufferedOutputStream(fos);
        int bytesRead;
        int currentTotal = 0;
        byte[] buffer = new byte[chunkSize];
        while ((bytesRead = dis.read(buffer, 0, buffer.length)) > 0) {

            currentTotal += bytesRead;
            System.out.println("Receiving " + currentTotal + " bytes");
            bos.write(buffer, 0, bytesRead);
            bos.flush();
            //send acknowledgement
            sendAcknowledgement(dos);

//            System.out.println("now currentot: "+ currentTotal);

            if (checkSum(fileSize,currentTotal)) {
                System.out.println("File " + fileName + " received successfully");
                break;
            }

        }

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
}
