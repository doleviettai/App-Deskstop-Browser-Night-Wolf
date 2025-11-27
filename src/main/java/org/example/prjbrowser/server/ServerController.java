package org.example.prjbrowser.server;

import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.ImagePattern;
import javafx.scene.shape.Circle;
import javafx.util.Callback;
import org.example.prjbrowser.model.MessageFeedbackFromServer;
import org.example.prjbrowser.model.Urls;
import org.example.prjbrowser.model.UserSessionView;
import org.example.prjbrowser.model.database;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public class ServerController implements Initializable {

    // ================== FXML COMPONENTS ==================
    @FXML private VBox Server_Vbox;

    @FXML private Label lblServerStatus;
    @FXML private Label lblPort;
    @FXML private Label lblClients;
    @FXML private Label lblUptime;

    @FXML private Button btnStart;
    @FXML private Button btnStop;
    @FXML private Button btnClear;

    //show user
    @FXML private TableView<UserSessionView> tblUsers;

    @FXML private TableColumn<UserSessionView, String> colUsername;
    @FXML private TableColumn<UserSessionView, String> colFirst;
    @FXML private TableColumn<UserSessionView, String> colLast;
    @FXML private TableColumn<UserSessionView, String> colPhone;

    @FXML private TableColumn<UserSessionView, String> colSession;
    @FXML private TableColumn<UserSessionView, Timestamp> colCreated;
    @FXML private TableColumn<UserSessionView, Timestamp> colExpires;

    @FXML private LineChart<String, Number> lineChart;
    @FXML private ImageView imgAvatar;

    //show url
    @FXML private TableView<Urls> tblUrls;
    @FXML private TableColumn<Urls, String> colUrl;
    @FXML private TableColumn<Urls, String> colTitle;
    @FXML private TableColumn<Urls, Integer> colVisit;

    @FXML private BarChart<String, Number> urlBarChart;


    @FXML
    private Tab tabActivityLogs;

    @FXML
    private Tab tabUser;

    @FXML
    private Tab tabUrls;

    @FXML private VBox vboxUsers;
    @FXML private VBox vboxConversations;
    @FXML private VBox vboxMessages;
    @FXML private Tab tabListMessageUserWithChatBot;
    private Button selectedConversationBtn = null;

    @FXML
    private TableView<MessageFeedbackFromServer> tblFeedback;

    @FXML
    private TableColumn<MessageFeedbackFromServer, Integer> colId;

    @FXML
    private TableColumn<MessageFeedbackFromServer, String> colUsernameFB;

    @FXML
    private TableColumn<MessageFeedbackFromServer, String> colFeedback;

    @FXML
    private TableColumn<MessageFeedbackFromServer, String> colComment;

    @FXML
    private TableColumn<MessageFeedbackFromServer, Timestamp> colCreatedAt;

    @FXML private Tab tabMessageFeedback;

    @FXML
    private PieChart feedbackPieChart;


    private final int PORT = 12345;

    private ServerSocket serverSocket;
    private boolean isRunning = false;

    private Instant startTime;

    private final List<ClientHandler> clientHandlers = new ArrayList<>();
    private int clientCount = 0;


    // ======================================================
    //                  UI LOG FUNCTION
    // ======================================================
    public void addLog(String msg) {
        Platform.runLater(() -> {
            Label label = new Label(msg);
            label.setStyle("-fx-text-fill: #ADFF2F; -fx-font-family: 'Consolas'; -fx-font-size: 12;");
            Server_Vbox.getChildren().add(label);
        });
    }


    // ======================================================
    //                  START SERVER
    // ======================================================
    @FXML
    private void handleStartServer() {
        if (isRunning) {
            addLog("⚠ Server đã chạy rồi.");
            return;
        }

        new Thread(() -> {
            try {
                serverSocket = new ServerSocket(PORT);

                InetAddress ip = InetAddress.getLocalHost();

                Platform.runLater(() -> {
                    lblServerStatus.setText("● RUNNING");
                    lblServerStatus.setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold;");
                    btnStart.setDisable(true);
                    btnStop.setDisable(false);
                    lblPort.setText(String.valueOf(PORT));
                    addLog("🔥 Server START thành công!");
                    addLog("🌐 Server IP: " + ip.getHostAddress());
                });

                isRunning = true;
                startTime = Instant.now();

                updateUptimeThread();
                updateClientCount();

                while (isRunning) {
                    Socket socket = serverSocket.accept();
                    addLog("🔗 Client mới: " + socket.getInetAddress());

                    ClientHandler handler = new ClientHandler(socket, this);
                    clientHandlers.add(handler);

                    Thread clientThread = new Thread(handler);
                    clientThread.start();

                    updateClientCount();
                }

            } catch (IOException e) {
                if (isRunning) {
                    addLog("❌ Lỗi server: " + e.getMessage());
                }
            }
        }).start();
    }


    // ======================================================
    //                  STOP SERVER
    // ======================================================
    @FXML
    private void handleStopServer() {
        if (!isRunning) {
            addLog("⚠ Server chưa chạy.");
            return;
        }

        isRunning = false;

        try {
            serverSocket.close();
        } catch (IOException ignored) {}

        Platform.runLater(() -> {
            lblServerStatus.setText("● STOPPED");
            lblServerStatus.setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
            btnStart.setDisable(false);
            btnStop.setDisable(true);
            addLog("🛑 Server đã STOP.");
        });
    }


    // ======================================================
    //                  CLEAR LOGS
    // ======================================================
    @FXML
    private void handleClear() {
        Server_Vbox.getChildren().clear();
        addLog("🧹 Log đã được xóa.");
    }


    // ======================================================
    //                  REFRESH INFO
    // ======================================================
    @FXML
    private void handleRefreshServer() {
        updateClientCount();
        updateUptime();
        addLog("🔄 Refresh thông tin server.");
    }


    // ======================================================
    //                  UPTIME THREAD
    // ======================================================
    private void updateUptimeThread() {
        new Thread(() -> {
            while (isRunning) {
                Platform.runLater(this::updateUptime);
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ignored) {}
            }
        }).start();
    }

    private void updateUptime() {
        if (startTime == null) return;

        Duration uptime = Duration.between(startTime, Instant.now());
        long h = uptime.toHours();
        long m = uptime.toMinutesPart();
        long s = uptime.toSecondsPart();

        lblUptime.setText(String.format("%02d:%02d:%02d", h, m, s));
    }


    // ======================================================
    //                  UPDATE CLIENT COUNT
    // ======================================================
    // Tăng số client
    public synchronized void increaseClientCount() {
        clientCount++;
        Platform.runLater(() -> lblClients.setText(String.valueOf(clientCount)));
    }

    // Giảm số client
    public synchronized void decreaseClientCount() {
        if (clientCount > 0) clientCount--;
        Platform.runLater(() -> lblClients.setText(String.valueOf(clientCount)));
    }
    public void updateClientCount() {
//        Platform.runLater(() ->
//                lblClients.setText(String.valueOf(clientHandlers.size()))
//        );
        increaseClientCount();
        decreaseClientCount();
    }

    //=================================================================
    //===================Layout mainboard user=========================
    private void showUsers() {
        ObservableList<UserSessionView> list = FXCollections.observableArrayList();

        String sql =
                "SELECT u.id, u.username, u.firstname, u.lastname, u.password, " +
                        "u.phone_number, u.avatar, " +
                        "s.session_token, s.created_at, s.expires_at " +
                        "FROM users u " +
                        "LEFT JOIN sessions s ON u.id = s.user_id";

        try (Connection conn = database.connectDb();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {

                UserSessionView u = new UserSessionView(
                        rs.getInt("id"),
                        rs.getString("username"),
                        rs.getString("firstname"),
                        rs.getString("lastname"),
                        rs.getString("password"),
                        rs.getInt("phone_number"),
                        rs.getBytes("avatar"),
                        rs.getString("session_token"),
                        rs.getTimestamp("created_at"),
                        rs.getTimestamp("expires_at")
                );

                list.add(u);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        tblUsers.setItems(list);
    }
    private void showInTableUser() {

        colUsername.setCellValueFactory(new PropertyValueFactory<>("username"));
        colFirst.setCellValueFactory(new PropertyValueFactory<>("firstname"));
        colLast.setCellValueFactory(new PropertyValueFactory<>("lastname"));
        colPhone.setCellValueFactory(new PropertyValueFactory<>("phone_number"));

        colSession.setCellValueFactory(new PropertyValueFactory<>("sessionToken"));
        colCreated.setCellValueFactory(new PropertyValueFactory<>("createdAt"));
        colExpires.setCellValueFactory(new PropertyValueFactory<>("expiresAt"));

        // Load data
        showUsers();

        // Click row để hiện avatar
        tblUsers.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && newVal.getAvatar() != null) {
                Image image = new Image(new ByteArrayInputStream(newVal.getAvatar()));
                imgAvatar.setImage(image);
            }
        });
    }
    private void loadUserCreateChart() {

        XYChart.Series<String, Number> series = new XYChart.Series<>();

        String sql = "SELECT DATE(created_at) AS created_day, COUNT(*) AS total " +
                "FROM users GROUP BY DATE(created_at) ORDER BY created_day ASC";

        try (Connection conn = database.connectDb();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {

                String date = rs.getString("created_day");
                int count = rs.getInt("total");

                series.getData().add(new XYChart.Data<>(date, count));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        lineChart.getData().clear();     // clear old
        lineChart.getData().add(series); // add new
    }

    //=====================================================================
    //===================Layout mainboar đường dẫn=========================
    private ObservableList<Urls> loadUrls() {

        ObservableList<Urls> list = FXCollections.observableArrayList();

        String sql = "SELECT id, url, title, visit_count, typed_count, hidden FROM urls ORDER BY visit_count DESC";

        try (Connection conn = database.connectDb();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                Urls u = new Urls(
                        rs.getBoolean("hidden"),
                        rs.getInt("id"),
                        rs.getString("title"),
                        rs.getInt("typed_count"),
                        rs.getString("url"),
                        rs.getInt("visit_count")
                );
                list.add(u);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return list;
    }

    private void initUrlColumns() {
        colUrl.setCellValueFactory(new PropertyValueFactory<>("url"));
        colTitle.setCellValueFactory(new PropertyValueFactory<>("title"));
        colVisit.setCellValueFactory(new PropertyValueFactory<>("visit_count"));
    }

    private void loadUrlBarChart() {
        urlBarChart.getData().clear();

        XYChart.Series<String, Number> series = new XYChart.Series<>();

        String sql =
                "SELECT url, visit_count " +
                        "FROM urls " +
                        "ORDER BY visit_count DESC " +
                        "LIMIT 15";

        try (Connection conn = database.connectDb();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {

                String fullUrl = rs.getString("url");
                int visits = rs.getInt("visit_count");

                String domain = extractDomain(fullUrl); // rút gọn URL

                XYChart.Data<String, Number> data = new XYChart.Data<>(domain, visits);
                series.getData().add(data);

                // ⭐ FIX: Tooltip phải cài khi node đã được tạo
                data.nodeProperty().addListener((obs, oldNode, newNode) -> {
                    if (newNode != null) {
                        Tooltip tooltip = new Tooltip(fullUrl + "\nVisits: " + visits);
                        Tooltip.install(newNode, tooltip);
                    }
                });
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        urlBarChart.getData().add(series);
    }

    private String extractDomain(String url) {
        try {
            String clean = url.replaceFirst("https?://", "")
                    .replace("www.", "");

            // Cắt theo "/" để chỉ lấy domain
//            int slash = clean.indexOf("/");
//            if (slash > 0) {
//                clean = clean.substring(0, slash);
//            }

            // 🔥 Giới hạn độ dài domain
            int maxLen = 20;
            if (clean.length() > maxLen) {
                return clean.substring(0, maxLen) + "...";
            }

            return clean;

        } catch (Exception e) {
            return url;
        }
    }

    //======================================================
    //=============list message user and chatbot============
    //======================================================
    private void loadUsers() {
        vboxUsers.getChildren().clear();

        String sql = "SELECT id, username FROM users ORDER BY created_at DESC";

        try (Connection conn = database.connectDb();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                int userId = rs.getInt("id");
                String username = rs.getString("username");

                Button btn = new Button(username);
                btn.setMaxWidth(Double.MAX_VALUE);
                btn.getStyleClass().add("btn-user");

                btn.setOnAction(e -> loadConversations(userId));

                vboxUsers.getChildren().add(btn);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadConversations(int userId) {
        vboxConversations.getChildren().clear();
        vboxMessages.getChildren().clear();

        String sql = "SELECT id, title FROM conversations WHERE user_id = ? ORDER BY updated_at DESC";

        try (Connection conn = database.connectDb();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                int conversationId = rs.getInt("id");
                String title = rs.getString("title");

                Button btn = new Button(title);
                btn.setMaxWidth(Double.MAX_VALUE);
                btn.getStyleClass().add("btn-conversation");

                btn.setOnAction(e -> {

                    // Remove highlight old
                    if (selectedConversationBtn != null)
                        selectedConversationBtn.getStyleClass().remove("selected-conversation");

                    // Apply highlight
                    btn.getStyleClass().add("selected-conversation");
                    selectedConversationBtn = btn;

                    loadMessages(conversationId);
                });

                vboxConversations.getChildren().add(btn);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadMessages(int conversationId) {
        vboxMessages.getChildren().clear();

        String sql = "SELECT sender, content, created_at FROM messages WHERE conversation_id = ? ORDER BY created_at ASC";

        try (Connection conn = database.connectDb();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, conversationId);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                String sender = rs.getString("sender");
                String content = rs.getString("content");

                HBox row = new HBox();
                row.setSpacing(10);

                // ====== Tin nhắn ======
                Label bubble = new Label();
                bubble.setText(content);          // set text đầy đủ
                bubble.setWrapText(true);         // wrap xuống dòng
                bubble.setMaxWidth(Double.MAX_VALUE);  // không giới hạn width cố định
                bubble.setStyle(
                        sender.equals("user")
                                ? "-fx-background-color: #4caf50; -fx-padding: 10; -fx-text-fill: white; -fx-background-radius: 10;"
                                : "-fx-background-color: #2196F3; -fx-padding: 10; -fx-text-fill: white; -fx-background-radius: 10;"
                );

                // ====== Avatar ======
                Circle avatar = new Circle(18);
                String imgPath = sender.equals("user")
                        ? "/image/user.png"
                        : "/image/bot.png";
                avatar.setFill(new ImagePattern(new Image(
                        getClass().getResourceAsStream(imgPath)
                )));

                if (sender.equals("user")) {
                    row.getChildren().addAll(avatar, bubble);
                } else {
                    row.getChildren().addAll(bubble, avatar);
                    row.setAlignment(Pos.CENTER_RIGHT);
                }

                // ====== Fade animation ======
                Duration javaDuration = Duration.ofMillis(250);
                javafx.util.Duration fxDuration = javafx.util.Duration.millis(javaDuration.toMillis());
                FadeTransition fade = new FadeTransition(fxDuration, row);
                fade.setFromValue(0);
                fade.setToValue(1);
                fade.play();

                // ====== Thêm row vào VBox ======
                vboxMessages.getChildren().add(row);
            }

            // ====== Thêm để Label mở rộng theo width VBox ======
            vboxMessages.widthProperty().addListener((obs, oldVal, newVal) -> {
                for (Node node : vboxMessages.getChildren()) {
                    if (node instanceof HBox hbox) {
                        for (Node child : hbox.getChildren()) {
                            if (child instanceof Label lbl) {
                                lbl.setMaxWidth(newVal.doubleValue() - 50); // trừ padding và avatar
                            }
                        }
                    }
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    //==============Message FeedBack=======================
    private void initColumns() {
        colId.setCellValueFactory(cellData -> cellData.getValue().idProperty().asObject());
        colUsernameFB.setCellValueFactory(cellData -> cellData.getValue().usernameProperty());
        colFeedback.setCellValueFactory(cellData -> cellData.getValue().feedbackProperty());
        colComment.setCellValueFactory(cellData -> cellData.getValue().commentProperty());
        colCreatedAt.setCellValueFactory(cellData -> cellData.getValue().createdAtProperty());

        // Tô màu cột feedback
        colFeedback.setCellFactory(param -> new TableCell<>() {
            @Override
            protected void updateItem(String feedback, boolean empty) {
                super.updateItem(feedback, empty);
                if (empty || feedback == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(feedback.toUpperCase());
                    switch (feedback) {
                        case "like" -> setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-alignment: CENTER;");
                        case "dislike" -> setStyle("-fx-background-color: #F44336; -fx-text-fill: white; -fx-alignment: CENTER;");
                        case "love" -> setStyle("-fx-background-color: #E91E63; -fx-text-fill: white; -fx-alignment: CENTER;");
                        case "haha" -> setStyle("-fx-background-color: #FFEB3B; -fx-text-fill: black; -fx-alignment: CENTER;");
                        case "wow" -> setStyle("-fx-background-color: #FF9800; -fx-text-fill: white; -fx-alignment: CENTER;");
                        case "sad" -> setStyle("-fx-background-color: #2196F3; -fx-text-fill: white; -fx-alignment: CENTER;");
                        case "angry" -> setStyle("-fx-background-color: #9C27B0; -fx-text-fill: white; -fx-alignment: CENTER;");
                        default -> setStyle("");
                    }
                }
            }
        });

        // Định dạng cột thời gian
        colCreatedAt.setCellFactory(column -> new TableCell<MessageFeedbackFromServer, Timestamp>() {
            private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

            @Override
            protected void updateItem(Timestamp item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.toLocalDateTime().format(formatter));
                }
            }
        });
    }

    private void loadFeedbacks() {
        ObservableList<MessageFeedbackFromServer> list = FXCollections.observableArrayList();

        String sql = """
                SELECT mf.id, u.username, mf.feedback, mf.comment, mf.created_at
                FROM message_feedback mf
                JOIN users u ON mf.user_id = u.id
                ORDER BY mf.created_at DESC
                """;

        try (Connection conn = database.connectDb();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                int id = rs.getInt("id");
                String username = rs.getString("username");
                String feedback = rs.getString("feedback");
                String comment = rs.getString("comment");
                Timestamp createdAt = rs.getTimestamp("created_at");

                list.add(new MessageFeedbackFromServer(id, username, feedback, comment, createdAt));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        tblFeedback.setItems(list);
    }


    private void loadFeedbackPieChart() {
        feedbackPieChart.getData().clear();

        String sql = "SELECT feedback, COUNT(*) as count " +
                "FROM message_feedback " +
                "GROUP BY feedback";

        try (Connection conn = database.connectDb();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                String feedback = rs.getString("feedback");
                int count = rs.getInt("count");

                PieChart.Data slice = new PieChart.Data(feedback + " (" + count + ")", count);
                feedbackPieChart.getData().add(slice);

                // Thêm tooltip hiển thị số lượng
                Tooltip tooltip = new Tooltip(feedback + ": " + count);
                Tooltip.install(slice.getNode(), tooltip);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        // Đặt legend sang bên trái để các giá trị hiển thị theo “chiều ngang” của bố cục
        feedbackPieChart.setLegendSide(Side.LEFT);
        feedbackPieChart.setLabelsVisible(false); // ẩn label mặc định trên slice
    }





    // ======================================================
    //                  INITIALIZE
    // ======================================================
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        lblPort.setText(String.valueOf(PORT));

        btnStop.setDisable(true);  // server chưa start khi mở UI
        lblServerStatus.setText("● STOPPED");
        lblServerStatus.setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");

//        showInTableUser();
//        // load biểu đồ
//        loadUserCreateChart();

        tabUser.setOnSelectionChanged(event -> {
            if (tabUser.isSelected()) {
                showInTableUser();
                loadUserCreateChart();
            }
        });

        tabUrls.setOnSelectionChanged(e -> {
            if (tabUrls.isSelected()) {

                initUrlColumns();

                ObservableList<Urls> list = loadUrls();
                tblUrls.setItems(list);

                loadUrlBarChart();   // ⭐ BarChart load lại khi mở tab
            }
        });

        tabListMessageUserWithChatBot.setOnSelectionChanged(e -> {
            if (tabListMessageUserWithChatBot.isSelected()) {
                loadUsers();
            }
        });

        tabMessageFeedback.setOnSelectionChanged(e->{
            if(tabMessageFeedback.isSelected()){
                initColumns();
                loadFeedbacks();
                loadFeedbackPieChart();
            }
        });


        addLog("👌 Server GUI đã load xong.");
    }
}
