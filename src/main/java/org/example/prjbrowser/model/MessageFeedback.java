package org.example.prjbrowser.model;

import java.io.Serializable;

public class MessageFeedback implements Serializable {
    private static final long serialVersionUID = 1L;

    private int id;
    private int messageId;
    private int userId;
    private String feedback; // "like" hoặc "dislike"
    private String comment;

    // --- Constructors ---
    public MessageFeedback() {}

    public MessageFeedback(int id, int messageId, int userId, String feedback, String comment) {
        this.id = id;
        this.messageId = messageId;
        this.userId = userId;
        this.feedback = feedback;
        this.comment = comment;
    }

    // --- Getters & Setters ---
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getMessageId() { return messageId; }
    public void setMessageId(int messageId) { this.messageId = messageId; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public String getFeedback() { return feedback; }
    public void setFeedback(String feedback) { this.feedback = feedback; }

    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
}
