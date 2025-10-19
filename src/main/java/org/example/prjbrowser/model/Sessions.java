package org.example.prjbrowser.model;

public class Sessions {
    private int id;
    private int userId;
    private String sessionToken;

    // 🔹 Constructor trống (bắt buộc khi dùng trong DAO)
    public Sessions() {}

    // 🔹 Constructor đầy đủ
    public Sessions(int id, int userId, String sessionToken) {
        this.id = id;
        this.userId = userId;
        this.sessionToken = sessionToken;
    }

    // ✅ Getter & Setter
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getSessionToken() {
        return sessionToken;
    }

    public void setSessionToken(String sessionToken) {
        this.sessionToken = sessionToken;
    }

    @Override
    public String toString() {
        return "Session{" +
                "id=" + id +
                ", userId=" + userId +
                ", sessionToken='" + sessionToken +
                '}';
    }
}
