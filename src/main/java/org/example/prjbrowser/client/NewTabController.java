package org.example.prjbrowser.client;

import javafx.animation.*;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Worker;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebHistory;
import javafx.scene.web.WebView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;
import netscape.javascript.JSObject;
import org.example.prjbrowser.client.desginer.dialog;
import org.example.prjbrowser.common.Message;
import org.example.prjbrowser.model.*;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.model.StyleSpans;
import org.fxmisc.richtext.model.StyleSpansBuilder;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NewTabController implements Initializable {

    @FXML
    public TextField search;

    @FXML
    public ScrollPane tabScrollPane;

    @FXML
    private BorderPane main_browser;

    @FXML
    private ProgressBar progress;

    @FXML
    private WebView webView;

    @FXML
    private AnchorPane mainBackground;

    @FXML
    private AnchorPane historyBrowser;

    @FXML
    private TextField searchBrowser;

    @FXML
    private TableView<HistoryItem> table_history_browser;

    @FXML
    private TableColumn<HistoryItem, String> time_col_table;

    @FXML
    private TableColumn<HistoryItem, String> url_col_table;

    @FXML
    private TableColumn<?, ?> action_col_table;

    @FXML
    private Button backBtn;
    @FXML
    private Button forwardBtn;
    @FXML
    private Button Logout;
    @FXML
    private Button Login;
    @FXML
    private Button list_History;
    @FXML
    public Button reloadBtn;
    @FXML
    private Button zoomInBtn;
    @FXML
    private Button zoomOutBtn;
    @FXML
    private AnchorPane overlay;
    @FXML
    private AnchorPane slideMenu;
    @FXML
    private StackPane stackRoot;
    @FXML
    private Label username_browser;
    @FXML
    private ImageView image_browser;
    @FXML
    private Label Date_label;
    @FXML
    private Label Time_label;
    @FXML
    private Button bookmarkBtn;
    @FXML
    private HBox tabBar;

    @FXML
    private AnchorPane browserAlert;

    @FXML
    private Label alertMessage;

    private Timeline alertTimeline;

    @FXML
    private AnchorPane htmlInspector;

    @FXML
    private CodeArea htmlCodeArea;

    @FXML
    private SplitPane splitPane;

    private boolean inspectorVisible = false;

    private File selectedImageFile; // lưu file ảnh vừa chọn

    private WebEngine engine;
    private double zoomLevel = 1.0;
    private boolean isMenuOpen = false;

    private String currentId = "";
    private String currentUsername;
    private String currentFullname;
    private String currentPageTitle = "";

    private dialog dl = new dialog();

    String username_log;
    // Biến toàn cục trong class (NewTabController)
    private String lastSavedUrl = "";
    private String pendingUrl = "";
    private boolean manualLoad = false;

    private boolean atHome = true; // trạng thái: true = đang ở "trang chủ"

//  Tích hợp chatBot
    @FXML private Label welcomeLabel;

    // Left menu
    @FXML private TextField searchField;
    @FXML private VBox conversationListVBox;
    @FXML private Button newChatButton;

    // Chat center
    @FXML private ScrollPane chatScrollPane;
    @FXML private VBox chatVBox;
    @FXML private TextField messageField;
    @FXML private Button sendButton;

    @FXML private StackPane menuContainer;
    @FXML private StackPane centerContainer;
    @FXML private AnchorPane centerAnchor;
    @FXML private VBox menuVBox;
    @FXML private VBox centerVBox;
    @FXML private Button toggleMenuButton;
    @FXML private StackPane leftBox;

    private boolean menuVisible = true;

//    @FXML
//    private SplitPane splitPane; // SplitPane chính (chứa splitPane_chilren và htmlInspector)
    @FXML
    private BorderPane leftPane;
    @FXML
    private SplitPane splitPane_chilren; // SplitPane bên trong (chứa leftPane và WebView)
    @FXML
    private Button toggleInspectButton; // Nút ☰

    private boolean isLeftPaneVisible = true; // Mặc định hiển thị

//    @FXML
//    private WebView webView;

    // --- Socket dài hạn từ LoginController ---
    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;



//    private int userId;
    private int currentConversationId = -1;

    public void receiverNickName(String id,String username, String fullname) {
        this.currentId = id;
        this.currentUsername = username;
        this.currentFullname = fullname;
        username_browser.setText(id+" "+fullname); // hiển thị họ tên

        if (username_browser != null)
            username_browser.setText(id + " " + fullname);

        // Load avatar từ server
        loadUserAvatar(id);

        // gọi cập nhật nút login/logout
        loadUserBookmarks();
        Login_Logout();
    }

    // getter nếu cần dùng sau
    public String getCurrentId(){return currentId;}
    public String getCurrentUsername() { return currentUsername; }
    public String getCurrentFullname() { return currentFullname; }
    public String getCurrentPageTitle() {return currentPageTitle; }
    public void setCurrentPageTitle(String title) {
        this.currentPageTitle = title;
    }

    private Message sendRequest(Message request) throws IOException, ClassNotFoundException {
        Socket socket = new Socket("localhost", 12345);
//        Socket socket = new Socket("172.20.10.2", 12345);
//        Socket socket = new Socket("192.168.56.1", 12345);
        ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
        ObjectInputStream in = new ObjectInputStream(socket.getInputStream());

        out.writeObject(request);
        out.flush();

        Message response = (Message) in.readObject();

        in.close();
        out.close();
        socket.close();

        return response;
    }

    public void updateTime() {
        // Lấy thời gian hiện tại từ hệ thống
        LocalDateTime now = LocalDateTime.now();

        // Định dạng ngày tháng
        DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        Date_label.setText("Date: " + dateFormat.format(now));

        int hour = now.getHour();
        boolean isDaytime = (hour >= 6 && hour < 18);

        String period = (hour < 12) ? "AM" : "PM";
        DateTimeFormatter timeFormat = DateTimeFormatter.ofPattern("hh:mm:ss");
        Time_label.setText("Time: " + timeFormat.format(now) + " " + period);

        if (isDaytime) {
            Time_label.setStyle("-fx-background-color: linear-gradient(to top, #0980F7, #00d4FF);");
            Time_label.setTextFill(Color.BLACK);
        } else {
            Time_label.setStyle("-fx-background-color: #000;");
            Time_label.setTextFill(Color.WHITE);
        }
    }

    public void TimeChay() {
        // Tạo một timeline để cập nhật thời gian mỗi giây
        Timeline timeline = new Timeline(new KeyFrame(Duration.seconds(1), event -> updateTime()));
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();

        updateTime();
    }


    public void loadUrl() {
        manualLoad = true; // đánh dấu là load thủ công
        String inputUrl = search.getText().trim();
        if (inputUrl.isEmpty()) return;

        final String url = normalizeUrl(inputUrl);
        atHome = false;
        mainBackground.setVisible(false);
        historyBrowser.setVisible(false);
        webView.setVisible(true);

        WebEngine engine = webView.getEngine();
        final String finalUrl = url; // ✅ bản sao dùng trong lambda

        // 🧩 Giả lập Chrome
        engine.setUserAgent(
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
                        "AppleWebKit/537.36 (KHTML, like Gecko) " +
                        "Chrome/120.0.0.0 Safari/537.36"
        );

        Worker<Void> worker = engine.getLoadWorker();

        // Xử lý trạng thái tải trang
        worker.stateProperty().addListener((obs, oldState, newState) -> {
            if (newState == Worker.State.SUCCEEDED) {
                String title = engine.getTitle();
                if (title == null || title.isEmpty()) title = "Unknown";
                setCurrentPageTitle(title);

                // ✅ Gửi lịch sử khi load thủ công
                if (currentId != null && currentUsername != null) {
                    try {
                        Message request = new Message();
                        request.getData().put("action", "add_visit");
                        request.getData().put("user_id", currentId);
                        request.getData().put("url", url);
                        request.getData().put("title", title);
                        request.getData().put("hidden", false);
                        sendRequest(request);
                        lastSavedUrl = url;
                        System.out.println("📜 Đã lưu lịch sử truy cập (tải thủ công): " + title);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }

                // ✅ Lưu cache trang HTML chính
                try {
                    URL urlObj = new URL(finalUrl);
                    HttpURLConnection conn = (HttpURLConnection) urlObj.openConnection();

                    // 🧩 Giả lập trình duyệt Chrome
                    conn.setRequestProperty("User-Agent",
                            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
                                    "AppleWebKit/537.36 (KHTML, like Gecko) " +
                                    "Chrome/120.0.0.0 Safari/537.36");
                    conn.setRequestProperty("Accept-Encoding", "gzip, deflate");
                    conn.setRequestProperty("Cache-Control", "no-cache");
                    conn.setConnectTimeout(5000);
                    conn.setReadTimeout(5000);

                    int responseCode = conn.getResponseCode();
                    if (responseCode == HttpURLConnection.HTTP_OK) {
                        String etag = conn.getHeaderField("ETag");
                        String lastModified = conn.getHeaderField("Last-Modified");

                        // Nếu server không gửi header thì tự sinh fallback
                        if (etag == null || etag.isBlank()) {
                            etag = "auto-" + System.currentTimeMillis();
                        }
                        if (lastModified == null || lastModified.isBlank()) {
                            lastModified = new java.text.SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", java.util.Locale.US)
                                    .format(new java.util.Date());
                        }

                        System.out.println("📡 Header Info:");
                        System.out.println("   ➤ ETag: " + etag);
                        System.out.println("   ➤ Last-Modified: " + lastModified);

                        // 🧩 Lấy nội dung HTML từ WebEngine
                        String htmlContent = (String) engine.executeScript("document.documentElement.outerHTML");
                        byte[] bytes = htmlContent.getBytes(StandardCharsets.UTF_8);

                        // ✅ Lưu cache vào server
                        saveResourceCache(finalUrl, bytes, "text/html", etag, lastModified, Integer.parseInt(currentId));
                    } else {
                        System.out.println("⚠️ Server trả về mã lỗi: " + responseCode);
                    }

                    conn.disconnect();

                } catch (Exception e) {
                    System.out.println("⚠️ Không thể lấy thông tin header hoặc lưu cache cho: " + finalUrl);
                    e.printStackTrace();
                }

                manualLoad = false; // reset flag
            }

            else if (newState == Worker.State.FAILED || newState == Worker.State.CANCELLED) {
                System.out.println("⚠️ Không thể tải trang: " + url);
                Platform.runLater(() -> {
                    search.setText(url);
                    engine.loadContent(
                            "<iframe src='https://doleviettai.github.io/ViewWebError/' " +
                                    "style='width:100%;height:100%;border:none;'></iframe>"
                    );
                    setCurrentPageTitle("Không thể truy cập trang web này");
                });
                manualLoad = false;
            }
        });

        // Bắt đầu tải trang
        try {
            engine.load(url);
            try {
                Message req = new Message();
                req.getData().put("action", "get_cookies");
                req.getData().put("user_id", currentId);
                req.getData().put("host_key", URI.create(url).getHost());
                Message res = sendRequest(req);

                if ("success".equals(res.get("status"))) {
                    List<Map<String, String>> cookies = (List<Map<String, String>>) res.get("cookies");
                    for (Map<String, String> ck : cookies) {
                        String script = String.format("document.cookie='%s=%s; path=/';", ck.get("name"), ck.get("value"));
                        engine.executeScript(script);
                    }
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }

        } catch (Exception ex) {
            System.out.println("⚠️ URL load error: " + ex.getMessage());
            search.setText(url);
            engine.loadContent(
                    "<iframe src='https://doleviettai.github.io/ViewWebError/' " +
                            "style='width:100%;height:100%;border:none;'></iframe>"
            );
            setCurrentPageTitle("Không thể truy cập trang web này");
            manualLoad = false;
        }
    }


    // ============================
    // 🔧 Chuẩn hóa URL an toàn
    // ============================
    public String normalizeUrl(String url) {
        if (url == null || url.trim().isEmpty()) return "";
        url = url.trim();

        // Nếu người dùng chỉ nhập domain thì thêm https://
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "https://" + url;
        }

        // Bỏ dấu "/" cuối cùng (nếu có)
        if (url.endsWith("/") && url.length() > 8) {
            url = url.substring(0, url.length() - 1);
        }

        return url;
    }

    // ============================
    // 🔁 Theo dõi thay đổi URL (kể cả khi người dùng click trong trang)
    // ============================
    private void loadUrlWhenPathChanges() {
//        WebEngine engine = webView.getEngine();

        // Theo dõi URL thay đổi (khi click link trong trang)
        engine.locationProperty().addListener((obs, oldLoc, newLoc) -> {
            if (newLoc == null || newLoc.isEmpty()) return;
            if (!newLoc.equals(oldLoc)) {
                pendingUrl = newLoc;
                search.setText(newLoc);
                System.out.println("🌐 URL thay đổi: " + newLoc);
            }
        });

        // Khi trang tải xong sau khi thay đổi
        engine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            if (newState == Worker.State.SUCCEEDED) {
                String currentUrl = engine.getLocation();
                if (manualLoad) return; // ❌ bỏ qua load thủ công (đã gửi ở loadUrl)

                // Chỉ gửi nếu khác URL cũ
                if (currentUrl != null && !currentUrl.equals(lastSavedUrl)) {
                    String title = engine.getTitle();
                    if (title == null || title.isEmpty()) title = "Unknown";

                    if (currentId != null && currentUsername != null) {
                        try {
                            Message request = new Message();
                            request.getData().put("action", "add_visit");
                            request.getData().put("user_id", currentId);
                            request.getData().put("url", currentUrl);
                            request.getData().put("title", title);
                            request.getData().put("hidden", false);
                            sendRequest(request);
                            lastSavedUrl = currentUrl;
                            System.out.println("📜 Đã lưu lịch sử truy cập (click link): " + title);
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    }
                }
            }
        });
    }

    private void saveCookieToServer(String cookieString) {
        if (currentId == null) return; // chưa đăng nhập trình duyệt

        // cookieString ví dụ: "c_user=12345; xs=abcd; fr=xyz"
        String host = webView.getEngine().getLocation();
        String domain = URI.create(host).getHost();

        for (String part : cookieString.split(";")) {
            String[] kv = part.trim().split("=", 2);
            if (kv.length == 2) {
                try {
                    Message req = new Message();
                    req.getData().put("action", "save_cookie");
                    req.getData().put("user_id", currentId);
                    req.getData().put("host_key", domain);
                    req.getData().put("name", kv[0]);
                    req.getData().put("value", kv[1]);
                    sendRequest(req);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void saveResourceCache(String resourceUrl, byte[] content, String contentType,
                                   String etag, String lastModified, int userId) {
        try {
            if (userId == 0 || resourceUrl == null || resourceUrl.isEmpty()) return;

            Message req = new Message();
            req.getData().put("action", "save_resource_cache");
            req.getData().put("user_id", userId);
            req.getData().put("resource_url", resourceUrl);
            req.getData().put("content_type", contentType != null ? contentType : "application/octet-stream");
            req.getData().put("etag", etag);
            req.getData().put("last_modified", lastModified);

            // 👉 encode nội dung file thành base64 để gửi qua mạng
            String base64Content = Base64.getEncoder().encodeToString(content);
            req.getData().put("content", base64Content);
            req.getData().put("size", content.length);

            Message res = sendRequest(req);
            if ("success".equals(res.get("status"))) {
                System.out.println("💾 Đã lưu cache: " + resourceUrl);
            } else {
                System.out.println("⚠️ Không thể lưu cache cho: " + resourceUrl);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private void setupCookieBridge(WebEngine engine) {
        CookieBridge bridge = new CookieBridge(cookie -> {
            // Khi JS gửi cookie về
            saveCookieToServer(cookie);
        });
        JSObject window = (JSObject) engine.executeScript("window");
        window.setMember("cookieBridge", bridge);
    }


    public void toggleMenu() {
        double menuWidth = getScreenWidth() / 3.8;
        slideMenu.setPrefWidth(menuWidth);

        TranslateTransition tt = new TranslateTransition(Duration.millis(300), slideMenu);

        if (!isMenuOpen) {
            overlay.setVisible(true);

            // Menu bắt đầu ngoài phải
            slideMenu.setTranslateX(getScreenWidth());
            tt.setToX(getScreenWidth() - menuWidth); // trượt vào
            tt.play();

            isMenuOpen = true;
        } else {
            tt.setToX(getScreenWidth()); // trượt ra ngoài
            tt.setOnFinished(e -> overlay.setVisible(false));
            tt.play();

            isMenuOpen = false;
        }
    }

    /** Lấy chiều rộng scene hiện tại */
    private double getScreenWidth() {
        return stackRoot.getScene() != null ? stackRoot.getScene().getWidth() : 1200;
    }

//    =======================Xem lịch sử===============
    public void list_History() {
        // 1️⃣ Kiểm tra đăng nhập
        boolean notLoggedIn = currentId == null
                || currentUsername == null
                || currentFullname == null
                || currentFullname.trim().equalsIgnoreCase("Chưa đăng nhập");

        if (notLoggedIn) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            dl.alertDialog(alert, "Lỗi", "Người dùng chưa đăng nhập. Không thể xem lịch sử.", "thatbai");
            return;
        }

        // 2️⃣ Hiển thị giao diện lịch sử
        atHome = false;
        mainBackground.setVisible(false);
        historyBrowser.setVisible(true);
        webView.setVisible(false);

        // Hiệu ứng đóng slide menu
        TranslateTransition tt = new TranslateTransition(Duration.millis(300), slideMenu);
        tt.setToX(getScreenWidth());
        tt.setOnFinished(e -> overlay.setVisible(false));
        tt.play();
        isMenuOpen = false;

        // 3️⃣ Gửi request lấy lịch sử
        new Thread(() -> {
            try {
                // Tạo message gửi lên server
                Message req = new Message();
                req.put("action", "show_history_user");
                req.put("user_id", currentId);

                // Gửi yêu cầu và nhận phản hồi
                Message res = sendRequest(req);

                // Xử lý phản hồi
                if (res != null && "success".equals(res.get("status"))) {
                    List<Map<String, String>> data = (List<Map<String, String>>) res.get("data");

                    Platform.runLater(() -> {
                        ObservableList<HistoryItem> historyData = FXCollections.observableArrayList();
                        for (Map<String, String> row : data) {
                            int id = Integer.parseInt(row.get("id"));
                            historyData.add(new HistoryItem(id, row.get("url"), row.get("visit_time")));
                        }

                        table_history_browser.setItems(historyData);
                        url_col_table.setCellValueFactory(new PropertyValueFactory<>("url"));
                        time_col_table.setCellValueFactory(new PropertyValueFactory<>("visitTime"));
                    });


                } else {
                    String msg = (res != null) ? (String) res.get("message") : "Không có phản hồi từ server";
                    Platform.runLater(() -> {
                        Alert alert = new Alert(Alert.AlertType.ERROR);
                        dl.alertDialog(alert, "Lỗi", msg, "thatbai");
                    });
                }

            } catch (Exception ex) {
                ex.printStackTrace();
                Platform.runLater(() -> {
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    dl.alertDialog(alert, "Lỗi", "Không thể kết nối đến server: " + ex.getMessage(), "thatbai");
                });
            }
        }).start();
    }

    public void deleteSelectedHistory() {
        HistoryItem selected = table_history_browser.getSelectionModel().getSelectedItem();

        if (selected == null) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            dl.alertDialog(alert, "Cảnh báo", "Vui lòng chọn 1 mục để xóa!", "thatbai");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Xác nhận xóa");
        confirm.setHeaderText("Bạn có chắc muốn xóa lịch sử này?");
        confirm.setContentText("URL: " + selected.getUrl());
        // Thêm CSS
        confirm.getDialogPane().getStylesheets().add(
                Objects.requireNonNull(getClass().getResource("/CSS/dialog.css")).toExternalForm()
        );
        // Thêm style class, ví dụ: thành công
        confirm.getDialogPane().getStyleClass().add("thanhcong");

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isEmpty() || result.get() != ButtonType.OK) return;

        new Thread(() -> {
            try {
                Message req = new Message();
                req.put("action", "delete_history_user");
                req.put("visit_id", selected.getId()); // gửi id

                Message res = sendRequest(req);

                if (res != null && "success".equals(res.get("status"))) {
                    Platform.runLater(() -> {
                        table_history_browser.getItems().remove(selected);
                        Alert alert = new Alert(Alert.AlertType.INFORMATION);
                        dl.alertDialog(alert, "Thành công", (String) res.get("message"), "thanhcong");
                    });
                } else {
                    String msg = (res != null) ? (String) res.get("message") : "Không có phản hồi từ server";
                    Platform.runLater(() -> {
                        Alert alert = new Alert(Alert.AlertType.ERROR);
                        dl.alertDialog(alert, "Lỗi", msg, "thatbai");
                    });
                }

            } catch (Exception ex) {
                Platform.runLater(() -> {
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    dl.alertDialog(alert, "Lỗi", "Không thể kết nối đến server: " + ex.getMessage(), "thatbai");
                });
            }
        }).start();
    }


    public void refreshHistory() {
        if (currentId == null) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            dl.alertDialog(alert, "Lỗi", "Người dùng chưa đăng nhập.", "thatbai");
            return;
        }

        new Thread(() -> {
            try {
                // Tạo message gửi lên server
                Message req = new Message();
                req.put("action", "show_history_user");
                req.put("user_id", currentId);

                // Gửi yêu cầu và nhận phản hồi
                Message res = sendRequest(req);

                if (res != null && "success".equals(res.get("status"))) {
                    List<Map<String, String>> data = (List<Map<String, String>>) res.get("data");

                    Platform.runLater(() -> {
                        ObservableList<HistoryItem> historyData = FXCollections.observableArrayList();
                        for (Map<String, String> row : data) {
                            int id = Integer.parseInt(row.get("id"));
                            historyData.add(new HistoryItem(id, row.get("url"), row.get("visit_time")));
                        }

                        table_history_browser.setItems(historyData);
                        url_col_table.setCellValueFactory(new PropertyValueFactory<>("url"));
                        time_col_table.setCellValueFactory(new PropertyValueFactory<>("visitTime"));

                        // Thông báo nhỏ sau khi refresh
                        Alert alert = new Alert(Alert.AlertType.INFORMATION);
                        dl.alertDialog(alert, "Cập nhật", "Đã làm mới lịch sử duyệt web", "thanhcong");
                    });

                } else {
                    String msg = (res != null) ? (String) res.get("message") : "Không có phản hồi từ server";
                    Platform.runLater(() -> {
                        Alert alert = new Alert(Alert.AlertType.ERROR);
                        dl.alertDialog(alert, "Lỗi", msg, "thatbai");
                    });
                }

            } catch (Exception ex) {
                ex.printStackTrace();
                Platform.runLater(() -> {
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    dl.alertDialog(alert, "Lỗi", "Không thể kết nối đến server: " + ex.getMessage(), "thatbai");
                });
            }
        }).start();
    }



    //==========================login-logout================================
    public void Login_Logout() {
        boolean notLoggedIn = currentUsername == null || currentFullname == null || currentUsername.isEmpty();

        Login.setVisible(notLoggedIn);
        Logout.setVisible(!notLoggedIn);

        if (notLoggedIn) {
            Login.setOnAction(e -> openLoginForm());
        } else {
            Logout.setOnAction(e -> logout());
        }
    }

    private void openLoginForm() {
        try {
            main_browser.getScene().getWindow().hide();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/FXML/login.fxml"));
            Parent root = loader.load();

            Stage stage = new Stage();
            stage.setTitle("Đăng nhập - Night Wolf");
            stage.setScene(new Scene(root));
            stage.getIcons().add(new Image(getClass().getResourceAsStream("/Image/wolf.png")));
            stage.show();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    private void logout() {
        try {
            AutoLoginService auto = AutoLoginService.getInstance();
            String token = auto.getSessionToken();

            if (token == null || token.isEmpty()) {
                System.out.println("⚠️ Không có session token để đăng xuất.");
                return;
            }

            // Gửi yêu cầu LOGOUT tới server
            Message request = new Message();
            request.put("action", "logout");
            request.put("token", token);

            Message response = sendRequest(request);

            if (response != null && "success".equals(response.get("status"))) {
                System.out.println("🚪 Đăng xuất thành công trên server.");

                // Xóa session cục bộ
                auto.clearSession();

                // Quay lại màn hình đăng nhập
                Stage currentStage = (Stage) main_browser.getScene().getWindow();
                currentStage.close();

                FXMLLoader loader = new FXMLLoader(getClass().getResource("/FXML/login.fxml"));
                Parent root = loader.load();
                Stage stage = new Stage();
                stage.setTitle("Đăng nhập - Night Wolf");
                stage.setScene(new Scene(root));
                stage.getIcons().add(new Image(getClass().getResourceAsStream("/Image/wolf.png")));
                stage.show();

            } else {
                System.out.println("❌ Server không phản hồi hoặc trả lỗi khi logout.");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void handleSelectImage(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Chọn ảnh đại diện");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Hình ảnh", "*.png", "*.jpg", "*.jpeg")
        );

        selectedImageFile = fileChooser.showOpenDialog(null);

        if (selectedImageFile != null) {
            Image image = new Image(selectedImageFile.toURI().toString());
            image_browser.setImage(image);
            System.out.println("✅ Đã chọn ảnh: " + selectedImageFile.getName());
        } else {
            System.out.println("⚠️ Không có ảnh nào được chọn.");
        }
    }

    public void handleSaveImage(ActionEvent event) {
        if (selectedImageFile == null) {
            System.out.println("⚠️ Chưa chọn ảnh nào để lưu.");
            return;
        }

        try {
            // Đọc dữ liệu ảnh thành mảng byte
            byte[] imageBytes = Files.readAllBytes(selectedImageFile.toPath());

            // Gửi yêu cầu lên server
            Message request = new Message();
            request.put("action", "upload_profile_image");
            request.put("user_id", currentId);
            request.put("image_data", imageBytes);

            Message response = sendRequest(request);

            if (response != null && "success".equals(response.get("status"))) {
                System.out.println("✅ Ảnh đại diện đã được lưu thành công!");
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                dl.alertDialog(alert, "Thành công", (String) response.get("message"), "thanhcong");
            } else {
                System.out.println("❌ Lỗi khi lưu ảnh: " + response.get("message"));
            }

        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    // Gọi sau khi đăng nhập thành công, truyền userId hoặc Users object
    public void loadUserAvatar(String userId) {
        try {
            // Tạo request gửi server
            Message request = new Message();
            request.put("action", "get_user_avatar");
            request.put("user_id", userId);

            Message response = sendRequest(request);

            if (response != null && "success".equals(response.get("status"))) {
                byte[] avatarBytes = (byte[]) response.get("avatar");
                if (avatarBytes != null && avatarBytes.length > 0) {
                    Image avatarImage = new Image(new ByteArrayInputStream(avatarBytes));
                    image_browser.setImage(avatarImage);
                } else {
                    System.out.println("⚠️ User chưa có ảnh đại diện.");
                }
            } else {
                System.out.println("❌ Lỗi khi load ảnh: " + response.get("message"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }



    //=======================Ghim thẻ trang============================================
    public void loadUserBookmarks() {
        if (currentId == null || currentId.isEmpty()) return;

        try {
            Message req = new Message();
            req.put("action", "show_bookmark_of_user");
            req.put("user_id", currentId);

            Message res = sendRequest(req);

            if (res != null && "success".equals(res.get("status"))) {
                List<Map<String, Object>> bookmarks = (List<Map<String, Object>>) res.get("bookmarks");

                // Duyệt từng bookmark từ server
                for (Map<String, Object> bm : bookmarks) {
                    String title = bm.get("title").toString();
                    String url = bm.get("url").toString();
                    String normalizedUrl = normalizeUrl(url);

                    // 🔹 Kiểm tra trong tabBar xem đã có URL này chưa
                    boolean alreadyExists = false;
                    for (Node node : tabBar.getChildren()) {
                        if (node instanceof Button btn && btn.getUserData() != null) {
                            String existingUrl = normalizeUrl(btn.getUserData().toString());
                            if (existingUrl.equals(normalizedUrl)) {
                                alreadyExists = true;
                                break;
                            }
                        }
                    }

                    // 🔸 Nếu chưa có trong tabBar → thêm mới
                    if (!alreadyExists) {
                        addBookmarkToTabBar(title, normalizedUrl);
                    } else {
                        System.out.println("⏩ Bỏ qua bookmark trùng: " + normalizedUrl);
                    }
                }

                System.out.println("⭐ Bookmark đã load xong cho user " + currentId);
            } else {
                System.out.println("⚠️ Không thể tải bookmark của user");
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void clearBookmarksUI() {
        tabBar.getChildren().clear();
    }




    // gọi khi nhấn nút bookmark
    public void addBookmark() {
        try {
            // Kiểm tra đăng nhập
            if (currentId == null || currentId.isEmpty()) {
                Alert alert = new Alert(Alert.AlertType.WARNING);
                dl.alertDialog(alert, "Cảnh báo", "Vui lòng đăng nhập trước khi thêm bookmark!", "canhbao");
                return;
            }

            // Lấy URL hiện tại
            String currentUrl = engine.getLocation();
            if (currentUrl == null || currentUrl.isEmpty()) {
                Alert alert = new Alert(Alert.AlertType.WARNING);
                dl.alertDialog(alert, "Cảnh báo", "Không có trang web nào để lưu bookmark!", "canhbao");
                return;
            }

            // Chuẩn hóa URL
            String normalizedUrl = normalizeUrl(currentUrl);

            // Kiểm tra trùng bookmark trong tabBar
            for (Node node : tabBar.getChildren()) {
                if (node instanceof Button btn) {
                    Object userData = btn.getUserData();
                    if (userData != null && normalizeUrl(userData.toString()).equals(normalizedUrl)) {
                        Alert alert = new Alert(Alert.AlertType.INFORMATION);
                        dl.alertDialog(alert, "Thông báo", "⭐ Bookmark này đã tồn tại!", "canhbao");
                        return;
                    }
                }
            }

            // Lấy worker load trang
            Worker<Void> worker = engine.getLoadWorker();

            Runnable addBookmarkAction = () -> {
                String title = engine.getTitle();
                if (title == null || title.isEmpty()) {
                    title = normalizedUrl;
                }

                // Gửi request
                Message req = new Message();
                req.put("action", "add_bookmark");
                req.put("user_id", Integer.parseInt(currentId));
                req.put("url", normalizedUrl);
                req.put("title", title);

                Message res = null;
                try {
                    res = sendRequest(req);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                } catch (ClassNotFoundException e) {
                    throw new RuntimeException(e);
                }

                boolean ok = false;
                if (res != null) {
                    Object status = res.get("status");
                    Object success = res.get("success");
                    if ("success".equals(status) || Boolean.TRUE.equals(success)) ok = true;
                }

                if (ok) {
                    addBookmarkToTabBar(title, normalizedUrl);
                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    dl.alertDialog(alert, "Thành công", "⭐ Đã thêm bookmark thành công!", "thanhcong");
                } else {
                    String err = (res != null && res.get("message") != null)
                            ? res.get("message").toString()
                            : "Không thể thêm bookmark. Vui lòng thử lại.";
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    dl.alertDialog(alert, "Lỗi", err, "thatbai");
                }
            };

            // Nếu trang đang load → chờ xong rồi mới lấy title
            if (worker.getState() == Worker.State.RUNNING) {
                ChangeListener<Worker.State> oneShot = new ChangeListener<>() {
                    @Override
                    public void changed(ObservableValue<? extends Worker.State> obs, Worker.State oldState, Worker.State newState) {
                        if (newState == Worker.State.SUCCEEDED) {
                            worker.stateProperty().removeListener(this);
                            Platform.runLater(addBookmarkAction);
                        }
                    }
                };
                worker.stateProperty().addListener(oneShot);
            }
            // Nếu trang đã load xong → lấy title ngay
            else if (worker.getState() == Worker.State.SUCCEEDED) {
                addBookmarkAction.run();
            }
            // Trường hợp load lỗi hoặc chưa load
            else {
                Alert alert = new Alert(Alert.AlertType.WARNING);
                dl.alertDialog(alert, "Cảnh báo", "Trang chưa sẵn sàng để lưu bookmark!", "canhbao");
            }

        } catch (Exception ex) {
            ex.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR);
            dl.alertDialog(alert, "Lỗi", "Lỗi khi thêm bookmark: " + ex.getMessage(), "thatbai");
        }
    }



    private void addBookmarkToTabBar(String title, String url) {
        if (tabBar == null) return;

        // Giới hạn độ dài hiển thị của title
        String displayTitle = title;
        int maxLength = 15; // giới hạn ký tự hiển thị
        if (title.length() > maxLength) {
            displayTitle = title.substring(0, maxLength - 3) + "...";
        }

        Button bookmarkButton = new Button(displayTitle);
        bookmarkButton.getStyleClass().add("bookmark");

        // Thêm tooltip hiển thị đầy đủ title khi hover
        Tooltip tooltip = new Tooltip(title);
        tooltip.setShowDelay(Duration.millis(100));
        Tooltip.install(bookmarkButton, tooltip);

        // Hover vào
        bookmarkButton.setOnMouseEntered(e -> {
            bookmarkButton.getStyleClass().remove("bookmark-exited");
            if (!bookmarkButton.getStyleClass().contains("bookmark-enter")) {
                bookmarkButton.getStyleClass().add("bookmark-enter");
            }
        });

        // Hover ra
        bookmarkButton.setOnMouseExited(e -> {
            bookmarkButton.getStyleClass().remove("bookmark-enter");
            if (!bookmarkButton.getStyleClass().contains("bookmark-exited")) {
                bookmarkButton.getStyleClass().add("bookmark-exited");
            }
        });

        // Click trái → mở trang
        bookmarkButton.setOnAction(e -> {
            engine.load(url);
            mainBackground.setVisible(false);
            historyBrowser.setVisible(false);
            webView.setVisible(true);
            setCurrentPageTitle(title);
        });

        // Click phải → hiện menu xóa
        bookmarkButton.setOnMouseClicked(e -> {
            if (e.getButton() == MouseButton.SECONDARY) { // chuột phải
                ContextMenu menu = new ContextMenu();
                MenuItem deleteItem = new MenuItem("❌ Bỏ ghim thẻ này");

                deleteItem.setOnAction(ev -> {
                    try {
                        Message req = new Message();
                        req.put("action", "delete_bookmark");
                        req.put("user_id", currentId);
                        req.put("url", url);

                        Message res = sendRequest(req);

                        if (res != null && "success".equals(res.get("status"))) {
                            tabBar.getChildren().remove(bookmarkButton); // xóa khỏi giao diện
                            System.out.println("🗑️ Đã xóa bookmark: " + title);
                        } else {
                            System.out.println("⚠️ Không thể xóa bookmark: " + title);
                        }
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                });

                menu.getItems().add(deleteItem);
                menu.show(bookmarkButton, e.getScreenX(), e.getScreenY());
            }
        });

        int insertIndex = Math.max(0, tabBar.getChildren().size() - 1);
        tabBar.getChildren().add(insertIndex, bookmarkButton);

        // Cuộn đến cuối để thấy thẻ mới
        Platform.runLater(() -> {
            ScrollPane scrollPane = (ScrollPane) tabBar.getParent();
            scrollPane.setHvalue(1.0); // cuộn sang phải
        });
    }





    public WebEngine getEngine() {
        return engine;
    }

    public TextField getSearch() {
        return search;
    }

    public WebView getWebView() {
        return webView;
    }

//    ====================================Kiểm tra html=======================================
    // Regex để highlight HTML
    private static final Pattern HTML_PATTERN = Pattern.compile(
            "(?<TAG></?\\w+)|(?<ATTRIBUTE>\\w+)=|(?<STRING>\"[^\"]*\")"
    );
    private void setupContextMenu() {
        ContextMenu contextMenu = new ContextMenu();
        MenuItem inspectItem = new MenuItem("🧩 Kiểm tra mã HTML");
        MenuItem headItem = new MenuItem("Hiện Header / POST");
        MenuItem cookieItem = new MenuItem("🍪 Xem Cookie"); // <-- Thêm dòng này
        MenuItem cacheItem = new MenuItem("🗂️ Xem Resource Cache"); // 🆕 Thêm menu mới
        contextMenu.getItems().addAll(inspectItem , headItem , new SeparatorMenuItem(), cookieItem, cacheItem);

        inspectItem.setOnAction(e -> toggleHtmlInspector());
        headItem.setOnAction(e -> showHeadAndPostInspectorDialog());
        cookieItem.setOnAction(e -> showCookieDialog()); // <-- Gọi hàm mới
        cacheItem.setOnAction(e -> showResourceCacheDialog()); // 🆕 Gọi hàm mới

        webView.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> {
            if (event.getButton() == MouseButton.SECONDARY) {
                contextMenu.show(webView, event.getScreenX(), event.getScreenY());
            } else {
                contextMenu.hide();
                if (inspectorVisible && event.getButton() == MouseButton.PRIMARY) {
                    hideHtmlInspector();
                }
            }
        });
    }

    private void toggleHtmlInspector() {
        if (inspectorVisible) {
            hideHtmlInspector();
        } else {
            showHtmlInspector();
        }
    }

    private void showHtmlInspector() {
        try {
            String html = (String) engine.executeScript("document.documentElement.outerHTML");
            htmlCodeArea.replaceText(html);

            // Animation trượt trước
            Timeline slideUp = new Timeline(
                    new KeyFrame(Duration.millis(0),
                            new KeyValue(splitPane.getDividers().get(0).positionProperty(), 1.0)),
                    new KeyFrame(Duration.millis(300),
                            new KeyValue(splitPane.getDividers().get(0).positionProperty(), 0.3)) // inspector cao
            );
            slideUp.play();

            inspectorVisible = true;

            // Chạy highlight SAU animation (không chặn UI)
            Platform.runLater(() -> {
                htmlCodeArea.setStyleSpans(0, computeHighlighting(html));
            });

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void showHeadAndPostInspectorDialog() {
        try {
            String currentUrl = engine.getLocation();
            if (currentUrl == null || currentUrl.isEmpty()) {
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("Không có trang");
                alert.setHeaderText("Không thể kiểm tra thông tin");
                alert.setContentText("Chưa có trang web nào được tải.");
                alert.showAndWait();
                return;
            }

            // Chuẩn bị thông tin HEAD và POST
            StringBuilder sb = new StringBuilder();
            try {
                HttpURLConnection headConn = (HttpURLConnection) new URL(currentUrl).openConnection();
                headConn.setRequestMethod("HEAD");
                headConn.connect();

                sb.append("== HEAD Request Info ==\n");
                sb.append("URL: ").append(currentUrl).append("\n");
                sb.append("Status: ").append(headConn.getResponseCode())
                        .append(" ").append(headConn.getResponseMessage()).append("\n");
                sb.append("Content-Type: ").append(headConn.getContentType()).append("\n\n");

                sb.append("== Response Headers ==\n");
                headConn.getHeaderFields().forEach((key, values) -> {
                    if (key != null)
                        sb.append(key).append(": ").append(String.join(", ", values)).append("\n");
                });

                // Thử POST (nếu được)
                sb.append("\n\n== POST Request Test ==\n");
                try {
                    HttpURLConnection postConn = (HttpURLConnection) new URL(currentUrl).openConnection();
                    postConn.setRequestMethod("POST");
                    postConn.setDoOutput(true);
                    postConn.getOutputStream().write("test=data".getBytes());
                    postConn.connect();
                    sb.append("Status: ").append(postConn.getResponseCode())
                            .append(" ").append(postConn.getResponseMessage()).append("\n");
                } catch (Exception e) {
                    sb.append("POST test failed: ").append(e.getMessage()).append("\n");
                }
            } catch (Exception e) {
                sb.append("Không thể lấy HEAD/POST info: ").append(e.getMessage()).append("\n");
            }

            // Hiển thị trong dialog
            Platform.runLater(() -> {
                Dialog<Void> dialog = new Dialog<>();
                dialog.setTitle("🧩 Thông tin HEAD / POST");
                dialog.setHeaderText("Phân tích trang: " + currentUrl);
                dialog.getDialogPane().setPrefSize(800, 500);

                // 🎨 CSS Gradient cho dialog
                String gradientStyle = """
                -fx-background-color: linear-gradient(to bottom right, rgb(255,148,114), rgb(242,112,156));
                -fx-border-color: white;
                -fx-border-width: 2;
                -fx-background-radius: 15;
                -fx-border-radius: 15;
            """;
                dialog.getDialogPane().setStyle(gradientStyle);

                // Tạo TextArea hiển thị HEAD & POST info
                TextArea networkArea = new TextArea(sb.toString());
                networkArea.setEditable(false);
                networkArea.setWrapText(true);
                networkArea.setStyle("""
                -fx-font-family: Consolas;
                -fx-font-size: 13;
                -fx-control-inner-background: rgba(255,255,255,0.9);
                -fx-text-fill: black;
                -fx-background-radius: 10;
            """);

                ScrollPane scroll = new ScrollPane(networkArea);
                scroll.setFitToWidth(true);
                scroll.setFitToHeight(true);
                scroll.setStyle("-fx-background-color: transparent;");

                dialog.getDialogPane().setContent(scroll);

                ButtonType closeButton = new ButtonType("Đóng", ButtonBar.ButtonData.CANCEL_CLOSE);
                dialog.getDialogPane().getButtonTypes().add(closeButton);

                // Style nút đóng
                Button closeBtn = (Button) dialog.getDialogPane().lookupButton(closeButton);
                closeBtn.setStyle("""
                -fx-background-color: rgba(255,255,255,0.85);
                -fx-text-fill: rgb(242,112,156);
                -fx-font-weight: bold;
                -fx-background-radius: 10;
                -fx-cursor: hand;
            """);

                dialog.showAndWait();
            });

        } catch (Exception ex) {
            ex.printStackTrace();
            Platform.runLater(() -> {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Lỗi");
                alert.setHeaderText("Không thể kiểm tra HEAD/POST");
                alert.setContentText(ex.getMessage());
                alert.showAndWait();
            });
        }
    }

    private void hideHtmlInspector() {
        Timeline slideDown = new Timeline(
                new KeyFrame(Duration.millis(0),
                        new KeyValue(splitPane.getDividers().get(0).positionProperty(),
                                splitPane.getDividers().get(0).getPosition())),
                new KeyFrame(Duration.millis(300),
                        new KeyValue(splitPane.getDividers().get(0).positionProperty(), 1.0)) // trả về 100%
        );
        slideDown.play();

        inspectorVisible = false;
    }

    /**
     * Áp dụng highlight dựa trên regex HTML
     */
    private StyleSpans<? extends Collection<String>> computeHighlighting(String text) {
        Matcher matcher = HTML_PATTERN.matcher(text);
        int lastKwEnd = 0;
        StyleSpansBuilder<Collection<String>> spansBuilder = new StyleSpansBuilder<>();

        while (matcher.find()) {
            String styleClass =
                    matcher.group("TAG") != null ? "tag" :
                            matcher.group("ATTRIBUTE") != null ? "attribute" :
                                    matcher.group("STRING") != null ? "string" : null;

            assert styleClass != null;
            spansBuilder.add(java.util.Collections.emptyList(), matcher.start() - lastKwEnd);
            spansBuilder.add(java.util.Collections.singleton(styleClass), matcher.end() - matcher.start());
            lastKwEnd = matcher.end();
        }
        spansBuilder.add(java.util.Collections.emptyList(), text.length() - lastKwEnd);
        return spansBuilder.create();
    }

    private void showCookieDialog() {
        try {
            String currentUrl = engine.getLocation();
            if (currentUrl == null || currentUrl.isEmpty()) {
                Alert alert = new Alert(Alert.AlertType.WARNING, "Chưa có trang nào được tải.", ButtonType.OK);
                alert.setHeaderText("⚠️ Không thể xem cookie");
                alert.showAndWait();
                return;
            }

            String host = new URL(currentUrl).getHost();
            //int userId = 1; // 👈 Thay bằng ID user hiện tại khi có hệ thống login

            // ======= Gửi yêu cầu lấy cookie =======
            Message req = new Message();
            req.put("action", "get_cookies");
            req.put("user_id", currentId);
            req.put("host_key", host);

            Message res = sendRequest(req);
            if (!"success".equals(res.get("status"))) {
                throw new RuntimeException("Không thể tải cookie từ server");
            }

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> cookieMaps = (List<Map<String, Object>>) res.get("cookies");

            // ======= Chuyển dữ liệu sang đối tượng Cookies =======
            ObservableList<Cookies> cookieList = FXCollections.observableArrayList();
            for (Map<String, Object> map : cookieMaps) {
                Cookies ck = new Cookies(
                        null,
                        Integer.parseInt(currentId),
                        (String) map.get("host_key"),
                        (String) map.get("name"),
                        (String) map.get("value"),
                        null,
                        false,
                        (Boolean) map.get("http_only"),
                        null,
                        (Timestamp) map.get("creation_time"),
                        null,
                        false
                );
                cookieList.add(ck);
            }

            // ======= Tạo dialog =======
            Dialog<Void> dialog = new Dialog<>();
            dialog.setTitle("🍪 Trình quản lý Cookie");
            dialog.setHeaderText("Tên miền: " + host);
            // 🎨 CSS Gradient cho dialog
            String gradientStyle = """
                -fx-background-color: linear-gradient(to bottom right, rgb(255,148,114), rgb(242,112,156));
                -fx-border-color: white;
                -fx-border-width: 2;
                -fx-background-radius: 15;
                -fx-border-radius: 15;
            """;
            dialog.getDialogPane().setStyle(gradientStyle);
            dialog.getDialogPane().setPrefSize(850, 500);

            TableView<Cookies> table = new TableView<>();
            table.setItems(cookieList);

            // ======= Các cột =======
            TableColumn<Cookies, String> hostCol = new TableColumn<>("Host");
            hostCol.setCellValueFactory(new PropertyValueFactory<>("host_key"));
            hostCol.setPrefWidth(130);

            TableColumn<Cookies, String> nameCol = new TableColumn<>("Tên");
            nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
            nameCol.setPrefWidth(10);

            TableColumn<Cookies, String> valueCol = new TableColumn<>("Giá trị");
            valueCol.setCellValueFactory(new PropertyValueFactory<>("value"));
            valueCol.setPrefWidth(200);

            TableColumn<Cookies, Boolean> httpCol = new TableColumn<>("HTTP Only");
            httpCol.setCellValueFactory(new PropertyValueFactory<>("http_only"));
            httpCol.setPrefWidth(100);

            TableColumn<Cookies, Timestamp> timeCol = new TableColumn<>("Thời gian tạo");
            timeCol.setCellValueFactory(new PropertyValueFactory<>("creation_time"));
            timeCol.setPrefWidth(180);

            // ======= Cột hành động =======
            TableColumn<Cookies, Void> actionCol = new TableColumn<>("Hành động");
            actionCol.setCellFactory(col -> new TableCell<>() {
                private final Button deleteBtn = new Button("Xóa");

                {
                    deleteBtn.setOnAction(e -> {
                        Cookies item = getTableView().getItems().get(getIndex());
                        deleteCookie(Integer.parseInt(currentId), item.getHost_key(), item.getName());
                        getTableView().getItems().remove(item);
                    });
                    deleteBtn.setStyle("-fx-background-color: #ff5f6d; -fx-text-fill: white; -fx-background-radius: 5;");
                }

                @Override
                protected void updateItem(Void item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty) setGraphic(null);
                    else setGraphic(deleteBtn);
                }
            });
            actionCol.setPrefWidth(90);

            table.getColumns().addAll(hostCol, nameCol, valueCol, httpCol, timeCol, actionCol);

            // ======= Nút xóa toàn bộ =======
            Button deleteAllBtn = new Button("🧹 Xóa toàn bộ Cookie");
            deleteAllBtn.setOnAction(e -> {
                deleteAllCookies(Integer.parseInt(currentId), host);
                table.getItems().clear();
            });
            deleteAllBtn.setStyle("""
            -fx-background-color: linear-gradient(to right, #f83600, #fe8c00);
            -fx-text-fill: white;
            -fx-background-radius: 8;
            -fx-font-weight: bold;
        """);

            VBox vbox = new VBox(10, table, deleteAllBtn);
            vbox.setPadding(new Insets(10));
            dialog.getDialogPane().setContent(vbox);
            dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

            dialog.showAndWait();

        } catch (Exception ex) {
            ex.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR, ex.getMessage(), ButtonType.OK);
            alert.setHeaderText("❌ Lỗi khi xem cookie");
            alert.showAndWait();
        }
    }

    private void deleteCookie(int userId, String host, String name) {
        try {
            Message req = new Message();
            req.put("action", "delete_cookie");
            req.put("user_id", userId);
            req.put("host_key", host);
            req.put("name", name);

            Message res = sendRequest(req);
            if ("success".equals(res.get("status"))) {
                System.out.println("✅ Đã xóa cookie: " + name);
            } else {
                System.out.println("⚠️ Không thể xóa cookie: " + name);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void deleteAllCookies(int userId, String host) {
        try {
            Message req = new Message();
            req.put("action" , "delete_all_cookies_for_host");
            req.put("user_id", userId);
            req.put("host_key", host);

            Message res = sendRequest(req);
            if ("success".equals(res.get("status"))) {
                System.out.println("🧹 Đã xóa toàn bộ cookie cho host " + host);
            } else {
                System.out.println("⚠️ Không thể xóa toàn bộ cookie");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showResourceCacheDialog() {
        try {
            // 1️⃣ Lấy URL hiện tại
            String currentUrl = webView.getEngine().getLocation();
            if (currentUrl == null || currentUrl.isEmpty()) {
                new Alert(Alert.AlertType.WARNING, "Không có URL hợp lệ để xem cache.").show();
                return;
            }

            // 2️⃣ Gửi yêu cầu lấy cache
            Message req = new Message();
            req.put("action", "get_resource_cache");
            req.put("user_id", currentId);
            req.put("resource_url", currentUrl);

            Message res = sendRequest(req);
            String status = (String) res.get("status");
            if (!"success".equals(status)) {
                new Alert(Alert.AlertType.INFORMATION,
                        (String) res.getOrDefault("message", "Không thể lấy cache")).show();
                return;
            }

            // 3️⃣ Lấy danh sách cache
            List<Map<String, Object>> caches = (List<Map<String, Object>>) res.get("caches");
            if (caches == null || caches.isEmpty()) {
                new Alert(Alert.AlertType.INFORMATION, "Không có cache hợp lệ cho URL này.").show();
                return;
            }

            // 4️⃣ Map dữ liệu vào danh sách Cache model
            ObservableList<Cache> cacheList = FXCollections.observableArrayList();
            for (Map<String, Object> c : caches) {
                Cache cache = new Cache();
                cache.setId(((Number) c.get("id")).intValue());
                cache.setResourceUrl((String) c.get("resource_url"));
                cache.setEtag((String) c.get("etag"));
                cache.setContentType((String) c.get("content_type"));
                cache.setSize(((Number) c.get("size")).intValue());

                cache.setLastModified(parseTimestamp(c.get("last_modified")));
                cache.setRecvTime(parseTimestamp(c.get("recv_time")));
                cache.setExpireTime(parseTimestamp(c.get("expire_time")));
                cacheList.add(cache);
            }

            // 5️⃣ Tạo bảng TableView
            TableView<Cache> table = new TableView<>(cacheList);
            table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

            TableColumn<Cache, String> colUrl = new TableColumn<>("🌐 Resource URL");
            colUrl.setCellValueFactory(new PropertyValueFactory<>("resourceUrl"));

            TableColumn<Cache, String> colEtag = new TableColumn<>("📦 ETag");
            colEtag.setCellValueFactory(new PropertyValueFactory<>("etag"));

            TableColumn<Cache, String> colType = new TableColumn<>("📁 Content-Type");
            colType.setCellValueFactory(new PropertyValueFactory<>("contentType"));

            TableColumn<Cache, Integer> colSize = new TableColumn<>("📏 Size");
            colSize.setCellValueFactory(new PropertyValueFactory<>("size"));

            TableColumn<Cache, String> colModified = new TableColumn<>("🕓 Last Modified");
            colModified.setCellValueFactory(new PropertyValueFactory<>("lastModified"));

            TableColumn<Cache, String> colRecv = new TableColumn<>("⏰ Received");
            colRecv.setCellValueFactory(new PropertyValueFactory<>("recvTime"));

            TableColumn<Cache, String> colExpire = new TableColumn<>("🕒 Expire Time");
            colExpire.setCellValueFactory(new PropertyValueFactory<>("expireTime"));

            // 6️⃣ Cột hành động: xóa từng cache
            TableColumn<Cache, Void> colAction = new TableColumn<>("⚙️ Hành động");
            colAction.setCellFactory(param -> new TableCell<>() {
                private final Button deleteBtn = new Button("🗑️ Xóa");

                {
                    deleteBtn.setOnAction(e -> {
                        Cache cache = getTableView().getItems().get(getIndex());
                        deleteCacheById(cache.getId());
                        getTableView().getItems().remove(cache); // cập nhật UI
                    });
                    deleteBtn.getStyleClass().add("danger-btn");
                }

                @Override
                protected void updateItem(Void item, boolean empty) {
                    super.updateItem(item, empty);
                    setGraphic(empty ? null : deleteBtn);
                }
            });

            table.getColumns().addAll(colUrl, colEtag, colType, colSize, colModified, colRecv, colExpire, colAction);

            // 7️⃣ Nút xóa tất cả cache của URL hiện tại
//            Button clearAllBtn = new Button("🧹 Xóa toàn bộ cache của URL này");
//            clearAllBtn.setOnAction(e -> {
//                Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
//                        "Bạn có chắc muốn xóa toàn bộ cache cho URL này?",
//                        ButtonType.YES, ButtonType.NO);
//                confirm.setHeaderText("Xác nhận hành động");
//                confirm.showAndWait().ifPresent(btn -> {
//                    if (btn == ButtonType.YES) {
//                        deleteAllCacheForUrl(currentUrl);
//                        table.getItems().clear();
//                    }
//                });
//            });

            // 8️⃣ Hiển thị Dialog
            VBox vbox = new VBox(10, table);
            vbox.setPadding(new Insets(10));

            Dialog<Void> dialog = new Dialog<>();
            dialog.setTitle("Quản lý Resource Cache");
            dialog.getDialogPane().setContent(vbox);
            dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
            dialog.setResizable(true);
            // 🎨 CSS Gradient cho dialog
            String gradientStyle = """
                -fx-background-color: linear-gradient(to bottom right, rgb(255,148,114), rgb(242,112,156));
                -fx-border-color: white;
                -fx-border-width: 2;
                -fx-background-radius: 15;
                -fx-border-radius: 15;
            """;
            dialog.getDialogPane().setStyle(gradientStyle);
            dialog.getDialogPane().setPrefSize(950, 500);
            dialog.show();

        } catch (Exception ex) {
            ex.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Lỗi khi tải cache: " + ex.getMessage()).show();
        }
    }

    private Timestamp parseTimestamp(Object val) {
        if (val == null) return null;
        try {
            return Timestamp.valueOf(val.toString());
        } catch (Exception e) {
            return null;
        }
    }

    // 🔹 Xóa 1 cache theo id
    private void deleteCacheById(int cacheId) {
        try {
            Message req = new Message();
            req.put("action", "delete_resource_cache");
            req.put("cache_id", cacheId);

            Message res = sendRequest(req);
            String msg = (String) res.getOrDefault("message", "Không rõ kết quả");
            new Alert(Alert.AlertType.INFORMATION, msg).show();

        } catch (Exception e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Lỗi khi xóa cache: " + e.getMessage()).show();
        }
    }

    // 🔹 Xóa tất cả cache của 1 URL theo user
    private void deleteAllCacheForUrl(String resourceUrl) {
        try {
            Message req = new Message();
            req.put("action", "delete_all_resource_cache");
            req.put("user_id", currentId);
            req.put("resource_url", resourceUrl);

            Message res = sendRequest(req);
            String msg = (String) res.getOrDefault("message", "Không rõ kết quả");
            new Alert(Alert.AlertType.INFORMATION, msg).show();

        } catch (Exception e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Lỗi khi xóa toàn bộ cache: " + e.getMessage()).show();
        }
    }




    //=================================================================================
    @FXML
    private void back(ActionEvent e) {
        WebHistory history = engine.getHistory();
        if (!atHome && history.getCurrentIndex() > 0) {
            history.go(-1);
        } else if (atHome && history.getEntries().size() > 0) {
            // nếu đang ở home mà có lịch sử → bật lại webView và đi trang cuối
            atHome = false;
            mainBackground.setVisible(false);
            webView.setVisible(true);
            history.go(-1);
        } else {
            atHome = true;
            mainBackground.setVisible(true);
            historyBrowser.setVisible(false);
            webView.setVisible(false);
        }
    }

    @FXML
    private void forward(ActionEvent e) {
        WebHistory history = engine.getHistory();
        int currentIndex = history.getCurrentIndex();
        int lastIndex = history.getEntries().size() - 1;

        if (currentIndex < lastIndex) {
            // Nếu có thể tiến tới trang tiếp theo
            history.go(1);
            atHome = false;
            mainBackground.setVisible(false);
            historyBrowser.setVisible(false);
            webView.setVisible(true);
        }
        else if (atHome && lastIndex > 0) {
            // Nếu đang ở Home mà vẫn có lịch sử phía sau → bật lại webView và đi tới trang đầu tiên
            atHome = false;
            mainBackground.setVisible(false);
            historyBrowser.setVisible(false);
            webView.setVisible(true);
            history.go(1);
        }
    }


    @FXML
    private void reload(ActionEvent e) {
        engine.reload();
    }

    @FXML
    private void home(ActionEvent e) {
        // Đặt trạng thái về "trang chủ"
        atHome = true;

        // Ẩn WebView và lịch sử, chỉ hiển thị giao diện chính
        webView.setVisible(false);
        historyBrowser.setVisible(false);
        mainBackground.setVisible(true);

        // Dừng load trang (nếu đang load)
        if (engine.getLoadWorker().isRunning()) {
            engine.getLoadWorker().cancel();
        }

        // Dừng hiển thị tiến trình
        progress.progressProperty().unbind();
        progress.setProgress(0);

        // Đặt lại thanh tìm kiếm trống (cho đẹp)
        search.setText("");
    }


    @FXML
    private void zoomin() {
        if (zoomLevel < 3.0) {   // Giới hạn zoom tối đa
            zoomLevel += 0.1;
            webView.setZoom(zoomLevel);
        }
    }

    @FXML
    private void zoomout() {
        if (zoomLevel > 0.5) {   // Giới hạn zoom tối thiểu
            zoomLevel -= 0.1;
            webView.setZoom(zoomLevel);
        }
    }

    // ====================================
// ⚡ Hiển thị thông báo nổi của trình duyệt
// ====================================
    public void showBrowserAlert(String message) {
        if (browserAlert == null || alertMessage == null) return;

        Platform.runLater(() -> {
            alertMessage.setText(message);
            browserAlert.setVisible(true);
            browserAlert.setOpacity(0);

            // Vị trí ban đầu: trượt nhẹ từ dưới lên
            browserAlert.setTranslateY(30);

            // Hiệu ứng hiện ra (fade + slide)
            FadeTransition fadeIn = new FadeTransition(Duration.millis(250), browserAlert);
            fadeIn.setFromValue(0);
            fadeIn.setToValue(1);

            TranslateTransition slideUp = new TranslateTransition(Duration.millis(250), browserAlert);
            slideUp.setFromY(30);
            slideUp.setToY(0);

            ParallelTransition showAnim = new ParallelTransition(fadeIn, slideUp);
            showAnim.play();

            // Nếu đang có alert trước đó, dừng lại
            if (alertTimeline != null) alertTimeline.stop();

            // Tự động ẩn sau 3 giây
            alertTimeline = new Timeline(
                    new KeyFrame(Duration.seconds(3), e -> hideBrowserAlert())
            );
            alertTimeline.play();
        });
    }

    // ====================================
// 🕶️ Ẩn alert với hiệu ứng mờ dần + trượt xuống
// ====================================
    private void hideBrowserAlert() {
        if (browserAlert == null) return;

        FadeTransition fadeOut = new FadeTransition(Duration.millis(300), browserAlert);
        fadeOut.setFromValue(1);
        fadeOut.setToValue(0);

        TranslateTransition slideDown = new TranslateTransition(Duration.millis(300), browserAlert);
        slideDown.setFromY(0);
        slideDown.setToY(20);

        ParallelTransition hideAnim = new ParallelTransition(fadeOut, slideDown);
        hideAnim.setOnFinished(e -> {
            browserAlert.setVisible(false);
            browserAlert.setTranslateY(0);
        });
        hideAnim.play();
    }

    // ====================================
// 🧩 Khởi tạo alert (gọi 1 lần khi load giao diện)
// ====================================
    private void initializeBrowserAlert() {
        browserAlert.setVisible(false);
        browserAlert.setOpacity(0);
        alertMessage.setText("");
    }


    // responsive cho bảng ở xem lịch sử
    private void tableHistoryBroserReponsive(){
        // Khi bảng thay đổi kích thước, chia tỉ lệ cột
        table_history_browser.widthProperty().addListener((obs, oldWidth, newWidth) -> {
            double totalWidth = newWidth.doubleValue();

            // Trừ đi chiều rộng scrollbar dự kiến (khoảng 15px)
            double usableWidth = totalWidth - 15;

            time_col_table.setPrefWidth(usableWidth * 0.2);   // 20%
            url_col_table.setPrefWidth(usableWidth * 0.6);    // 60%
            action_col_table.setPrefWidth(usableWidth * 0.2); // 20%
        });
    }

    //=======================Tích hợp chatBot======================================

    /** Thiết lập socket dài hạn từ LoginController */
    public void setSocket(Socket socket, ObjectOutputStream out, ObjectInputStream in) {
        this.socket = socket;
        this.out = out;
        this.in = in;

        // Nếu userId đã có trước đó thì load conversations
        if (currentId != null) loadConversations();
    }
    /** Setter nhận userId từ LoginController */
    public void setUserId() {
        if (welcomeLabel != null) {
            welcomeLabel.setText("Welcome, User :" + currentFullname);
        }
        if (socket != null && out != null && in != null) {
            loadConversations();
        }
    }

    /** Tạo animation trượt menu */
    private void setupMenuSlide() {
        double menuWidth = menuVBox.getPrefWidth();

        // Ẩn menu sẵn
        menuContainer.setTranslateX(-menuWidth);
        menuContainer.setVisible(false);
        menuVisible = false;

        // Center chiếm toàn bộ left
        BorderPane parent = (BorderPane) centerContainer.getParent(); // dùng parent trực tiếp, tránh getScene()
        parent.setLeft(null);
        centerAnchor.setTranslateX(0);

        toggleMenuButton.setOnAction(e -> toggleMenu_1());
    }

    private void toggleMenu_1() {
        double menuWidth = menuVBox.getWidth();
        double duration = 250;

        TranslateTransition menuSlide = new TranslateTransition(Duration.millis(duration), menuContainer);
        TranslateTransition centerSlide = new TranslateTransition(Duration.millis(duration), centerAnchor);

        BorderPane parent = (BorderPane) toggleMenuButton.getScene().getRoot();

        if (!menuVisible) {
            // HIỆN MENU: trượt menu từ trái vào
            toggleMenuButton.setText("⮞");
            parent.setLeft(leftBox);
            menuContainer.setVisible(true);

            menuSlide.setFromX(-menuWidth);
            menuSlide.setToX(0);

            // Center thụt nhẹ theo menu (phần phải cố định)
            centerSlide.setFromX(0);
            centerSlide.setToX(menuWidth);

            menuSlide.setOnFinished(e -> menuContainer.setTranslateX(0));
            centerSlide.setOnFinished(e -> centerAnchor.setTranslateX(0));

            menuVisible = true;

        } else {
            // ẨN MENU: trượt menu ra trái
            toggleMenuButton.setText("☰");
            menuSlide.setFromX(0);
            menuSlide.setToX(-menuWidth);

            // Center thụt lại sang trái
            centerSlide.setFromX(0);
            centerSlide.setToX(0); // hoặc -menuWidth nếu muốn center trượt nhẹ

            menuSlide.setOnFinished(e -> {
                menuContainer.setVisible(false);
                parent.setLeft(null);
                menuContainer.setTranslateX(0);
            });

            centerSlide.setOnFinished(e -> centerAnchor.setTranslateX(0));

            menuVisible = false;
        }

        // Chạy animation song song
        menuSlide.play();
        centerSlide.play();
    }

    /**
     * ✅ Hàm mở / đóng giao diện chat (leftPane)
     */
    private void toggleInspector() {

        SplitPane.Divider divider = splitPane_chilren.getDividers().get(0);

        if (!isLeftPaneVisible) {
            // ✅ --- HIỆN LEFT PANE (CHAT) ---
            // leftPane từ 0 → 50%
            Timeline openAnim = new Timeline(
                    new KeyFrame(Duration.ZERO,
                            new KeyValue(divider.positionProperty(), divider.getPosition())),
                    new KeyFrame(Duration.millis(350),
                            new KeyValue(divider.positionProperty(), 0.35))
            );
            openAnim.play();

            toggleInspectButton.setText("⮜ Inspector");
            isLeftPaneVisible = true;
        }
        else {
            // ✅ --- ẨN LEFT PANE (CHAT) ---
            // leftPane từ 50% → 0%
            Timeline closeAnim = new Timeline(
                    new KeyFrame(Duration.ZERO,
                            new KeyValue(divider.positionProperty(), divider.getPosition())),
                    new KeyFrame(Duration.millis(350),
                            new KeyValue(divider.positionProperty(), 0)) // Inspector full
            );
            closeAnim.play();

            toggleInspectButton.setText("⮞ Inspector");
            isLeftPaneVisible = false;
        }
    }

    /** Load danh sách conversation */
    private void loadConversations() {
        if (conversationListVBox == null || socket == null || socket.isClosed()) return;
        conversationListVBox.getChildren().clear();

        Message request = new Message();
        request.put("action", "list_conversations");
        int id = Integer.parseInt(currentId); // đảm bảo là int
        request.put("user_id", id);

        new Thread(() -> {
            try {
                Message response = sendRequest(request);
                Platform.runLater(() -> {
                    if ("ok".equals(response.getOrDefault("status", ""))) {
                        List<Conversation> conversations =
                                (List<Conversation>) response.getOrDefault("conversations", List.of());
                        for (Conversation conv : conversations) {
                            // === Container chứa 2 nút ===
                            HBox container = new HBox(5);
                            container.setAlignment(Pos.CENTER_LEFT);

                            // === Nút mở hội thoại ===
                            Button openBtn = new Button(conv.getTitle());
                            openBtn.setMaxWidth(Double.MAX_VALUE);
                            HBox.setHgrow(openBtn, Priority.ALWAYS);
                            openBtn.setStyle("""
                                -fx-background-color: #f2f2f2;
                                -fx-border-color: #cccccc;
                                -fx-border-radius: 5px;
                                -fx-background-radius: 5px;
                                -fx-font-size: 14px;
                                -fx-cursor: hand;
                            """);

                            // Hiệu ứng hover cho openBtn
                            openBtn.setOnMouseEntered(e -> openBtn.setStyle("""
                                -fx-background-color: #E0FFE0;
                                -fx-border-color: #8bc34a;
                                -fx-border-radius: 5px;
                                -fx-background-radius: 5px;
                                -fx-font-size: 14px;
                                -fx-cursor: hand;
                            """));
                            openBtn.setOnMouseExited(e -> openBtn.setStyle("""
                                -fx-background-color: #f2f2f2;
                                -fx-border-color: #cccccc;
                                -fx-border-radius: 5px;
                                -fx-background-radius: 5px;
                                -fx-font-size: 14px;
                                -fx-cursor: hand;
                            """));

                            openBtn.setOnAction(e -> openConversation(conv.getId()));

                            // === Nút xóa hội thoại ===
                            Button deleteBtn = new Button("❌");
                            deleteBtn.setStyle("""
                                -fx-background-color: transparent;
                                -fx-text-fill: red;
                                -fx-font-size: 14px;
                                -fx-cursor: hand;
                            """);

                            // Hiệu ứng hover cho deleteBtn
                            deleteBtn.setOnMouseEntered(e -> deleteBtn.setStyle("""
                                -fx-background-color: #FFCCCC;
                                -fx-text-fill: red;
                                -fx-font-size: 14px;
                                -fx-cursor: hand;
                                -fx-background-radius: 5px;
                            """));
                            deleteBtn.setOnMouseExited(e -> deleteBtn.setStyle("""
                                -fx-background-color: transparent;
                                -fx-text-fill: red;
                                -fx-font-size: 14px;
                                -fx-cursor: hand;
                            """));

                            deleteBtn.setOnAction(e -> deleteItemConversation(conv.getId(), container));

                            // Thêm vào HBox
                            container.getChildren().addAll(openBtn, deleteBtn);

                            // Thêm container vào danh sách
                            conversationListVBox.getChildren().add(container);
                        }
                    } else {
                        showAlert("Error", (String) response.getOrDefault("message", "Không thể tải conversation"));
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
                showAlert("Error", "Lỗi load conversation: " + e.getMessage());
            }
        }).start();
    }

    /** Gửi yêu cầu tìm kiếm conversation tới server */
    private void performSearchConversation(String keyword) {
        if (socket == null || socket.isClosed()) return;

        Message request = new Message();
        request.put("action", "search_conversation");
        int id = Integer.parseInt(currentId); // đảm bảo là int
        request.put("user_id", id);
        request.put("keyword", keyword);

        new Thread(() -> {
            try {
                Message response = sendRequest(request);
                Platform.runLater(() -> {
                    if ("ok".equals(response.getOrDefault("status", ""))) {
                        // Xóa danh sách cũ
                        conversationListVBox.getChildren().clear();

                        List<Conversation> conversations =
                                (List<Conversation>) response.getOrDefault("conversations", List.of());

                        for (Conversation conv : conversations) {
                            // === Container chứa 2 nút ===
                            HBox container = new HBox(5);
                            container.setAlignment(Pos.CENTER_LEFT);

                            // === Nút mở hội thoại ===
                            Button openBtn = new Button(conv.getTitle());
                            openBtn.setMaxWidth(Double.MAX_VALUE);
                            HBox.setHgrow(openBtn, Priority.ALWAYS);
                            openBtn.setStyle("""
                                -fx-background-color: #f2f2f2;
                                -fx-border-color: #cccccc;
                                -fx-border-radius: 5px;
                                -fx-background-radius: 5px;
                                -fx-font-size: 14px;
                                -fx-cursor: hand;
                            """);

                            // Hiệu ứng hover cho openBtn
                            openBtn.setOnMouseEntered(e -> openBtn.setStyle("""
                                -fx-background-color: #E0FFE0;
                                -fx-border-color: #8bc34a;
                                -fx-border-radius: 5px;
                                -fx-background-radius: 5px;
                                -fx-font-size: 14px;
                                -fx-cursor: hand;
                            """));
                            openBtn.setOnMouseExited(e -> openBtn.setStyle("""
                                -fx-background-color: #f2f2f2;
                                -fx-border-color: #cccccc;
                                -fx-border-radius: 5px;
                                -fx-background-radius: 5px;
                                -fx-font-size: 14px;
                                -fx-cursor: hand;
                            """));

                            openBtn.setOnAction(e -> openConversation(conv.getId()));

                            // === Nút xóa hội thoại ===
                            Button deleteBtn = new Button("❌");
                            deleteBtn.setStyle("""
                                -fx-background-color: transparent;
                                -fx-text-fill: red;
                                -fx-font-size: 14px;
                                -fx-cursor: hand;
                            """);

                            // Hiệu ứng hover cho deleteBtn
                            deleteBtn.setOnMouseEntered(e -> deleteBtn.setStyle("""
                                -fx-background-color: #FFCCCC;
                                -fx-text-fill: red;
                                -fx-font-size: 14px;
                                -fx-cursor: hand;
                                -fx-background-radius: 5px;
                            """));
                            deleteBtn.setOnMouseExited(e -> deleteBtn.setStyle("""
                                -fx-background-color: transparent;
                                -fx-text-fill: red;
                                -fx-font-size: 14px;
                                -fx-cursor: hand;
                            """));

                            deleteBtn.setOnAction(e -> deleteItemConversation(conv.getId(), container));

                            // Thêm vào HBox
                            container.getChildren().addAll(openBtn, deleteBtn);

                            // Thêm container vào danh sách
                            conversationListVBox.getChildren().add(container);
                        }
                    } else {
                        showAlert("Error", (String) response.getOrDefault("message", "Không tìm được conversation"));
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
                showAlert("Error", "Lỗi tìm kiếm conversation: " + e.getMessage());
            }
        }).start();
    }

    /** Xóa 1 hội thoại */
    private void deleteItemConversation(int conversationId, HBox container) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Xóa hội thoại");
        confirm.setHeaderText("Bạn có chắc muốn xóa hội thoại này?");
        confirm.setContentText("Thao tác này không thể hoàn tác!");

        confirm.showAndWait().ifPresent(result -> {
            if (result == ButtonType.OK) {
                Message request = new Message();
                request.put("action", "delete_item_conversation");
                request.put("conversationId", conversationId);

                new Thread(() -> {
                    try {
                        Message response = sendRequest(request);
                        Platform.runLater(() -> {
                            if ("ok".equals(response.getOrDefault("status", ""))) {
                                conversationListVBox.getChildren().remove(container);
                                if (currentConversationId == conversationId) {
                                    chatVBox.getChildren().clear();
                                    currentConversationId = -1;
                                }
                                showInfo("Thành công", (String) response.get("message"));
                            } else {
                                showAlert("Lỗi", (String) response.getOrDefault("message", "Không thể xóa hội thoại"));
                            }
                        });
                    } catch (Exception e) {
                        e.printStackTrace();
                        showAlert("Lỗi", "Không thể gửi yêu cầu xóa: " + e.getMessage());
                    }
                }).start();
            }
        });
    }

    /**
     * Mở conversation và hiển thị tất cả tin nhắn (user + AI)
     */
    private void openConversation(int conversationId) {
        currentConversationId = conversationId;

        if (chatVBox != null) chatVBox.getChildren().clear();

        Message request = new Message();
        request.put("action", "get_history");
        request.put("conversationId", conversationId);

        new Thread(() -> {
            try {
                Message response = sendRequest(request);

                Platform.runLater(() -> {
                    if ("ok".equals(response.getOrDefault("status", ""))) {
                        List<Messages> messages =
                                (List<Messages>) response.getOrDefault("messages", List.of());

                        for (Messages msg : messages) {
                            boolean isUser = !"AI".equalsIgnoreCase(msg.getSender());

                            // Loại bỏ prefix "AI: " nếu sender là AI và có prefix
                            String content = msg.getContent();
                            if (!isUser && content != null && content.startsWith("AI: ")) {
                                content = content.substring(4).trim(); // cắt "AI: "
                                msg.setContent(content);
                            }

                            // Nếu AI chưa có content, hiện GIF
                            if (!isUser && (msg.getContent() == null || msg.getContent().isEmpty())) {
                                addChatBubbleWithGif(msg, false, "/Image/Running log.gif");
                            } else {
                                // Hiển thị text bình thường, auto wrap đầy đủ
                                addChatBubbleWithGif(msg, isUser, null);
                            }
                        }

                        // Scroll xuống cuối
                        if (chatScrollPane != null) {
                            chatScrollPane.layout();
                            chatScrollPane.setVvalue(1.0);
                        }

                    } else {
                        showAlert("Error", (String) response.getOrDefault("message", "Không lấy được lịch sử chat"));
                    }
                });

            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> showAlert("Error", "Lỗi mở conversation: " + e.getMessage()));
            }
        }).start();
    }

    /** Gửi tin nhắn và nhận AI response, hiển thị cả hai */
    @FXML
    private void handleSendMessage() {
        if (messageField == null || socket == null || socket.isClosed()) return;

        String text = messageField.getText().trim();
        if (text.isEmpty() || currentConversationId == -1) return;

        // --- User bubble ---
        Messages userMsg = new Messages(0, currentConversationId, "user", text);
        addChatBubble(userMsg, true); // TextArea cho user, không thêm "user:"

        // --- AI bubble: GIF "đang suy nghĩ" ---
        Messages aiPendingMsg = new Messages(0, currentConversationId, "ai", "");
        HBox aiContainer = addChatBubbleWithGif(aiPendingMsg, false, "/Image/Running dog.gif");

        messageField.clear();
        if (chatScrollPane != null) chatScrollPane.setVvalue(1.0);

        new Thread(() -> {
            try {
                Message request = new Message();
                request.put("action", "send_message");
                request.put("conversationId", currentConversationId);
                request.put("content", text);

                Message response = sendRequest(request);

                Platform.runLater(() -> {
                    if ("ok".equals(response.getOrDefault("status", ""))) {
                        Messages aiMsg = (Messages) response.get("aiMessage");
                        if (aiMsg != null) {
                            VBox containerVBox = (VBox) aiContainer.getChildren().get(0);
                            containerVBox.getChildren().clear(); // Xóa GIF

                            // --- Loại bỏ prefix "AI: " nếu có ---
                            String aiContent = aiMsg.getContent();
                            if (aiContent.startsWith("AI: ")) {
                                aiContent = aiContent.substring(4).trim();
                            }

                            // --- AI bubble: Label auto wrap, skyblue, full text ---
                            Label aiLabel = new Label(aiContent);
                            aiLabel.setWrapText(true);
                            aiLabel.setMaxWidth(400);
                            aiLabel.setStyle(
                                    "-fx-background-color: skyblue;" +
                                            "-fx-background-radius: 12;" +
                                            "-fx-font-size: 14px;" +
                                            "-fx-text-fill: black;" +
                                            "-fx-padding: 10;"
                            );

                            // --- Tính chiều cao tự động dựa trên nội dung ---
                            Text tempText = new Text(aiContent);
                            tempText.setFont(Font.font(14));
                            tempText.setWrappingWidth(380); // padding + border
                            double textHeight = tempText.getLayoutBounds().getHeight() + 20;
                            aiLabel.setMinHeight(textHeight);
                            aiLabel.setPrefHeight(textHeight);
                            aiLabel.setMaxHeight(textHeight);

                            containerVBox.getChildren().add(aiLabel);

                            // --- Feedback emojis ---
                            HBox reactionHBox = new HBox(5);
                            reactionHBox.setAlignment(Pos.CENTER_LEFT);
                            String[] emojis = {"👍", "❤️", "😆", "😮", "😢", "😡"};
                            String[] feedbackKeys = {"like","love","haha","wow","sad","angry"};

                            for (int i = 0; i < emojis.length; i++) {
                                String key = feedbackKeys[i];
                                Button btn = new Button(emojis[i]);
                                btn.setStyle("-fx-background-color: transparent; -fx-font-size: 16px;");

                                btn.setOnMouseEntered(e -> btn.setStyle("-fx-background-color: greenyellow; -fx-font-size: 16px;"));
                                btn.setOnMouseExited(e -> btn.setStyle("-fx-background-color: transparent; -fx-font-size: 16px;"));

                                btn.setOnAction(e -> {
                                    Node existingInput = null;
                                    for (Node node : containerVBox.getChildren()) {
                                        if ("feedbackInput".equals(node.getId())) {
                                            existingInput = node;
                                            break;
                                        }
                                    }

                                    if (existingInput != null) {
                                        containerVBox.getChildren().remove(existingInput);
                                    } else {
                                        HBox feedbackInput = showFeedbackInput(aiMsg.getId(), key);
                                        feedbackInput.setId("feedbackInput");
                                        containerVBox.getChildren().add(feedbackInput);
                                    }
                                });

                                reactionHBox.getChildren().add(btn);
                            }

                            containerVBox.getChildren().add(reactionHBox);

                            // --- Scroll xuống cuối ---
                            if (chatScrollPane != null) {
                                chatScrollPane.layout();
                                chatScrollPane.setVvalue(1.0);
                            }
                        }
                    } else {
                        showAlert("Error", (String) response.getOrDefault("message", "Không gửi được tin nhắn"));
                    }
                });

            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> showAlert("Error", "Lỗi gửi tin nhắn: " + e.getMessage()));
            }
        }).start();
    }

    /**
     * Thêm chat bubble, trả về outerContainer HBox để sau này có thể cập nhật nội dung
     */
    private HBox addChatBubble(Messages msg, boolean isUser) {

        Node contentNode;

        if (isUser) {
            // ============================
            // USER: TextArea tự tăng chiều cao
            // ============================
            TextArea textArea = new TextArea(msg.getContent());
            textArea.setWrapText(true);
            textArea.setEditable(false);
            textArea.setFocusTraversable(false);
            textArea.setMouseTransparent(false);
            textArea.setCursor(Cursor.TEXT);

            textArea.setStyle(
                    "-fx-background-color: yellowgreen;" +
                            "-fx-background-radius: 12;" +
                            "-fx-font-size: 14px;" +
                            "-fx-text-fill: black;" +
                            "-fx-border-color: transparent;" +
                            "-fx-focus-color: transparent;" +
                            "-fx-faint-focus-color: transparent;"
            );

            textArea.setMaxWidth(400);

            // Auto height
            Text tempText = new Text(msg.getContent());
            tempText.setFont(Font.font(14));
            tempText.setWrappingWidth(380);
            double height = tempText.getLayoutBounds().getHeight() + 20;

            textArea.setMinHeight(height);
            textArea.setPrefHeight(height);
            textArea.setMaxHeight(height);

            contentNode = textArea;

        } else {
            // ============================
            // AI: Dùng Label (gọn, đẹp, không scroll)
            // ============================
            Label label = new Label(msg.getContent());
            label.setWrapText(true);
            label.setMaxWidth(400);

            label.setStyle(
                    "-fx-background-color: skyblue;" +
                            "-fx-background-radius: 12;" +
                            "-fx-padding: 10;" +
                            "-fx-font-size: 14px;" +
                            "-fx-text-fill: black;"
            );

            contentNode = label;
        }

        // ============================
        // CONTAINER VBOX
        // ============================
        VBox containerVBox = new VBox();
        containerVBox.setSpacing(5);
        containerVBox.setAlignment(isUser ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
        containerVBox.getChildren().add(contentNode);

        // ============================
        // FEEDBACK CHỈ DÀNH CHO AI
        // ============================
        if (!isUser) {
            HBox reactionHBox = new HBox(5);
            reactionHBox.setAlignment(Pos.CENTER_LEFT);

            String[] emojis = {"👍", "❤️", "😆", "😮", "😢", "😡"};
            String[] feedbackKeys = {"like","love","haha","wow","sad","angry"};

            for (int i = 0; i < emojis.length; i++) {
                String key = feedbackKeys[i];

                Button btn = new Button(emojis[i]);
                btn.setStyle("-fx-background-color: transparent; -fx-font-size: 16px;");

                btn.setOnMouseEntered(e -> btn.setStyle("-fx-background-color: greenyellow; -fx-font-size: 16px;"));
                btn.setOnMouseExited(e -> btn.setStyle("-fx-background-color: transparent; -fx-font-size: 16px;"));

                btn.setOnAction(e -> {
                    Node existingInput = null;
                    for (Node node : containerVBox.getChildren()) {
                        if ("feedbackInput".equals(node.getId())) {
                            existingInput = node;
                            break;
                        }
                    }

                    if (existingInput != null) {
                        containerVBox.getChildren().remove(existingInput);
                    } else {
                        HBox feedbackInput = showFeedbackInput(msg.getId(), key);
                        feedbackInput.setId("feedbackInput");
                        containerVBox.getChildren().add(feedbackInput);
                    }
                });

                reactionHBox.getChildren().add(btn);
            }

            containerVBox.getChildren().add(reactionHBox);
        }

        // ============================
        // BỌC NGOÀI HBOX (để căn trái/phải)
        // ============================
        HBox outerContainer = new HBox(containerVBox);
        outerContainer.setPadding(new Insets(5,10,5,10));
        outerContainer.setAlignment(isUser ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);

        chatVBox.getChildren().add(outerContainer);
        if (chatScrollPane != null) chatScrollPane.setVvalue(1.0);

        return outerContainer;
    }

    private HBox addChatBubbleWithGif(Messages msg, boolean isUser, String gifPath) {
        VBox containerVBox = new VBox();
        containerVBox.setSpacing(5);
        containerVBox.setAlignment(isUser ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);

        StackPane contentContainer = new StackPane();
        contentContainer.setPadding(new Insets(10));
        contentContainer.setStyle(
                "-fx-background-color: " + (isUser ? "yellowgreen" : "skyblue") + ";" +
                        "-fx-background-radius: 12;" +
                        "-fx-border-color: transparent;"
        );

        Node contentNode;

        // ================================
        // HIỂN THỊ NỘI DUNG
        // ================================
        if (isUser || (msg.getContent() != null && !msg.getContent().isEmpty())) {

            if (isUser) {
                // ------- USER: TextArea -------
                TextArea textArea = new TextArea(msg.getContent());
                textArea.setWrapText(true);
                textArea.setEditable(false);
                textArea.setFocusTraversable(false);
                textArea.setMouseTransparent(false);
                textArea.setStyle(
                        "-fx-background-color: transparent;" +
                                "-fx-font-size: 14px;" +
                                "-fx-text-fill: black;" +
                                "-fx-border-color: transparent;"
                );
                textArea.setMaxWidth(400);

                // Auto resize height
                Text tempText = new Text(msg.getContent());
                tempText.setFont(Font.font(14));
                tempText.setWrappingWidth(380);
                double height = tempText.getLayoutBounds().getHeight() + 20;

                textArea.setMinHeight(height);
                textArea.setPrefHeight(height);
                textArea.setMaxHeight(height);

                contentNode = textArea;

            } else {
                // ------- AI: TextFlow (Auto grow height) -------
                Text text = new Text(msg.getContent());
                text.setStyle("-fx-font-size: 14px; -fx-fill: black;");

                TextFlow flow = new TextFlow(text);
                flow.setMaxWidth(400);
                flow.setPrefWidth(400);
                flow.setLineSpacing(3);

                // Cho phép TextFlow auto expand theo nội dung
                flow.setPadding(Insets.EMPTY);

                contentNode = flow;
            }

        } else {
            // ------- AI đang suy nghĩ: GIF -------
            Image gif = new Image(getClass().getResourceAsStream(gifPath));
            ImageView imageView = new ImageView(gif);
            imageView.setFitWidth(80);
            imageView.setPreserveRatio(true);
            contentNode = imageView;
        }

        contentContainer.getChildren().add(contentNode);
        containerVBox.getChildren().add(contentContainer);

        // ================================
        // FEEDBACK CHỈ CHO AI
        // ================================
        if (!isUser) {
            HBox reactionHBox = new HBox(5);
            reactionHBox.setAlignment(Pos.CENTER_LEFT);

            String[] emojis = {"👍", "❤️", "😆", "😮", "😢", "😡"};
            String[] keys = {"like","love","haha","wow","sad","angry"};

            for (int i = 0; i < emojis.length; i++) {
                String key = keys[i];
                Button btn = new Button(emojis[i]);
                btn.setStyle("-fx-background-color: transparent; -fx-font-size: 16px;");

                btn.setOnMouseEntered(e -> btn.setStyle("-fx-background-color: greenyellow; -fx-font-size: 16px;"));
                btn.setOnMouseExited(e -> btn.setStyle("-fx-background-color: transparent; -fx-font-size: 16px;"));

                btn.setOnAction(e -> {
                    Node existingInput = null;

                    for (Node node : containerVBox.getChildren()) {
                        if ("feedbackInput".equals(node.getId())) {
                            existingInput = node;
                            break;
                        }
                    }

                    if (existingInput != null) {
                        containerVBox.getChildren().remove(existingInput);
                    } else {
                        HBox feedbackInput = showFeedbackInput(msg.getId(), key);
                        feedbackInput.setId("feedbackInput");
                        containerVBox.getChildren().add(feedbackInput);
                    }
                });

                reactionHBox.getChildren().add(btn);
            }

            containerVBox.getChildren().add(reactionHBox);
        }

        // ================================
        // THÊM VÀO CHAT BOX
        // ================================
        HBox outer = new HBox(containerVBox);
        outer.setPadding(new Insets(5,10,5,10));
        outer.setAlignment(isUser ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);

        chatVBox.getChildren().add(outer);

        // Auto scroll xuống cuối
        if (chatScrollPane != null) chatScrollPane.setVvalue(1.0);

        outer.setUserData(contentContainer);
        return outer;
    }

    private HBox showFeedbackInput(int messageId, String feedbackType) {
        HBox inputRow = new HBox(5);
        inputRow.setAlignment(Pos.CENTER_LEFT);

        TextField commentField = new TextField();
        commentField.setPromptText("Ghi chú nhanh...");
        commentField.setPrefWidth(150);

        Button sendBtn = new Button("Gửi");

        sendBtn.setOnAction(ev -> {
            String comment = commentField.getText().trim();
            if (comment.isEmpty()) {
                showAlert("Lỗi", "Bạn chưa nhập phản hồi!");
                return;
            }

            Message request = new Message();
            request.put("action", "add_or_update_feedback");
            request.put("messageId", messageId);
            int id = Integer.parseInt(currentId); // đảm bảo là int
            request.put("user_id", id);
            request.put("feedback", feedbackType);
            request.put("comment", comment);

            new Thread(() -> {
                try {
                    Message response = sendRequest(request);
                    Platform.runLater(() -> {
                        if ("ok".equals(response.getOrDefault("status", ""))) {
                            showInfo("Thành công", "Đã gửi phản hồi!");
                            // remove inputRow khỏi parent
                            ((VBox) inputRow.getParent()).getChildren().remove(inputRow);
                        } else {
                            showAlert("Lỗi", (String) response.getOrDefault("message", "Không gửi được phản hồi"));
                        }
                    });
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }).start();
        });

        inputRow.getChildren().addAll(commentField, sendBtn);
        return inputRow; // chỉ trả về HBox
    }

    private void deleteFeedback(int feedbackId) {
        Message request = new Message();
        request.put("action", "delete_message_feedback");
        request.put("feedback_id", feedbackId);

        new Thread(() -> {
            try {
                Message response = sendRequest(request);
                Platform.runLater(() -> {
                    if ("ok".equals(response.get("status"))) {
                        showInfo("Đã xoá", "Feedback đã được xoá!");
                    } else {
                        showAlert("Lỗi", (String) response.get("message"));
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    /** Tạo conversation mới và mở ngay */
    @FXML
    private void handleNewConversation() {
//        if (socket == null || socket.isClosed()) return;

        // --- Hiển thị hộp thoại nhập tên ---
        TextInputDialog dialog = new TextInputDialog("New Chat");
        dialog.setTitle("Tạo hội thoại mới");
        dialog.setHeaderText("Đặt tên cho cuộc trò chuyện");
        dialog.setContentText("Nhập tên:");

        dialog.showAndWait().ifPresent(title -> {
            if (title.trim().isEmpty()) {
                showAlert("Lỗi", "Tên hội thoại không được để trống!");
                return;
            }

            Message request = new Message();
            request.put("action", "new_conversation");
            int id = Integer.parseInt(currentId); // đảm bảo là int
            request.put("user_id", id);
            request.put("title", title.trim());

            new Thread(() -> {
                try {
                    Message response = sendRequest(request);
                    Platform.runLater(() -> {
                        if ("ok".equals(response.getOrDefault("status", ""))) {
                            Conversation conv = (Conversation) response.getOrDefault("conversation", null);
                            if (conv != null) {
                                // === Container chứa 2 nút ===
                                HBox container = new HBox(5);
                                container.setAlignment(Pos.CENTER_LEFT);

                                // === Nút mở hội thoại ===
                                Button openBtn = new Button(conv.getTitle());
                                openBtn.setMaxWidth(Double.MAX_VALUE);
                                HBox.setHgrow(openBtn, Priority.ALWAYS);
                                openBtn.setStyle("""
                                -fx-background-color: #f2f2f2;
                                -fx-border-color: #cccccc;
                                -fx-border-radius: 5px;
                                -fx-background-radius: 5px;
                                -fx-font-size: 14px;
                                -fx-cursor: hand;
                            """);

                                // Hiệu ứng hover cho openBtn
                                openBtn.setOnMouseEntered(e -> openBtn.setStyle("""
                                -fx-background-color: #E0FFE0;
                                -fx-border-color: #8bc34a;
                                -fx-border-radius: 5px;
                                -fx-background-radius: 5px;
                                -fx-font-size: 14px;
                                -fx-cursor: hand;
                            """));
                                openBtn.setOnMouseExited(e -> openBtn.setStyle("""
                                -fx-background-color: #f2f2f2;
                                -fx-border-color: #cccccc;
                                -fx-border-radius: 5px;
                                -fx-background-radius: 5px;
                                -fx-font-size: 14px;
                                -fx-cursor: hand;
                            """));

                                openBtn.setOnAction(e -> openConversation(conv.getId()));

                                // === Nút xóa hội thoại ===
                                Button deleteBtn = new Button("❌");
                                deleteBtn.setStyle("""
                                -fx-background-color: transparent;
                                -fx-text-fill: red;
                                -fx-font-size: 14px;
                                -fx-cursor: hand;
                            """);

                                // Hiệu ứng hover cho deleteBtn
                                deleteBtn.setOnMouseEntered(e -> deleteBtn.setStyle("""
                                -fx-background-color: #FFCCCC;
                                -fx-text-fill: red;
                                -fx-font-size: 14px;
                                -fx-cursor: hand;
                                -fx-background-radius: 5px;
                            """));
                                deleteBtn.setOnMouseExited(e -> deleteBtn.setStyle("""
                                -fx-background-color: transparent;
                                -fx-text-fill: red;
                                -fx-font-size: 14px;
                                -fx-cursor: hand;
                            """));

                                deleteBtn.setOnAction(e -> deleteItemConversation(conv.getId(), container));

                                // Thêm vào HBox
                                container.getChildren().addAll(openBtn, deleteBtn);

                                // Thêm container vào danh sách
                                conversationListVBox.getChildren().add(container);

                                // Mở ngay conversation mới
                                openConversation(conv.getId());
                            }
                        } else {
                            showAlert("Error", (String) response.getOrDefault("message", "Không tạo được conversation"));
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                    showAlert("Error", "Lỗi tạo conversation: " + e.getMessage());
                }
            }).start();
        });
    }

    /** Hiển thị alert */
    private void showAlert(String title, String content) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle(title);
            alert.setContentText(content);
            alert.showAndWait();
        });
    }

    private void showInfo(String title, String content) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle(title);
            alert.setContentText(content);
            alert.showAndWait();
        });
    }

    /** Đóng socket khi app đóng */
    public synchronized void closeConnection() {
        try {
            if (in != null) in.close();
            if (out != null) out.close();
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException ignored) {}
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        engine = webView.getEngine();

        // --- Xử lý alert() từ JavaScript ---
        engine.setOnAlert(event -> {
            Platform.runLater(() -> {
                String message = event.getData();
                showBrowserAlert("🔔 " + message);
            });
        });

        // --- (Tuỳ chọn) Xử lý confirm() ---
        engine.setConfirmHandler(message -> {
            Platform.runLater(() -> showBrowserAlert("❓ " + message));
            return true; // giả định người dùng bấm OK
        });

        // --- (Tuỳ chọn) Xử lý prompt() ---
        engine.setPromptHandler(param -> {
            Platform.runLater(() -> showBrowserAlert("💬 " + param.getMessage()));
            return ""; // không nhập gì
        });

        loadUrlWhenPathChanges();
        tableHistoryBroserReponsive();
        TimeChay();

        // 1️⃣ Tạo và đăng ký bridge Java <-> JavaScript
        // 1️⃣ Theo dõi khi trang tải xong
        engine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            if (newState == Worker.State.SUCCEEDED) {
                try {
                    // 2️⃣ Gắn cầu nối Java ↔ JavaScript vào context của trang hiện tại
                    setupCookieBridge(engine);

                    // 3️⃣ Gọi JS lấy cookie và gửi về Java
                    engine.executeScript("cookieBridge.setCookie(document.cookie);");

                    System.out.println("✅ CookieBridge đã gắn lại và gửi cookie thành công!");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });


        // Ctrl + / Ctrl - để zoom
        webView.setOnKeyPressed((KeyEvent e) -> {
            if (e.isControlDown()) {
                if (e.getCode() == KeyCode.PLUS || e.getCode() == KeyCode.EQUALS) {
                    zoomin();
                } else if (e.getCode() == KeyCode.MINUS) {
                    zoomout();
                }
            }
        });

        // Liên kết progress bar với tiến trình tải trang
        progress.progressProperty().bind(engine.getLoadWorker().progressProperty());

        // Khi URL thay đổi → update vào thanh search
        engine.locationProperty().addListener((obs, oldLoc, newLoc) -> search.setText(newLoc));

        // Gõ Enter để load URL
        search.setOnKeyPressed((KeyEvent e) -> {
            if (e.getCode() == KeyCode.ENTER) {
                loadUrl();
            }
        });

        // Load mặc định Google
//        engine.load("https://www.google.com");
        setupContextMenu();
        initializeBrowserAlert();

        AutoLoginService autoLogin = AutoLoginService.getInstance();

        if (autoLogin.hasSession()) {
            this.currentId = String.valueOf(autoLogin.getUserId());
            this.currentUsername = autoLogin.getUsername();
            this.currentFullname = autoLogin.getFullname();

            if (username_browser != null)
                username_browser.setText(currentId + " " + currentFullname);
        }

        Login_Logout();

        // Ban đầu đặt slideMenu ngoài màn hình phải
        slideMenu.setTranslateX(getScreenWidth());
//====================================================================================
        // Lắng nghe khi text thay đổi để tìm kiếm
        setUserId();
        // Tạo PauseTransition để debounce (200ms)
        PauseTransition pause = new PauseTransition(Duration.millis(250));

        searchField.textProperty().addListener((obs, oldText, newText) -> {
            // Mỗi lần text thay đổi, reset timer
            pause.setOnFinished(event -> performSearchConversation(newText.trim()));
            pause.playFromStart();
        });

        // slide menu left
        // Delay setup để Scene đã sẵn sàng
        Platform.runLater(this::setupMenuSlide);

        Platform.runLater(() -> {
            leftPane.setMinWidth(0);
            leftPane.setPrefWidth(0);
            leftPane.setMaxWidth(Double.MAX_VALUE);

            // Ẩn hoàn toàn vùng inspector
            splitPane.setDividerPositions(1.0);

            // Cho WebView chiếm toàn bộ
            splitPane_chilren.setDividerPositions(0);
        });


        // Nếu bạn có nút toggleInspectButton để chuyển đổi hiển thị
        if (toggleInspectButton != null) {
            toggleInspectButton.setOnAction(e -> toggleInspector());
        }
    }
}
