package org.example.prjbrowser.model;

import java.io.File;
import java.io.IOException;
import java.util.Properties;
import java.io.FileOutputStream;

public class AutoLoginService {
    private static AutoLoginService instance;
    private String sessionToken;
    private int userId;
    private String username;
    private String fullname;

    private static final String SESSION_FILE = "session.properties"; // đồng bộ với LoginController

    private AutoLoginService() {}

    public static AutoLoginService getInstance() {
        if (instance == null) {
            instance = new AutoLoginService();
        }
        return instance;
    }

    public void saveSession(int userId, String username, String fullname, String token) {
        this.userId = userId;
        this.username = username;
        this.fullname = fullname;
        this.sessionToken = token;
    }

    public String getSessionToken() { return sessionToken; }
    public int getUserId() { return userId; }
    public String getUsername() { return username; }
    public String getFullname() { return fullname; }

    public boolean hasSession() {
        return sessionToken != null && !sessionToken.isEmpty();
    }

    /** 🧹 Xóa session khỏi bộ nhớ + file session.properties */
    public void clearSession() {
        sessionToken = null;
        userId = 0;
        username = null;
        fullname = null;

        File file = new File(SESSION_FILE);
        if (file.exists()) {
            if (file.delete()) {
                System.out.println("🧹 Session đã được xóa khỏi file session.properties");
            } else {
                System.out.println("⚠️ Không thể xóa file session.properties");
            }
        }

        System.out.println("🧹 Session đã được xóa khỏi bộ nhớ");
    }

    /** 🔄 Ghi đè file session.properties rỗng (nếu cần reset mà không xóa file) */
    public void resetSessionFile() {
        try (FileOutputStream fos = new FileOutputStream(SESSION_FILE)) {
            Properties props = new Properties();
            props.setProperty("id", "");
            props.setProperty("username", "");
            props.setProperty("fullname", "");
            props.setProperty("session_token", "");
            props.store(fos, "Session cleared");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
