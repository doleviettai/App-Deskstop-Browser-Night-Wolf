package org.example.prjbrowser.dao;

import org.example.prjbrowser.model.Sessions;

import java.sql.*;
import java.util.UUID;

public class SessionsDAO {
    private final Connection conn;

    public SessionsDAO(Connection conn) {
        this.conn = conn;
    }

    /**
     * Tạo session mới cho user
     */
    public Sessions createSession(int userId) throws SQLException {
        String token = UUID.randomUUID().toString(); // tạo session token ngẫu nhiên
        Timestamp expiresAt = new Timestamp(System.currentTimeMillis() + 7L * 24 * 60 * 60 * 1000); // hết hạn sau 7 ngày

        String sql = "INSERT INTO brower.sessions (user_id, session_token, expires_at) VALUES (?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, userId);
            ps.setString(2, token);
            ps.setTimestamp(3, expiresAt);
            ps.executeUpdate();

            ResultSet rs = ps.getGeneratedKeys();
            if (rs.next()) {
                int id = rs.getInt(1);
                Sessions session = new Sessions(id, userId, token);
                return session;
            }
        }
        throw new SQLException("Không thể tạo session mới");
    }

    /**
     * Kiểm tra token còn hạn hay không
     */
    public boolean validateSession(String token) throws SQLException {
        String sql = "SELECT expires_at FROM brower.sessions WHERE session_token = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, token);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                Timestamp expires = rs.getTimestamp("expires_at");
                return expires.after(new Timestamp(System.currentTimeMillis()));
            }
        }
        return false;
    }

    /**
     * Xóa session khi logout
     */
    public void deleteSession(String token) throws SQLException {
        String sql = "DELETE FROM brower.sessions WHERE session_token = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, token);
            int rows = ps.executeUpdate();
            System.out.println("🗑️ Đã xóa " + rows + " session có token: " + token);
        }
    }

}
