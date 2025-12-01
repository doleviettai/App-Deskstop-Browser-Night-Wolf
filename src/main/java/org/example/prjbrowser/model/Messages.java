package org.example.prjbrowser.model;

import java.io.Serializable;

public class Messages implements Serializable {
    private static final long serialVersionUID = 1L;

    private int id;
    private int conversationId;
    private String sender; // "user" hoặc "ai"
    private String content;

    // --- File fields ---
    private String fileName;
    private byte[] filePath; // LONGBLOB -> byte[]
    private String fileType;
    private int fileSize;

    // --- Constructors ---
    public Messages() {}

    public Messages(int id, int conversationId, String sender, String content) {
        this.id = id;
        this.conversationId = conversationId;
        this.sender = sender;
        this.content = content;
    }

    public Messages(int id, int conversationId, String sender, String content,
                    String fileName, byte[] filePath, String fileType, int fileSize) {
        this.id = id;
        this.conversationId = conversationId;
        this.sender = sender;
        this.content = content;
        this.fileName = fileName;
        this.filePath = filePath;
        this.fileType = fileType;
        this.fileSize = fileSize;
    }

    // --- Getters & Setters ---
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getConversationId() { return conversationId; }
    public void setConversationId(int conversationId) { this.conversationId = conversationId; }

    public String getSender() { return sender; }
    public void setSender(String sender) { this.sender = sender; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public byte[] getFilePath() { return filePath; }
    public void setFilePath(byte[] filePath) { this.filePath = filePath; }

    public String getFileType() { return fileType; }
    public void setFileType(String fileType) { this.fileType = fileType; }

    public int getFileSize() { return fileSize; }
    public void setFileSize(int fileSize) { this.fileSize = fileSize; }
}

