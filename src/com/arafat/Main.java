package com.arafat;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

public class Main {

    public static void main(String[] args) {
        // write your code here
    }

    private static void sendFile(String fileName) {
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(fileName));
            writer.write("Hello World");
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
