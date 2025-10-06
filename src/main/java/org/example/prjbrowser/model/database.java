package org.example.prjbrowser.model;

import java.sql.Connection;
import java.sql.DriverManager;

public class database {
    public static Connection connectDb() throws Exception {
        String url = "jdbc:mysql://localhost:3306/brower";
        String user = "root";
        String pass = "taido231105";
        return DriverManager.getConnection(url, user, pass);
    }

    public static void main(String[] args) {
        try (Connection conn = connectDb()) {
            System.out.println("✅ Kết nối thành công!");
            System.out.println("Database Product: " + conn.getMetaData().getDatabaseProductName());
            System.out.println("Database Version: " + conn.getMetaData().getDatabaseProductVersion());
            System.out.println("User: " + conn.getMetaData().getUserName());
            System.out.println("URL: " + conn.getMetaData().getURL());
        } catch (Exception e) {
            System.out.println("❌ Kết nối thất bại!");
            e.printStackTrace();
        }
    }
}
