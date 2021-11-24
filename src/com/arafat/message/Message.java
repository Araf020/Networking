package com.arafat.message;

import java.io.Serial;
import java.io.Serializable;

public class Message implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String message;
    private int chunkSize;
    private int  requestId;


    public Message(String message, int requestId) {
        this.message = message;
        this.requestId = requestId;
    }
    public Message(int chunkSize, int requestId) {
        this.chunkSize = chunkSize;
        this.requestId = requestId;
    }

    public int getChunkSize() {
        return chunkSize;
    }

    public void setChunkSize(int chunkSize) {
        this.chunkSize = chunkSize;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public int getRequestId() {
        return requestId;
    }

    public void setRequestId(int requestId) {
        this.requestId = requestId;
    }




}
