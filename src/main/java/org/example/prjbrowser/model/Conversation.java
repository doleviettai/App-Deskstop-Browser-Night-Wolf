package org.example.prjbrowser.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Conversation implements Serializable {
    private static final long serialVersionUID = 1L;

    private int id;
    private int userId;
    private String title;

    // ✅ Danh sách tin nhắn trong conversation
    private List<Messages> messages = new ArrayList<>();

    // --- Constructors ---
    public Conversation() {}

    public Conversation(int id, int userId, String title) {
        this.id = id;
        this.userId = userId;
        this.title = title;
    }

    // --- Getters & Setters ---
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public List<Messages> getMessages() { return messages; }
    public void addMessage(Messages message) { messages.add(message); }
}