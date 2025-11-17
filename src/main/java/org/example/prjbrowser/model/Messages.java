package org.example.prjbrowser.model;

import java.io.Serializable;

public class Messages implements Serializable {
    private static final long serialVersionUID = 1L;

    private int id;
    private int conversationId;
    private String sender;   // "user" hoặc "ai"
    private String content;

    // --- Constructors ---
    public Messages() {}

    public Messages(int id, int conversationId, String sender, String content) {
        this.id = id;
        this.conversationId = conversationId;
        this.sender = sender;
        this.content = content;
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
}
