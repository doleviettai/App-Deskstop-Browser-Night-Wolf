package org.example.prjbrowser.server;

import org.example.prjbrowser.common.Message;
import org.example.prjbrowser.dao.SessionsDAO;
import org.example.prjbrowser.model.Bookmarks;
import org.example.prjbrowser.model.Jbcrypt;
import org.example.prjbrowser.model.Sessions;
import org.example.prjbrowser.model.database;
import java.io.*;
import java.net.Socket;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ClientHandler implements Runnable {
    private Socket socket;
    private ServerController server;

    public ClientHandler(Socket socket, ServerController server) {
        this.socket = socket;
        this.server = server;
    }

    @Override
    public void run() {
        try {
            ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
            ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());

            Message request = (Message) in.readObject();
            Message response = handleRequest(request);

            out.writeObject(response);
            out.flush();

            in.close();
            out.close();
            socket.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Message handleRequest(Message req) {
        String action = (String) req.get("action");  // lấy action từ request
        Message res = new Message();

        try (Connection conn = database.connectDb()) {
            switch (action) {
                case "login": {
                    PreparedStatement ps = conn.prepareStatement(
                            "SELECT * FROM brower.users WHERE username=? AND con_password=?"
                    );
                    ps.setString(1, (String) req.get("username"));
                    ps.setString(2, Jbcrypt.encodePassword((String) req.get("password")));
                    ResultSet rs = ps.executeQuery();

                    if (rs.next()) {
                        int id = rs.getInt("id");
                        String username = rs.getString("username");
                        String firstname = rs.getString("firstname");
                        String lastname  = rs.getString("lastname");

                        // 🔹 Tạo session mới
                        SessionsDAO sessionsDAO = new SessionsDAO(conn);
                        Sessions session = sessionsDAO.createSession(id);

                        // 🔹 Trả thông tin session cho client
                        res.put("status", "success");
                        res.put("message", "Đăng nhập thành công, chào mừng " + firstname + " " + lastname);
                        res.put("id", id);
                        res.put("username", username);
                        res.put("fullname", firstname + " " + lastname);
                        res.put("session_token", session.getSessionToken()); // trả về session token
                    } else {
                        res.put("status", "fail");
                        res.put("message", "Sai tài khoản hoặc mật khẩu");
                    }
                    break;
                }

                case "validate_session": {
                    String token = (String) req.get("session_token");
                    SessionsDAO sessionsDAO = new SessionsDAO(conn);

                    boolean valid = sessionsDAO.validateSession(token);
                    if (valid) {
                        res.put("status", "success");
                        res.put("message", "Session hợp lệ");
                    } else {
                        res.put("status", "fail");
                        res.put("message", "Session đã hết hạn hoặc không hợp lệ");
                    }
                    break;
                }

                case "logout": {
                    String token = (String) req.get("token");

                    if (token == null || token.isEmpty()) {
                        res.put("status", "fail");
                        res.put("message", "Không có token để đăng xuất");
                        break;
                    }

                    SessionsDAO sessionsDAO = new SessionsDAO(conn);
                    sessionsDAO.deleteSession(token);

                    System.out.println("🚪 User đã đăng xuất, token: " + token);
                    res.put("status", "success");
                    res.put("message", "Đã đăng xuất và xóa session khỏi DB");
                    break;
                }





                case "register": {
                    PreparedStatement ps = conn.prepareStatement(
                            "INSERT INTO brower.users(username,firstname , lastname, password, con_password, phone_number) VALUES (?, ?,?,? ,?, ?)"
                    );
                    String username = (String) req.get("username");
                    String firstname = (String) req.get("firstname");
                    String lastname = (String) req.get("lastname");
                    String password = (String) req.get("password");
                    String con_password = (String) req.get("confirm_password");
                    String phone = (String) req.get("phone_number");

                    // Lưu password đã hash vào cả password và con_password để đồng bộ
                    String hashed = Jbcrypt.encodePassword(con_password);
                    ps.setString(1, username);
                    ps.setString(2, firstname);
                    ps.setString(3, lastname);
                    ps.setString(4, password);
                    ps.setString(5, hashed);
                    ps.setString(6, phone);

                    int row = ps.executeUpdate();

                    if (row > 0) {
                        res.put("status", "success");
                        res.put("message", "Đăng ký tài khoản thành công!");
                    } else {
                        res.put("status", "fail");
                        res.put("message", "Đăng ký thất bại!");
                    }
                    break;
                }

                case "forgot_check": {
                    PreparedStatement ps = conn.prepareStatement(
                            "SELECT * FROM brower.users WHERE username=? AND phone_number=?"
                    );
                    ps.setString(1, (String) req.get("username"));
                    ps.setString(2, (String) req.get("phone_number"));
                    ResultSet rs = ps.executeQuery();

                    if (rs.next()) {
                        res.put("status", "success");
                        res.put("message", "Người dùng tồn tại, bạn có thể đổi mật khẩu");
                    } else {
                        res.put("status", "fail");
                        res.put("message", "Không tìm thấy tài khoản!");
                    }
                    break;
                }

                case "forgot_update": {
                    PreparedStatement ps = conn.prepareStatement(
                            "UPDATE brower.users SET password=?, con_password=? WHERE username=?"
                    );

                    String plainPass = (String) req.get("password");  // mật khẩu gốc
                    String hashPass = Jbcrypt.encodePassword((String) req.get("confirm_password")); // mật khẩu mã hóa

                    ps.setString(1, plainPass);   // cột password → lưu plain text
                    ps.setString(2, hashPass);    // cột con_password → lưu hash
                    ps.setString(3, (String) req.get("username"));

                    int row = ps.executeUpdate();

                    if (row > 0) {
                        res.put("status", "success");
                        res.put("message", "Cập nhật mật khẩu thành công!");
                    } else {
                        res.put("status", "fail");
                        res.put("message", "Cập nhật mật khẩu thất bại!");
                    }
                    break;
                }

                case "add_visit": {
                    int userId = Integer.parseInt(req.get("user_id").toString());
                    String url = (String) req.get("url");
                    String title = (String) req.get("title");
                    boolean hidden = Boolean.parseBoolean(req.get("hidden").toString());

                    try {
                        int urlId;

                        // 1) Kiểm tra URL tồn tại
                        PreparedStatement psCheck = conn.prepareStatement(
                                "SELECT id, title, visit_count, typed_count FROM brower.urls WHERE url = ?"
                        );
                        psCheck.setString(1, url);
                        ResultSet rs = psCheck.executeQuery();

                        if (rs.next()) {
                            // đã tồn tại → cập nhật
                            urlId = rs.getInt("id");
                            String oldTitle = rs.getString("title");
                            int visitCount = rs.getInt("visit_count") + 1;
                            int typedCount = rs.getInt("typed_count") + 1;

                            if (title != null && !title.trim().isEmpty() && !title.equals(oldTitle)) {
                                PreparedStatement psUpdate = conn.prepareStatement(
                                        "UPDATE brower.urls SET visit_count = ?, typed_count = ?, title = ?, last_visit_time = ? WHERE id = ?"
                                );
                                psUpdate.setInt(1, visitCount);
                                psUpdate.setInt(2, typedCount);
                                psUpdate.setString(3, title);
                                psUpdate.setTimestamp(4, new java.sql.Timestamp(System.currentTimeMillis()));
                                psUpdate.setInt(5, urlId);
                                psUpdate.executeUpdate();
                                psUpdate.close();
                            } else {
                                PreparedStatement psUpdate = conn.prepareStatement(
                                        "UPDATE brower.urls SET visit_count = ?, typed_count = ?, last_visit_time = ? WHERE id = ?"
                                );
                                psUpdate.setInt(1, visitCount);
                                psUpdate.setInt(2, typedCount);
                                psUpdate.setTimestamp(3, new java.sql.Timestamp(System.currentTimeMillis()));
                                psUpdate.setInt(4, urlId);
                                psUpdate.executeUpdate();
                                psUpdate.close();
                            }
                        } else {
                            // chưa có → thêm mới
                            PreparedStatement psInsert = conn.prepareStatement(
                                    "INSERT INTO brower.urls (url, title, visit_count, typed_count, hidden, last_visit_time) VALUES (?, ?, ?, ?, ?, ?)",
                                    Statement.RETURN_GENERATED_KEYS
                            );
                            psInsert.setString(1, url);
                            psInsert.setString(2, (title == null || title.isEmpty()) ? "Unknown" : title);
                            psInsert.setInt(3, 1);
                            psInsert.setInt(4, 1);
                            psInsert.setBoolean(5, hidden);
                            psInsert.setTimestamp(6, new java.sql.Timestamp(System.currentTimeMillis()));
                            psInsert.executeUpdate();

                            ResultSet keys = psInsert.getGeneratedKeys();
                            if (keys.next()) {
                                urlId = keys.getInt(1);
                            } else {
                                throw new SQLException("Không lấy được id mới của urls");
                            }
                            psInsert.close();
                            keys.close();
                        }

                        // 2) Thêm visits
                        PreparedStatement psVisit = conn.prepareStatement(
                                "INSERT INTO brower.visits (user_id, url_id, from_visit, transition_type, visit_time) VALUES (?, ?, ?, ?, ?)"
                        );
                        psVisit.setInt(1, userId);
                        psVisit.setInt(2, urlId);
                        psVisit.setNull(3, Types.INTEGER);
                        psVisit.setInt(4, 1); // transition_type = 1 (typed)
                        psVisit.setTimestamp(5, new java.sql.Timestamp(System.currentTimeMillis()));
                        psVisit.executeUpdate();
                        psVisit.close();

                        rs.close();
                        psCheck.close();

                        res.put("status", "success");
                        res.put("message", "Lưu lịch sử thành công!");
                    } catch (Exception ex) {
                        res.put("status", "fail");
                        res.put("message", "Lỗi khi lưu lịch sử: " + ex.getMessage());
                        ex.printStackTrace();
                    }
                    break;
                }

                case "show_history_user": {
                    try {
                        int userId = Integer.parseInt(req.get("user_id").toString());

                        PreparedStatement ps = conn.prepareStatement("""
                            SELECT u.url, v.visit_time
                            FROM brower.visits v
                            JOIN brower.urls u ON v.url_id = u.id
                            WHERE v.user_id = ?
                            ORDER BY v.visit_time DESC
                        """);
                        ps.setInt(1, userId);
                        ResultSet rs = ps.executeQuery();

                        List<Map<String, String>> historyList = new ArrayList<>();
                        while (rs.next()) {
                            Map<String, String> item = new HashMap<>();
                            item.put("url", rs.getString("url"));
                            item.put("visit_time", rs.getString("visit_time"));
                            historyList.add(item);
                        }

                        res.put("status", "success");
                        res.put("message", "Lấy lịch sử thành công");
                        res.put("data", historyList);

                        rs.close();
                        ps.close();

                    } catch (Exception ex) {
                        res.put("status", "fail");
                        res.put("message", "Lỗi khi lấy lịch sử: " + ex.getMessage());
                        ex.printStackTrace();
                    }
                    break;
                }

                case "show_bookmark_of_user": {
                    try {
                        int userId = Integer.parseInt(req.get("user_id").toString());
                        List<Map<String, Object>> list = new ArrayList<>();

                        String sql = """
                            SELECT b.id, b.user_id, b.url_id, b.title, b.position, u.url
                            FROM bookmarks b
                            JOIN urls u ON b.url_id = u.id
                            WHERE b.user_id = ?
                            ORDER BY b.position ASC
                        """;

                        PreparedStatement ps = conn.prepareStatement(sql);
                        ps.setInt(1, userId);
                        ResultSet rs = ps.executeQuery();

                        while (rs.next()) {
                            Map<String, Object> bm = new HashMap<>();
                            bm.put("id", rs.getInt("id"));
                            bm.put("user_id", rs.getInt("user_id"));
                            bm.put("url_id", rs.getInt("url_id"));
                            bm.put("title", rs.getString("title"));
                            bm.put("url", rs.getString("url"));
                            bm.put("position", rs.getInt("position"));
                            list.add(bm);
                        }
                        rs.close();
                        ps.close();

                        res.put("action", "show_bookmark_of_user_success");
                        res.put("status", "success");
                        res.put("bookmarks", list);

                        System.out.println("📑 User " + userId + " có " + list.size() + " bookmark(s).");

                    } catch (Exception e) {
                        res.put("action", "show_bookmark_of_user_fail");
                        res.put("status", "error");
                        res.put("message", e.getMessage());
                        e.printStackTrace();
                    }
                    break;
                }

                case "add_bookmark": {
                    try {
                        // 🔹 1. Lấy dữ liệu từ request
                        int userId = Integer.parseInt(req.get("user_id").toString());
                        String url = (String) req.get("url");
                        String title = (String) req.get("title");

                        // 🔹 2. Chuẩn hóa URL (tránh trùng www hoặc / cuối)
//                        String url = normalizeUrl(rawUrl);

                        // 🔹 3. Kiểm tra URL trong bảng urls
                        int urlId = 0;
                        PreparedStatement findUrl = conn.prepareStatement("SELECT id FROM urls WHERE url = ? LIMIT 1");
                        findUrl.setString(1, url);
                        ResultSet frs = findUrl.executeQuery();
                        if (frs.next()) {
                            urlId = frs.getInt("id");
                        }
                        frs.close();
                        findUrl.close();

                        // 🔹 4. Nếu chưa có thì thêm mới URL
                        if (urlId == 0) {
                            String insertUrl = "INSERT INTO urls (url, title, last_visit_time) VALUES (?, ?, NOW())";
                            PreparedStatement insertUrlStmt = conn.prepareStatement(insertUrl, Statement.RETURN_GENERATED_KEYS);
                            insertUrlStmt.setString(1, url);
                            insertUrlStmt.setString(2, title);
                            insertUrlStmt.executeUpdate();

                            ResultSet gk = insertUrlStmt.getGeneratedKeys();
                            if (gk.next()) urlId = gk.getInt(1);
                            gk.close();
                            insertUrlStmt.close();
                        }

                        // 🔹 5. Kiểm tra xem user đã bookmark URL này chưa
                        PreparedStatement checkBk = conn.prepareStatement(
                                "SELECT id FROM bookmarks WHERE user_id=? AND url_id=? LIMIT 1"
                        );
                        checkBk.setInt(1, userId);
                        checkBk.setInt(2, urlId);
                        ResultSet brs = checkBk.executeQuery();

                        if (brs.next()) {
                            // Đã tồn tại → không thêm nữa
                            brs.close();
                            checkBk.close();

                            res.put("status", "exists");
                            res.put("success", true);
                            res.put("message", "Bookmark đã tồn tại");
                        } else {
                            brs.close();
                            checkBk.close();

                            // 🔹 6. Tính vị trí tiếp theo của user
                            int nextPosition = 1;
                            PreparedStatement posStmt = conn.prepareStatement(
                                    "SELECT COALESCE(MAX(position), 0) + 1 AS next_pos FROM bookmarks WHERE user_id = ?"
                            );
                            posStmt.setInt(1, userId);
                            ResultSet prs = posStmt.executeQuery();
                            if (prs.next()) nextPosition = prs.getInt("next_pos");
                            prs.close();
                            posStmt.close();

                            // 🔹 7. Thêm bookmark mới
                            String insertBookmark = """
                                INSERT INTO bookmarks (user_id, url_id, title, date_added, position)
                                VALUES (?, ?, ?, NOW(), ?)
                            """;
                            PreparedStatement bmStmt = conn.prepareStatement(insertBookmark);
                            bmStmt.setInt(1, userId);
                            bmStmt.setInt(2, urlId);
                            bmStmt.setString(3, title);
                            bmStmt.setInt(4, nextPosition);
                            bmStmt.executeUpdate();
                            bmStmt.close();

                            res.put("status", "success");
                            res.put("success", true);
                            res.put("url", url);
                            res.put("title", title);
                            res.put("position", nextPosition);
                        }

                        System.out.println("⭐ User " + userId + " đã bookmark: " + title + " (" + url + ")");

                    } catch (Exception e) {
                        res.put("status", "error");
                        res.put("success", false);
                        res.put("message", e.getMessage());
                        e.printStackTrace();
                    }
                    break;
                }







                default:
                    res.put("status", "error");
                    res.put("message", "Yêu cầu không hợp lệ!");
            }

            // log ra server UI
            server.addLog("[" + action + "] " + res.get("message"));

        } catch (Exception e) {
            res.put("status", "error");
            res.put("message", "Server error: " + e.getMessage());
            e.printStackTrace();
        }

        return res;
    }

}
