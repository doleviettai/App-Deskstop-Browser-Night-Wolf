package org.example.prjbrowser.client;

import javafx.animation.*;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Worker;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
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
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebHistory;
import javafx.scene.web.WebView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.example.prjbrowser.client.desginer.dialog;
import org.example.prjbrowser.common.Message;
import org.example.prjbrowser.model.AutoLoginService;
import org.example.prjbrowser.model.HistoryItem;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.model.StyleSpans;
import org.fxmisc.richtext.model.StyleSpansBuilder;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.Socket;
import java.net.URL;
import java.nio.file.Files;
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
    private boolean atHome = true; // trạng thái: true = đang ở "trang chủ"

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
        String inputUrl = search.getText().trim();
        if (inputUrl.isEmpty()) return;

        final String url = normalizeUrl(inputUrl);
        atHome = false;
        mainBackground.setVisible(false);
        historyBrowser.setVisible(false);
        webView.setVisible(true);

        WebEngine engine = webView.getEngine();

        // ⚙️ Giả lập trình duyệt Chrome để tương thích tốt hơn với HTTPS
        engine.setUserAgent(
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
                        "AppleWebKit/537.36 (KHTML, like Gecko) " +
                        "Chrome/120.0.0.0 Safari/537.36"
        );

        Worker<Void> worker = engine.getLoadWorker();

        // Lắng nghe trạng thái tải trang
        ChangeListener<Worker.State> oneShot = new ChangeListener<>() {
            @Override
            public void changed(ObservableValue<? extends Worker.State> obs,
                                Worker.State oldState,
                                Worker.State newState) {
                if (newState == Worker.State.SUCCEEDED) {
                    // ✅ Load thành công
                    worker.stateProperty().removeListener(this);

                    String title = engine.getTitle();
                    if (title == null || title.isEmpty()) title = "Unknown";
                    setCurrentPageTitle(title);

                    // Gửi lưu lịch sử nếu có đăng nhập
                    if (currentId != null && currentUsername != null) {
                        try {
                            Message request = new Message();
                            request.getData().put("action", "add_visit");
                            request.getData().put("user_id", currentId);
                            request.getData().put("url", url);
                            request.getData().put("title", title);
                            request.getData().put("hidden", false);
                            sendRequest(request);
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    }
                }
                // ❌ Load thất bại → hiển thị trang lỗi
                else if (newState == Worker.State.FAILED || newState == Worker.State.CANCELLED) {
                    worker.stateProperty().removeListener(this);

                    System.out.println("⚠️ Không thể tải trang: " + url);
                    Platform.runLater(() -> {
                        // ⚠️ Giữ nguyên URL người dùng nhập trong TextField
                        search.setText(url);

                        // ⚠️ Load trang lỗi nhưng KHÔNG đổi URL trong thanh địa chỉ
                        engine.loadContent(
                                "<iframe src='https://doleviettai.github.io/ViewWebError/' " +
                                        "style='width:100%;height:100%;border:none;'></iframe>"
                        );

                        setCurrentPageTitle("Không thể truy cập trang web này");
//                        showBrowserAlert("Không thể truy cập trang web này");
                    });
                }
            }
        };

        // Gắn listener
        worker.stateProperty().addListener(oneShot);

        // Xử lý ngoại lệ khi load trang
        worker.exceptionProperty().addListener((obs, old, ex) -> {
            if (ex != null) {
                System.out.println("❌ Exception khi load trang: " + ex.getMessage());
                Platform.runLater(() -> {
                    search.setText(url);
                    engine.loadContent(
                            "<iframe src='https://doleviettai.github.io/ViewWebError/' " +
                                    "style='width:100%;height:100%;border:none;'></iframe>"
                    );
                    setCurrentPageTitle("Không thể truy cập trang web này");
                });
            }
        });

        engine.setOnError(e -> {
            System.out.println("❌ WebView Error: " + e.getMessage());
            Platform.runLater(() -> {
                search.setText(url);
                engine.loadContent(
                        "<iframe src='https://doleviettai.github.io/ViewWebError/' " +
                                "style='width:100%;height:100%;border:none;'></iframe>"
                );
                setCurrentPageTitle("Không thể truy cập trang web này");
            });
        });

        // Bắt đầu tải trang
        try {
            engine.load(url);
        } catch (Exception ex) {
            System.out.println("⚠️ URL load error: " + ex.getMessage());
            search.setText(url);
            engine.loadContent(
                    "<iframe src='https://doleviettai.github.io/ViewWebError/' " +
                            "style='width:100%;height:100%;border:none;'></iframe>"
            );
            setCurrentPageTitle("Không thể truy cập trang web này");
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
        contextMenu.getItems().addAll(inspectItem , headItem);

        inspectItem.setOnAction(e -> toggleHtmlInspector());
        headItem.setOnAction(e -> showHeadAndPostInspectorDialog());

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

            // Áp dụng highlight
            htmlCodeArea.setStyleSpans(0, computeHighlighting(html));

            // Hiện inspector bằng hiệu ứng trượt
            Timeline slideUp = new Timeline(
                    new KeyFrame(Duration.millis(0), new KeyValue(splitPane.getDividers().get(0).positionProperty(), 1.0)),
                    new KeyFrame(Duration.millis(300), new KeyValue(splitPane.getDividers().get(0).positionProperty(), 0.7))
            );
            slideUp.play();

            inspectorVisible = true;
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
                new KeyFrame(Duration.millis(0), new KeyValue(splitPane.getDividers().get(0).positionProperty(), splitPane.getDividers().get(0).getPosition())),
                new KeyFrame(Duration.millis(300), new KeyValue(splitPane.getDividers().get(0).positionProperty(), 1.0))
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

        tableHistoryBroserReponsive();
        TimeChay();

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
    }
}
