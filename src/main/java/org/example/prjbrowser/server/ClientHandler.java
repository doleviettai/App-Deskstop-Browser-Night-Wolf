package org.example.prjbrowser.server;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.cdimascio.dotenv.Dotenv;
import org.example.prjbrowser.common.Message;
import org.example.prjbrowser.dao.SessionsDAO;
import org.example.prjbrowser.model.*;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.Socket;
import java.net.URL;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

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
                        // 🔥 Tăng số client online
                        server.increaseClientCount();
                        server.addLog("✔ Valid session from client.");
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

                case "upload_profile_image": {
                    try {
                        int userId = Integer.parseInt((String) req.get("user_id"));
                        byte[] imageData = (byte[]) req.get("image_data");

                        if (imageData == null || imageData.length == 0) {
                            res.put("status", "fail");
                            res.put("message", "Không có dữ liệu ảnh để lưu.");
                            break;
                        }

                        PreparedStatement ps = conn.prepareStatement(
                                "UPDATE brower.users SET avatar = ? WHERE id = ?"
                        );
                        ps.setBytes(1, imageData);
                        ps.setInt(2, userId);

                        int rows = ps.executeUpdate();

                        if (rows > 0) {
                            res.put("status", "success");
                            res.put("message", "Cập nhật ảnh đại diện thành công!");
                        } else {
                            res.put("status", "fail");
                            res.put("message", "Không tìm thấy người dùng có ID tương ứng.");
                        }

                    } catch (SQLException e) {
                        e.printStackTrace();
                        res.put("status", "fail");
                        res.put("message", "Lỗi SQL: " + e.getMessage());
                    } catch (Exception e) {
                        e.printStackTrace();
                        res.put("status", "fail");
                        res.put("message", "Lỗi khi xử lý upload ảnh: " + e.getMessage());
                    }
                    break;
                }

                case "get_user_avatar": {
                    try {
                        int userId = Integer.parseInt((String) req.get("user_id"));
                        PreparedStatement ps = conn.prepareStatement("SELECT avatar FROM brower.users WHERE id=?");
                        ps.setInt(1, userId);
                        ResultSet rs = ps.executeQuery();

                        if (rs.next()) {
                            byte[] avatarBytes = rs.getBytes("avatar");
                            res.put("status", "success");
                            res.put("avatar", avatarBytes); // có thể null nếu chưa upload
                        } else {
                            res.put("status", "fail");
                            res.put("message", "Không tìm thấy người dùng.");
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        res.put("status", "fail");
                        res.put("message", "Lỗi khi lấy ảnh: " + e.getMessage());
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
                            SELECT v.id AS visit_id, u.url, v.visit_time
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
                            item.put("id", rs.getString("visit_id"));
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


                case "delete_history_user": {
                    try {
                        int visitId = Integer.parseInt(req.get("visit_id").toString());

                        PreparedStatement ps = conn.prepareStatement("""
                            DELETE FROM brower.visits 
                            WHERE id = ?
                        """);
                        ps.setInt(1, visitId);

                        int rows = ps.executeUpdate();

                        if (rows > 0) {
                            res.put("status", "success");
                            res.put("message", "Đã xóa lịch sử truy cập thành công");
                        } else {
                            res.put("status", "fail");
                            res.put("message", "Không tìm thấy mục cần xóa");
                        }

                        ps.close();
                    } catch (Exception ex) {
                        res.put("status", "fail");
                        res.put("message", "Lỗi khi xóa lịch sử: " + ex.getMessage());
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

                case "delete_bookmark": {
                    try {
                        int userId = Integer.parseInt(req.get("user_id").toString());
                        String url = req.get("url").toString();

                        // Lấy id của url trước
                        String getUrlIdSql = "SELECT id FROM urls WHERE url = ?";
                        PreparedStatement ps1 = conn.prepareStatement(getUrlIdSql);
                        ps1.setString(1, url);
                        ResultSet rs = ps1.executeQuery();

                        int urlId = -1;
                        if (rs.next()) {
                            urlId = rs.getInt("id");
                        }
                        rs.close();
                        ps1.close();

                        if (urlId == -1) {
                            res.put("status", "error");
                            res.put("message", "URL không tồn tại trong CSDL");
                            break;
                        }

                        // Xóa khỏi bảng bookmarks
                        String deleteSql = "DELETE FROM bookmarks WHERE user_id = ? AND url_id = ?";
                        PreparedStatement ps2 = conn.prepareStatement(deleteSql);
                        ps2.setInt(1, userId);
                        ps2.setInt(2, urlId);

                        int rows = ps2.executeUpdate();
                        ps2.close();

                        if (rows > 0) {
                            res.put("status", "success");
                            res.put("message", "Đã xóa bookmark thành công");
                            System.out.println("🗑️ User " + userId + " đã xóa bookmark url_id=" + urlId);
                        } else {
                            res.put("status", "error");
                            res.put("message", "Không tìm thấy bookmark để xóa");
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        res.put("status", "error");
                        res.put("message", e.getMessage());
                    }
                    break;
                }

                case "save_cookie": {
                    int userId = Integer.parseInt(req.get("user_id").toString());
                    String host = (String) req.get("host_key");
                    String name = (String) req.get("name");
                    String value = (String) req.get("value");

                    // --- 1. Kiểm tra cookie đã tồn tại chưa ---
                    PreparedStatement check = conn.prepareStatement("""
                        SELECT id FROM cookies WHERE user_id = ? AND host_key = ? AND name = ?
                    """);
                    check.setInt(1, userId);
                    check.setString(2, host);
                    check.setString(3, name);
                    ResultSet rs = check.executeQuery();

                    if (rs.next()) {
                        // --- 2. Nếu đã có, UPDATE value + thời gian truy cập ---
                        int cookieId = rs.getInt("id");
                        PreparedStatement update = conn.prepareStatement("""
                            UPDATE cookies 
                            SET value = ?, last_access_time = NOW() 
                            WHERE id = ?
                        """);
                        update.setString(1, value);
                        update.setInt(2, cookieId);
                        update.executeUpdate();
                        update.close();

                        res.put("status", "updated");
                        res.put("message", "Cookie đã tồn tại, cập nhật giá trị mới");
                    } else {
                        // --- 3. Nếu chưa có, INSERT cookie mới ---
                        PreparedStatement insert = conn.prepareStatement("""
                            INSERT INTO cookies (user_id, host_key, name, value, creation_time, last_access_time)
                            VALUES (?, ?, ?, ?, NOW(), NOW())
                        """);
                        insert.setInt(1, userId);
                        insert.setString(2, host);
                        insert.setString(3, name);
                        insert.setString(4, value);
                        insert.executeUpdate();
                        insert.close();

                        res.put("status", "inserted");
                        res.put("message", "Đã lưu cookie mới");
                    }

                    rs.close();
                    check.close();
                    break;
                }


                case "get_cookies": {
                    int userId = Integer.parseInt(req.get("user_id").toString());
                    String host = (String) req.get("host_key");

                    PreparedStatement ps = conn.prepareStatement("""
                        SELECT host_key, name, value, http_only, creation_time 
                        FROM cookies 
                        WHERE user_id = ? AND host_key = ?
                    """);
                    ps.setInt(1, userId);
                    ps.setString(2, host);
                    ResultSet rs = ps.executeQuery();

                    List<Map<String, Object>> cookies = new ArrayList<>();
                    while (rs.next()) {
                        Map<String, Object> ck = new HashMap<>();
                        ck.put("host_key", rs.getString("host_key"));
                        ck.put("name", rs.getString("name"));
                        ck.put("value", rs.getString("value"));
                        ck.put("http_only", rs.getBoolean("http_only"));
                        ck.put("creation_time", rs.getTimestamp("creation_time"));
                        cookies.add(ck);
                    }

                    rs.close(); ps.close();
                    res.put("status", "success");
                    res.put("cookies", cookies);
                    break;
                }


                case "delete_cookie": {
                    int userId = Integer.parseInt(req.get("user_id").toString());
                    String host = (String) req.get("host_key");
                    String name = (String) req.get("name");

                    PreparedStatement ps = conn.prepareStatement("""
                        DELETE FROM cookies WHERE user_id = ? AND host_key = ? AND name = ?
                    """);
                    ps.setInt(1, userId);
                    ps.setString(2, host);
                    ps.setString(3, name);

                    int rows = ps.executeUpdate();
                    ps.close();

                    res.put("status", "success");
                    res.put("message", rows > 0 ? "Cookie deleted" : "No cookie found to delete");
                    break;
                }

                case "delete_all_cookies_for_host": {
                    int userId = Integer.parseInt(req.get("user_id").toString());
                    String host = (String) req.get("host_key");

                    PreparedStatement ps = conn.prepareStatement("""
                        DELETE FROM cookies WHERE user_id = ? AND host_key = ?
                    """);
                    ps.setInt(1, userId);
                    ps.setString(2, host);

                    int rows = ps.executeUpdate();
                    ps.close();

                    res.put("status", "success");
                    res.put("message", rows + " cookie(s) deleted for host " + host);
                    break;
                }

                case "save_resource_cache": {
                    try {
                        int userId = Integer.parseInt(req.get("user_id").toString());
                        String resourceUrl = (String) req.get("resource_url");
                        String etag = (String) req.getOrDefault("etag", null);
                        String lastModifiedRaw = (String) req.getOrDefault("last_modified", null);
                        String contentType = (String) req.getOrDefault("content_type", "application/octet-stream");
                        String base64Content = (String) req.get("content");
                        int size = Integer.parseInt(req.get("size").toString());

                        byte[] content = Base64.getDecoder().decode(base64Content);

                        int urlId = getOrCreateUrlId(conn, resourceUrl);

                        // ✅ Parse Last-Modified (hỗ trợ ICT)
                        String lastModifiedFormatted = null;
                        if (lastModifiedRaw != null && !lastModifiedRaw.isEmpty()) {
                            try {
                                // 👉 Bước 1: Chuẩn hóa timezone ICT -> GMT+07:00
                                String fixed = lastModifiedRaw
                                        .replace("ICT", "GMT+07:00")
                                        .replace("GMT+7", "GMT+07:00"); // thêm fallback

                                // 👉 Bước 2: Dùng formatter hỗ trợ offset dạng +07:00
                                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.ENGLISH);
                                ZonedDateTime zoned = ZonedDateTime.parse(fixed, formatter);

                                // 👉 Bước 3: Chuyển về dạng LocalDateTime phù hợp để lưu DB
                                LocalDateTime local = zoned.withZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime();
                                lastModifiedFormatted = local.toString().replace('T', ' ');

                                System.out.println("✅ Parsed Last-Modified: " + lastModifiedFormatted);
                            } catch (Exception ex) {
                                System.out.println("⚠️ Không thể parse Last-Modified: " + lastModifiedRaw);
                                lastModifiedFormatted = null;
                            }
                        }


                        PreparedStatement ps = conn.prepareStatement("""
                            INSERT INTO resource_cache 
                                (url_id, user_id, resource_url, etag, last_modified, content, content_type, size, recv_time, expire_time)
                            VALUES (?, ?, ?, ?, ?, ?, ?, ?, NOW(), DATE_ADD(NOW(), INTERVAL 7 DAY))
                            ON DUPLICATE KEY UPDATE 
                                content = VALUES(content),
                                etag = VALUES(etag),
                                last_modified = VALUES(last_modified),
                                content_type = VALUES(content_type),
                                recv_time = NOW(),
                                expire_time = DATE_ADD(NOW(), INTERVAL 7 DAY),
                                size = VALUES(size)
                        """);

                        ps.setInt(1, urlId);
                        ps.setInt(2, userId);
                        ps.setString(3, resourceUrl);
                        ps.setString(4, etag);
                        ps.setString(5, lastModifiedFormatted);
                        ps.setBytes(6, content);
                        ps.setString(7, contentType);
                        ps.setInt(8, size);
                        ps.executeUpdate();
                        ps.close();

                        res.put("status", "success");
                        res.put("message", "✅ Cache saved or updated successfully!");
                    } catch (Exception e) {
                        e.printStackTrace();
                        res.put("status", "error");
                        res.put("message", "❌ " + e.getMessage());
                    }
                    break;
                }

                case "get_resource_cache": {
                    try {
                        int userId = Integer.parseInt(req.get("user_id").toString());
                        String resourceUrl = (String) req.get("resource_url");

                        // ✅ Chuẩn hóa URL cho chắc ăn (tránh / cuối)
                        if (resourceUrl != null && resourceUrl.endsWith("/")) {
                            resourceUrl = resourceUrl.substring(0, resourceUrl.length() - 1);
                        }

                        // 🔹 1️⃣ Tìm url_id tương ứng (KHÔNG tạo mới)
                        int urlId = -1;
                        try (PreparedStatement ps = conn.prepareStatement(
                                "SELECT id FROM urls WHERE url = ? LIMIT 1")) {
                            ps.setString(1, resourceUrl);
                            ResultSet rs = ps.executeQuery();
                            if (rs.next()) {
                                urlId = rs.getInt("id");
                            }
                            rs.close();
                        }

                        if (urlId == -1) {
                            // Không tìm thấy url_id
                            res.put("status", "not_found");
                            res.put("message", "Không tìm thấy cache cho URL này (chưa có trong bảng urls)");
                            break;
                        }

                        // 🔹 2️⃣ Truy vấn các cache tương ứng (theo url_id + user_id)
                        String sql = """
            SELECT *
            FROM resource_cache
            WHERE url_id = ? AND user_id = ?
            ORDER BY recv_time DESC
        """;
                        PreparedStatement ps = conn.prepareStatement(sql);
                        ps.setInt(1, urlId);
                        ps.setInt(2, userId);
                        ResultSet rs = ps.executeQuery();

                        List<Map<String, Object>> caches = new ArrayList<>();
                        while (rs.next()) {
                            Timestamp expireTime = rs.getTimestamp("expire_time");
                            boolean expired = expireTime != null && expireTime.before(new Timestamp(System.currentTimeMillis()));

                            if (!expired) {
                                Map<String, Object> cache = new HashMap<>();
                                cache.put("id", rs.getInt("id"));
                                cache.put("resource_url", rs.getString("resource_url"));
                                cache.put("etag", rs.getString("etag"));
                                cache.put("last_modified", String.valueOf(rs.getTimestamp("last_modified")));
                                cache.put("content", Base64.getEncoder().encodeToString(rs.getBytes("content")));
                                cache.put("content_type", rs.getString("content_type"));
                                cache.put("recv_time", String.valueOf(rs.getTimestamp("recv_time")));
                                cache.put("expire_time", String.valueOf(rs.getTimestamp("expire_time")));
                                cache.put("size", rs.getInt("size"));
                                caches.add(cache);
                            }
                        }

                        rs.close();
                        ps.close();

                        // 🔹 3️⃣ Trả kết quả về client
                        if (caches.isEmpty()) {
                            res.put("status", "not_found");
                            res.put("message", "Không tìm thấy cache nào hợp lệ cho tài nguyên này");
                        } else {
                            res.put("status", "success");
                            res.put("caches", caches);
                        }

                    } catch (Exception e) {
                        e.printStackTrace();
                        res.put("status", "error");
                        res.put("message", "❌ Lỗi khi lấy cache: " + e.getMessage());
                    }
                    break;
                }


                // 🔹 Xóa cache theo ID
                case "delete_resource_cache": {
                    try {
                        int cacheId = Integer.parseInt(req.get("cache_id").toString());
                        PreparedStatement ps = conn.prepareStatement("DELETE FROM resource_cache WHERE id = ?");
                        ps.setInt(1, cacheId);

                        int affected = ps.executeUpdate();
                        ps.close();

                        if (affected > 0) {
                            res.put("status", "success");
                            res.put("message", "🗑️ Xóa cache thành công (ID: " + cacheId + ")");
                        } else {
                            res.put("status", "not_found");
                            res.put("message", "Không tìm thấy cache cần xóa");
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        res.put("status", "error");
                        res.put("message", "❌ Lỗi khi xóa cache: " + e.getMessage());
                    }
                    break;
                }

                // 🔹 Xóa toàn bộ cache của user theo URL
                case "delete_all_resource_cache": {
                    try {
                        int userId = Integer.parseInt(req.get("user_id").toString());
                        String resourceUrl = (String) req.get("resource_url");

                        Integer urlId = getUrlIdIfExists(conn, resourceUrl);
                        if (urlId == null) {
                            res.put("status", "not_found");
                            res.put("message", "Không tìm thấy URL trong bảng urls");
                            break;
                        }

                        PreparedStatement ps = conn.prepareStatement("""
            DELETE FROM resource_cache WHERE url_id = ? AND user_id = ?
        """);
                        ps.setInt(1, urlId);
                        ps.setInt(2, userId);

                        int affected = ps.executeUpdate();
                        ps.close();

                        res.put("status", "success");
                        res.put("message", "🧹 Đã xóa " + affected + " cache cho URL hiện tại");
                    } catch (Exception e) {
                        e.printStackTrace();
                        res.put("status", "error");
                        res.put("message", "❌ Lỗi khi xóa tất cả cache: " + e.getMessage());
                    }
                    break;
                }

                case "list_conversations":
                    handleListConversations(conn, req, res);
                    break;

                case "new_conversation":
                    handleNewConversation(conn, req, res);
                    break;

                case "search_conversation":
                    handleSearchConversation(conn, req, res);
                    break;

                case "delete_item_conversation":
                    handleDeleteItemConversation(conn, req, res);
                    break;

                case "get_history":
                    handleGetHistory(conn, req, res);
                    break;

                case "send_message":
                    handleSendMessage(conn, req, res);
                    break;

                case "open_conversation":
                    handleOpenConversation(conn, req, res);
                    break;

                case "add_or_update_feedback":
                    handleAddOrUpMessageFeedback(conn, req, res);
                    break;

                case "delete_message_feedback":
                    handleDeleteMessageFeedback(conn, req, res);
                    break;






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

    private int getOrCreateUrlId(Connection conn, String url) throws SQLException {
        int id = -1;

        // 🔹 1️⃣ Kiểm tra xem URL đã tồn tại chưa
        String select = "SELECT id FROM urls WHERE url = ?";
        try (PreparedStatement ps = conn.prepareStatement(select)) {
            ps.setString(1, url);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                id = rs.getInt("id");
            }
        }

        // 🔹 2️⃣ Nếu chưa có → tạo mới
        if (id == -1) {
            String insert = "INSERT INTO urls (url, last_visit_time) VALUES (?, NOW())";
            try (PreparedStatement ps = conn.prepareStatement(insert, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, url);
                ps.executeUpdate();
                ResultSet rs = ps.getGeneratedKeys();
                if (rs.next()) {
                    id = rs.getInt(1);
                }
            }
        }

        return id;
    }

    private Integer getUrlIdIfExists(Connection conn, String resourceUrl) throws SQLException {
        Integer id = null;
        String sql = "SELECT id FROM urls WHERE url = ? LIMIT 1";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, resourceUrl);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                id = rs.getInt("id");
            }
            rs.close();
        }
        return id;
    }

    private void handleListConversations(Connection conn, Message request, Message response) throws SQLException {
        Object uidObj = request.get("user_id");
        int userId = -1;

        if (uidObj instanceof Number) {
            userId = ((Number) uidObj).intValue();
        } else if (uidObj instanceof String) {
            try {
                userId = Integer.parseInt((String) uidObj);
            } catch (NumberFormatException e) {
                // invalid string
            }
        }

        if (userId <= 0) {
            response.put("status", "error");
            response.put("message", "Missing or invalid user_id");
            return;
        }

        List<Conversation> conversations = new ArrayList<>();

        String sql = "SELECT id, user_id, title FROM conversations WHERE user_id=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                conversations.add(new Conversation(
                        rs.getInt("id"),
                        rs.getInt("user_id"),
                        rs.getString("title")
                ));
            }
        }

        response.put("status", "ok");
        response.put("action", "list_conversations");
        response.put("conversations", conversations);
    }

    private void handleNewConversation(Connection conn, Message request, Message response) throws SQLException {
        // ✅ Parse user_id an toàn
        Object uidObj = request.get("user_id");
        int userId = -1;

        if (uidObj instanceof Number) {
            userId = ((Number) uidObj).intValue();
        } else if (uidObj instanceof String) {
            try {
                userId = Integer.parseInt((String) uidObj);
            } catch (NumberFormatException ignored) {}
        }

        if (userId <= 0) {
            response.put("status", "error");
            response.put("message", "Missing or invalid user_id");
            return;
        }

        // Lấy title, mặc định "New Chat"
        String title = (String) request.getOrDefault("title", "New Chat");

        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO conversations(user_id, title) VALUES(?, ?)",
                Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt(1, userId);
            ps.setString(2, title);

            int affected = ps.executeUpdate();
            if (affected > 0) {
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) {
                        // Trả về đối tượng Conversation
                        Conversation conv = new Conversation(keys.getInt(1), userId, title);
                        response.put("status", "ok");
                        response.put("action", "new_conversation");
                        response.put("conversation", conv);
                    }
                }
            }
        } catch (SQLException e) {
            response.put("status", "error");
            response.put("message", "Database error: " + e.getMessage());
        }
    }

    private void handleSearchConversation(Connection conn, Message request, Message response) throws SQLException {
        // ✅ Parse user_id an toàn
        Object uidObj = request.get("user_id");
        int userId = -1;

        if (uidObj instanceof Number) {
            userId = ((Number) uidObj).intValue();
        } else if (uidObj instanceof String) {
            try {
                userId = Integer.parseInt((String) uidObj);
            } catch (NumberFormatException ignored) {}
        }

        if (userId <= 0) {
            response.put("status", "error");
            response.put("message", "Missing or invalid user_id");
            return;
        }

        String keyword = ((String) request.getOrDefault("keyword", "")).trim();

        List<Conversation> result = new ArrayList<>();

        String sql = "SELECT id, user_id, title FROM conversations WHERE user_id = ? AND title LIKE ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setString(2, "%" + keyword + "%"); // tìm gần đúng
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Conversation conv = new Conversation(
                            rs.getInt("id"),
                            rs.getInt("user_id"),
                            rs.getString("title")
                    );
                    result.add(conv);
                }
            }

            response.put("status", "ok");
            response.put("action", "search_conversation");
            response.put("conversations", result);

        } catch (SQLException e) {
            e.printStackTrace();
            response.put("status", "error");
            response.put("message", "Lỗi tìm kiếm conversation: " + e.getMessage());
        }
    }

    private void handleDeleteItemConversation(Connection conn,Message request, Message response) throws SQLException {
        int conversationId = (int) request.getOrDefault("conversationId", -1);
        if (conversationId == -1) {
            response.put("status", "error");
            response.put("message", "Invalid conversation ID");
            return;
        }

        try {
            conn.setAutoCommit(false);
            try {
                // Xóa tất cả tin nhắn thuộc conversation
                try (PreparedStatement psMsg = conn.prepareStatement(
                        "DELETE FROM messages WHERE conversation_id=?")) {
                    psMsg.setInt(1, conversationId);
                    psMsg.executeUpdate();
                }

                // Xóa conversation chính
                try (PreparedStatement psConv = conn.prepareStatement(
                        "DELETE FROM conversations WHERE id=?")) {
                    psConv.setInt(1, conversationId);
                    int affected = psConv.executeUpdate();
                    if (affected > 0) {
                        conn.commit();
                        response.put("status", "ok");
                        response.put("message", "Đã xóa hội thoại thành công!");
                    } else {
                        conn.rollback();
                        response.put("status", "error");
                        response.put("message", "Không tìm thấy hội thoại để xóa.");
                    }
                }
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }catch (Exception e){

        }
    }

    private void handleGetHistory(Connection conn,Message request, Message response) throws SQLException {
        int conversationId = (int) request.getOrDefault("conversationId", -1);
        if (conversationId == -1) {
            response.put("status", "error");
            response.put("message", "Invalid conversation ID");
            return;
        }

        List<Messages> messages = new ArrayList<>();

        try (
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT id, sender, content FROM messages WHERE conversation_id=? ORDER BY id ASC")) {

            ps.setInt(1, conversationId);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                messages.add(new Messages(
                        rs.getInt("id"),
                        conversationId,
                        rs.getString("sender"),
                        rs.getString("content")
                ));
            }
        }

        response.put("status", "ok");
        response.put("action", "get_history");
        response.put("messages", messages);
    }

    private void handleSendMessage(Connection conn,Message request, Message response) throws SQLException {
        int conversationId = (int) request.getOrDefault("conversationId", -1);
        String content = (String) request.getOrDefault("content", "");
        if (conversationId == -1 || content.isEmpty()) {
            response.put("status", "error");
            response.put("message", "Invalid conversation ID or empty content");
            return;
        }

        try {
            // 1️⃣ Lưu tin nhắn user
            int userMessageId;
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO messages(conversation_id, sender, content) VALUES(?, 'user', ?)",
                    Statement.RETURN_GENERATED_KEYS)) {
                ps.setInt(1, conversationId);
                ps.setString(2, content);
                ps.executeUpdate();
                ResultSet keys = ps.getGeneratedKeys();
                userMessageId = keys.next() ? keys.getInt(1) : 0;
            }

            // 2️⃣ Lấy lịch sử tin nhắn gần nhất (ví dụ 10 tin)
            List<Messages> history = new ArrayList<>();
            try (PreparedStatement psHist = conn.prepareStatement(
                    "SELECT sender, content FROM messages WHERE conversation_id=? ORDER BY id ASC")) {
                psHist.setInt(1, conversationId);
                ResultSet rs = psHist.executeQuery();
                while (rs.next()) {
                    history.add(new Messages(0, conversationId,
                            rs.getString("sender"),
                            rs.getString("content")));
                }
            }

            // 3️⃣ Chuẩn bị context cho AI
            StringBuilder context = new StringBuilder();
            for (Messages msg : history) {
                context.append(msg.getSender().equals("user") ? "User: " : "AI: ")
                        .append(msg.getContent())
                        .append("\n");
            }
            context.append("User: ").append(content); // tin nhắn mới

            // 4️⃣ Gọi AI và lưu tin nhắn AI
            String aiContent = getAIResponse(context.toString(), conversationId, conn);

            // 5️⃣ Trả về client
            Messages userMsg = new Messages(userMessageId, conversationId, "user", content);
            Messages aiMsg = new Messages(0, conversationId, "ai", aiContent);

            response.put("status", "ok");
            response.put("action", "send_message");
            response.put("userMessage", userMsg);
            response.put("aiMessage", aiMsg);
        }catch (Exception e) {
            response.put("status", "error");
            response.put("message", "Server error: " + e.getMessage());
        }
    }

    private String getAIResponse(String context, int conversationId, Connection conn) {
        try {
            String aiResponse = callGeminiAPI(context); // gửi cả lịch sử và tin nhắn mới
            saveAIMessage("", aiResponse, conversationId, conn); // lưu vào DB
            return aiResponse;
        } catch (Exception e) {
            return "🤖 AI: Không thể trả lời câu hỏi của bạn. Lỗi: " + e.getMessage();
        }
    }


    private void handleOpenConversation(Connection conn,Message request, Message response) throws SQLException {
        int conversationId = (int) request.getOrDefault("conversationId", -1);
        if (conversationId == -1) {
            response.put("status", "error");
            response.put("message", "Invalid conversation ID");
            return;
        }

        try (
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT id, user_id, title FROM conversations WHERE id=?")) {

            ps.setInt(1, conversationId);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                Conversation conv = new Conversation(
                        rs.getInt("id"),
                        rs.getInt("user_id"),
                        rs.getString("title")
                );
                response.put("status", "ok");
                response.put("action", "open_conversation");
                response.put("conversation", conv);
            } else {
                response.put("status", "error");
                response.put("message", "Conversation not found");
            }
        }
    }


    private String callGeminiAPI(String userMessage) throws Exception {
//        String apiKey = "AIzaSyAQeRX9PMuyIH2OxHlET_VcdtTtdf46tsg";
//        String apiKey = "AIzaSyBbpyqGOBGMVH5nF6LhcBPwp4UWT01MP7I";
//        String apiEndpoint =
//                "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=" + apiKey;
//        String apiEndpoint = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=" + apiKey;
        // Load API key từ file .env
        Dotenv dotenv = Dotenv.load();
        String apiKey = dotenv.get("GEMINI_API_KEY");

        if (apiKey == null || apiKey.isEmpty()) {
            return "❌ Lỗi: Không tìm thấy GEMINI_API_KEY trong file .env";
        }

        String apiEndpoint =
                "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=" + apiKey;

        // ✅ Chuẩn JSON body
        String jsonInputString = """
    {
      "contents": [
        {
          "role": "user",
          "parts": [{"text": "%s"}]
        }
      ]
    }
    """.formatted(userMessage.replace("\"", "\\\""));

        URL url = new URL(apiEndpoint);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        connection.setDoOutput(true);

        try (OutputStream os = connection.getOutputStream()) {
            byte[] input = jsonInputString.getBytes("utf-8");
            os.write(input, 0, input.length);
        }

        int status = connection.getResponseCode();
        InputStream stream = (status >= 200 && status < 300)
                ? connection.getInputStream()
                : connection.getErrorStream();

        BufferedReader reader = new BufferedReader(new InputStreamReader(stream, "utf-8"));
        StringBuilder response = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            response.append(line);
        }
        reader.close();

        String responseBody = response.toString();

        // ✅ Parse JSON phản hồi chính xác
        try {
            JsonObject json = JsonParser.parseString(responseBody).getAsJsonObject();
            String aiResponse = json.getAsJsonArray("candidates")
                    .get(0).getAsJsonObject()
                    .getAsJsonObject("content")
                    .getAsJsonArray("parts")
                    .get(0).getAsJsonObject()
                    .get("text").getAsString();
            return aiResponse;
        } catch (Exception e) {
            return "Không thể phân tích phản hồi từ AI. Phản hồi: " + responseBody;
        }
    }

    private void saveAIMessage(String userMessage, String aiMessage, int conversationId, Connection conn) throws SQLException {
        // Lưu tin nhắn AI
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO messages(conversation_id, sender, content) VALUES(?, 'ai', ?)")) {
            ps.setInt(1, conversationId);
            ps.setString(2, aiMessage);
            ps.executeUpdate();
        }
    }



    private void saveQuestionAndAnswer(Connection conn,String question, String answer) throws SQLException {
        try (
             PreparedStatement checkStmt = conn.prepareStatement(
                     "SELECT id FROM messages WHERE content = ?")) {
            checkStmt.setString(1, question);
            ResultSet rs = checkStmt.executeQuery();

            if (rs.next()) {
                return; // Nếu câu hỏi đã tồn tại, không cần lưu lại
            }

            // Lưu câu hỏi vào DB
            try (PreparedStatement insertStmt = conn.prepareStatement(
                    "INSERT INTO messages(conversation_id, sender, content) VALUES(?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS)) {
                insertStmt.setInt(1, 1); // Giả sử Conversation ID là 1, điều chỉnh theo nhu cầu
                insertStmt.setString(2, "user");
                insertStmt.setString(3, question);
                insertStmt.executeUpdate();
            }

            // Lưu câu trả lời từ AI
            try (PreparedStatement aiResponseStmt = conn.prepareStatement(
                    "INSERT INTO messages(conversation_id, sender, content) VALUES(?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS)) {
                aiResponseStmt.setInt(1, 1); // Giả sử Conversation ID là 1
                aiResponseStmt.setString(2, "ai");
                aiResponseStmt.setString(3, answer);
                aiResponseStmt.executeUpdate();
            }
        }
    }

    private void handleAddOrUpMessageFeedback(Connection conn, Message request, Message response) {
        // ✅ Parse messageId an toàn
        Object msgIdObj = request.get("messageId");
        int messageId = -1;
        if (msgIdObj instanceof Number) {
            messageId = ((Number) msgIdObj).intValue();
        } else if (msgIdObj instanceof String) {
            try {
                messageId = Integer.parseInt((String) msgIdObj);
            } catch (NumberFormatException ignored) {}
        }

        // ✅ Parse user_id an toàn
        Object uidObj = request.get("user_id");
        int userId = -1;
        if (uidObj instanceof Number) {
            userId = ((Number) uidObj).intValue();
        } else if (uidObj instanceof String) {
            try {
                userId = Integer.parseInt((String) uidObj);
            } catch (NumberFormatException ignored) {}
        }

        if (messageId <= 0 || userId <= 0) {
            response.put("status", "error");
            response.put("message", "Missing or invalid messageId/user_id");
            return;
        }

        String feedback = (String) request.get("feedback");
        String comment = (String) request.getOrDefault("comment", "");

        try {
            // Kiểm tra đã có feedback chưa
            try (PreparedStatement psCheck = conn.prepareStatement(
                    "SELECT id FROM message_feedback WHERE message_id=? AND user_id=?")) {
                psCheck.setInt(1, messageId);
                psCheck.setInt(2, userId);

                try (ResultSet rs = psCheck.executeQuery()) {
                    if (rs.next()) {
                        // Cập nhật nếu đã tồn tại
                        int feedbackId = rs.getInt("id");
                        try (PreparedStatement psUpdate = conn.prepareStatement(
                                "UPDATE message_feedback SET feedback=?, comment=?, created_at=NOW() WHERE id=?")) {
                            psUpdate.setString(1, feedback);
                            psUpdate.setString(2, comment);
                            psUpdate.setInt(3, feedbackId);
                            psUpdate.executeUpdate();
                        }
                        response.put("status", "ok");
                        response.put("message", "Cập nhật phản hồi thành công");
                    } else {
                        // Thêm mới nếu chưa có
                        try (PreparedStatement psInsert = conn.prepareStatement(
                                "INSERT INTO message_feedback(message_id,user_id,feedback,comment,created_at) VALUES(?,?,?,?,NOW())")) {
                            psInsert.setInt(1, messageId);
                            psInsert.setInt(2, userId);
                            psInsert.setString(3, feedback);
                            psInsert.setString(4, comment);
                            psInsert.executeUpdate();
                        }
                        response.put("status", "ok");
                        response.put("message", "Gửi phản hồi thành công");
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            response.put("status", "error");
            response.put("message", e.getMessage());
        }
    }


    private void handleDeleteMessageFeedback(Connection conn, Message request, Message response) {
        // ✅ Parse messageId an toàn
        Object msgIdObj = request.get("messageId");
        int messageId = -1;
        if (msgIdObj instanceof Number) {
            messageId = ((Number) msgIdObj).intValue();
        } else if (msgIdObj instanceof String) {
            try {
                messageId = Integer.parseInt((String) msgIdObj);
            } catch (NumberFormatException ignored) {}
        }

        // ✅ Parse user_id an toàn
        Object uidObj = request.get("user_id");
        int userId = -1;
        if (uidObj instanceof Number) {
            userId = ((Number) uidObj).intValue();
        } else if (uidObj instanceof String) {
            try {
                userId = Integer.parseInt((String) uidObj);
            } catch (NumberFormatException ignored) {}
        }

        if (messageId <= 0 || userId <= 0) {
            response.put("status", "error");
            response.put("message", "Missing or invalid messageId/user_id");
            return;
        }

        try (PreparedStatement ps = conn.prepareStatement(
                "DELETE FROM message_feedback WHERE message_id=? AND user_id=?")) {

            ps.setInt(1, messageId);
            ps.setInt(2, userId);
            int affected = ps.executeUpdate();

            if (affected > 0) {
                response.put("status", "ok");
                response.put("message", "Xóa phản hồi thành công");
            } else {
                response.put("status", "error");
                response.put("message", "Không tìm thấy phản hồi để xóa");
            }

        } catch (SQLException e) {
            e.printStackTrace();
            response.put("status", "error");
            response.put("message", e.getMessage());
        }
    }

}
