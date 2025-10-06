package org.example.prjbrowser.client;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Worker;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebHistory;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.example.prjbrowser.client.desginer.dialog;
import org.example.prjbrowser.common.Message;

import java.io.*;
import java.net.Socket;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.ResourceBundle;

public class NewTabController implements Initializable {

    @FXML
    public TextField search;

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
    private TableView<?> table_history_browser;

    @FXML
    private TableColumn<?, ?> time_col_table;

    @FXML
    private TableColumn<?, ?> url_col_table;

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
    private Label Date_label;
    @FXML
    private Label Time_label;

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

        // gọi cập nhật nút login/logout
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

        if (!inputUrl.startsWith("http")) {
            inputUrl = "https://" + inputUrl;
        }

        final String url = normalizeUrl(inputUrl); // final để capture an toàn
        atHome = false;
        mainBackground.setVisible(false);
        historyBrowser.setVisible(false);
        webView.setVisible(true);

        Worker<Void> worker = engine.getLoadWorker();

        // One-shot listener: sẽ tự remove khi SUCCEEDED
        ChangeListener<Worker.State> oneShot = new ChangeListener<>() {
            @Override
            public void changed(ObservableValue<? extends Worker.State> obs, Worker.State oldState, Worker.State newState) {
                if (newState == Worker.State.SUCCEEDED) {
                    // remove listener ngay để tránh gọi nhiều lần
                    worker.stateProperty().removeListener(this);

                    // Lấy title ổn định
                    String title = engine.getTitle();
                    if (title == null || title.isEmpty()) title = "Unknown";
                    setCurrentPageTitle(title);

                    // Nếu đã login thì gửi lưu lịch sử
                    if (currentId != null && currentUsername != null) {
                        try {
                            Message request = new Message();
                            request.getData().put("action", "add_visit");
                            request.getData().put("user_id", currentId);
                            request.getData().put("url", url);
                            request.getData().put("title", title);
                            request.getData().put("hidden", false);

                            // gửi đến server (blocking) — bạn có thể gửi async nếu muốn
                            sendRequest(request);
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    }
                }
            }
        };

        // Đăng ký listener 1 lần trước khi load
        worker.stateProperty().addListener(oneShot);

        // Bắt đầu load
        engine.load(url);
    }

    private String normalizeUrl(String url) {
        if (url == null) return "";
        url = url.trim().toLowerCase();

        // Thêm https nếu thiếu
        if (!url.startsWith("http")) {
            url = "https://" + url;
        }

        // Bỏ slash cuối
        if (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }

        // Bỏ www.
        url = url.replace("www.", "");
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

    public void list_History() {
        // Kiểm tra đăng nhập chính xác hơn
        boolean notLoggedIn = currentId == null
                || currentUsername == null
                || currentFullname == null
                || currentFullname.trim().equalsIgnoreCase("Chưa đăng nhập");

        if (notLoggedIn) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            dl.alertDialog(alert, "Lỗi", "Người dùng chưa đăng nhập. Không thể xem lịch sử.", "thatbai");
            return; // Dừng hàm, không cho chuyển sang giao diện lịch sử
        }

        // Nếu đã đăng nhập, hiển thị giao diện lịch sử
        atHome = false;
        mainBackground.setVisible(false);
        historyBrowser.setVisible(true);
        webView.setVisible(false);

        // Hiệu ứng đóng slide menu
        TranslateTransition tt = new TranslateTransition(Duration.millis(300), slideMenu);
        tt.setToX(getScreenWidth()); // Trượt ra ngoài
        tt.setOnFinished(e -> overlay.setVisible(false));
        tt.play();

        isMenuOpen = false;
    }


    public void Login_Logout(){
        if (currentUsername == null || currentFullname == null) {
            // Chưa login → hiện nút đăng nhập
            Login.setVisible(true);
            Logout.setVisible(false);

            Login.setOnAction(e -> {
                try {
                    // mở form login
                    main_browser.getScene().getWindow().hide();
                    FXMLLoader loader = new FXMLLoader(getClass().getResource("/FXML/login.fxml"));
                    Parent root = loader.load();

                    Stage stage = new Stage();
                    stage.setTitle("Đăng nhập");
                    stage.setScene(new Scene(root));

                    stage.setTitle("Night Wolf");
                    Image icon = new Image(getClass().getResourceAsStream("/Image/wolf.png"));
                    stage.getIcons().add(icon);
                    stage.show();

                    // sau khi login thành công, bạn sẽ nhận username + fullname từ LoginController
                    // rồi gọi lại Login_Logout() để cập nhật nút
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            });
        } else {
            // Đã login → hiện nút đăng xuất
            Login.setVisible(false);
            Logout.setVisible(true);

            Logout.setOnAction(e -> {
                // clear thông tin user
                currentUsername = null;
                currentFullname = null;

                try {
                    // mở form login
                    main_browser.getScene().getWindow().hide();
                    FXMLLoader loader = new FXMLLoader(getClass().getResource("/FXML/login.fxml"));
                    Parent root = loader.load();

                    Stage stage = new Stage();
                    stage.setTitle("Đăng nhập");
                    stage.setScene(new Scene(root));
                    stage.show();

                    // sau khi login thành công, bạn sẽ nhận username + fullname từ LoginController
                    // rồi gọi lại Login_Logout() để cập nhật nút
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            });
        }
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

        // Ban đầu đặt slideMenu ngoài màn hình phải
        slideMenu.setTranslateX(getScreenWidth());
    }
}
