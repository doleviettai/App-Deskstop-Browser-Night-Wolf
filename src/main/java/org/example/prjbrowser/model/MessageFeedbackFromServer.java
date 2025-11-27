package org.example.prjbrowser.model;

import javafx.beans.property.*;
import java.sql.Timestamp;

public class MessageFeedbackFromServer {
    private final IntegerProperty id;
    private final StringProperty username;
    private final StringProperty feedback;
    private final StringProperty comment;
    private final ObjectProperty<Timestamp> createdAt;

    // Constructor nhận giá trị thực
    public MessageFeedbackFromServer(int id, String username, String feedback, String comment, Timestamp createdAt) {
        this.id = new SimpleIntegerProperty(id);
        this.username = new SimpleStringProperty(username);
        this.feedback = new SimpleStringProperty(feedback);
        this.comment = new SimpleStringProperty(comment);
        this.createdAt = new SimpleObjectProperty<>(createdAt);
    }

    public int getId() {
        return id.get();
    }

    public IntegerProperty idProperty() {
        return id;
    }

    public String getUsername() {
        return username.get();
    }

    public StringProperty usernameProperty() {
        return username;
    }

    public String getFeedback() {
        return feedback.get();
    }

    public StringProperty feedbackProperty() {
        return feedback;
    }

    public String getComment() {
        return comment.get();
    }

    public StringProperty commentProperty() {
        return comment;
    }

    public Timestamp getCreatedAt() {
        return createdAt.get();
    }

    public ObjectProperty<Timestamp> createdAtProperty() {
        return createdAt;
    }
}
