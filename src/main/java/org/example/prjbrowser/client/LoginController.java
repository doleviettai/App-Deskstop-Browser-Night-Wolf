package org.example.prjbrowser.client;

import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import org.example.prjbrowser.client.desginer.dialog;
import org.example.prjbrowser.common.Message;
import org.example.prjbrowser.model.AutoLoginService;
import org.example.prjbrowser.model.Jbcrypt;
import org.example.prjbrowser.model.database;

import java.awt.*;
import java.io.*;
import java.net.Socket;
import java.net.URI;
import java.net.URL;
import java.sql.*;
import java.util.Date;
import java.util.Objects;
import java.util.Properties;
import java.util.ResourceBundle;

public class LoginController implements Initializable {
    @FXML
    private Button back_login;

    @FXML
    private Button forgot_kiemtra;

    @FXML
    private Button forgot_kiemtra_1;

    @FXML
    private Hyperlink forgot_password;

    @FXML
    private AnchorPane forgotpassword_form;

    @FXML
    private AnchorPane forgotpassword_form_1;

    @FXML
    private AnchorPane login;

    @FXML
    private Button login_btn;

    @FXML
    private AnchorPane login_form;

    @FXML
    private PasswordField password;
    String pass = String.valueOf(password);

    @FXML
    private TextField password_res;

    @FXML
    private TextField firstname_res;

    @FXML
    private TextField lastname_res;

    @FXML
    private TextField phonenumber_kiemtra;

    @FXML
    private TextField phonenumber_res;

    @FXML
    private Button register_btn;

    @FXML
    private AnchorPane register_form;

    @FXML
    private Button side_create_btn;

    @FXML
    private Button side_create_btn1_login;

    @FXML
    private Button side_create_btn_login;

    @FXML
    private AnchorPane silde_form;
    @FXML
    private TextField username_kiemtra;
    @FXML
    private TextField makhau_kiemtra_1;
    @FXML
    private PasswordField matkhau_kiemtra_1_1;
    @FXML
    private TextField username;
    @FXML
    private TextField username_res;

    @FXML
    private PasswordField xacnhanmatkhau_res;
    @FXML
    private TextField password_show;
    @FXML
    private CheckBox check;

    private Connection connect;
    private PreparedStatement prepare;
    private ResultSet resultSet;
    private Alert alert;
    private Statement statement;
    private double x = 0;
    private double y = 0;

    private dialog dl = new dialog();
    private final AutoLoginService Als = AutoLoginService.getInstance();

    private static final String SERVER_HOST = "localhost";
//    private static final String SERVER_HOST = "172.20.10.2";
//    private static final String SERVER_HOST = "192.168.56.1";
    private static final int SERVER_PORT = 12345;

    // Socket dài hạn cho toàn bộ app
    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;


    @FXML
    private void onMousePressed(MouseEvent event) {
        x = event.getSceneX();
        y = event.getSceneY();
    }

    @FXML
    private void onMouseDragged(MouseEvent event) {
        Stage stage = (Stage) login.getScene().getWindow();
        stage.setX(event.getScreenX() - x);
        stage.setY(event.getScreenY() - y);
    }

    public void switchFor(ActionEvent e) {
        TranslateTransition slider = new TranslateTransition();
        if (e.getSource() == side_create_btn) {
            slider.setNode(silde_form);
            slider.setToX(300);
            slider.setDuration(Duration.seconds(.5));
            slider.setOnFinished((ActionEvent ev) -> {
                side_create_btn_login.setVisible(true);
                side_create_btn.setVisible(false);
            });
            register_form.setVisible(true);
            forgotpassword_form.setVisible(false);
            forgotpassword_form_1.setVisible(false);
            slider.play();
        } else if (e.getSource() == side_create_btn_login) {
            slider.setNode(silde_form);
            slider.setToX(0);
            slider.setDuration(Duration.seconds(.5));
            slider.setOnFinished((ActionEvent ev) -> {
                side_create_btn_login.setVisible(false);
                side_create_btn.setVisible(true);
            });
            slider.play();
        }
    }

    public void switchHyper(ActionEvent e) {
        TranslateTransition slider = new TranslateTransition();
        if (e.getSource() == forgot_password) {
            slider.setNode(silde_form);
            slider.setToX(300);
            slider.setDuration(Duration.seconds(.5));
            slider.setOnFinished((ActionEvent ev) -> {
                side_create_btn_login.setVisible(true);
                //side_create_btn1_login.setVisible(false);
            });
            register_form.setVisible(false);
            forgotpassword_form.setVisible(true);
            forgotpassword_form_1.setVisible(false);
            slider.play();
        }
    }

//    private Message sendRequest(Message request) throws IOException, ClassNotFoundException {
//        Socket socket = new Socket("localhost", 12345);
////        Socket socket = new Socket("172.20.10.2", 12345);
////        Socket socket = new Socket("192.168.56.1", 12345);
//        ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
//        ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
//
//        out.writeObject(request);
//        out.flush();
//
//        Message response = (Message) in.readObject();
//
//        in.close();
//        out.close();
//        socket.close();
//
//        return response;
//    }

    /** Kết nối server lâu dài, nếu đã kết nối thì không tạo lại */
    private synchronized void connectToServer() throws IOException {
        if (socket != null && !socket.isClosed()) return; // đã có kết nối
        socket = new Socket(SERVER_HOST, SERVER_PORT);
        out = new ObjectOutputStream(socket.getOutputStream());
        in = new ObjectInputStream(socket.getInputStream());
    }

    /** Gửi request đến server qua socket dài hạn */
    private synchronized Message sendRequest(Message request) throws IOException, ClassNotFoundException {
        if (socket == null || socket.isClosed()) {
            connectToServer(); // đảm bảo socket luôn sẵn sàng
        }

        // Gửi request
        out.writeObject(request);
        out.flush();

        // Nhận response
        Message response = (Message) in.readObject();

        return response;
    }


    // ================== FORGOT CHECK ==================
    public void forgortXacthuc() {
        try {
            Message request = new Message();
            request.put("action", "forgot_check");
            request.put("username", username_kiemtra.getText());
            request.put("phone_number", phonenumber_kiemtra.getText());

            Message response = sendRequest(request);

            if ("success".equals(response.get("status"))) {
                alert = new Alert(Alert.AlertType.INFORMATION);
                dl.alertDialog(alert , "Thành công" , (String) response.get("message") , "thanhcong");

                forgotpassword_form.setVisible(false);
                forgotpassword_form_1.setVisible(true);
                register_form.setVisible(false);
            } else {
                alert = new Alert(Alert.AlertType.ERROR);
                dl.alertDialog(alert , "Lỗi" , (String) response.get("message") , "thatbai");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ================== FORGOT UPDATE PASSWORD ==================
    public void forgortXacnhanthaydoimatkhau() {
        try {
            String passPlain = makhau_kiemtra_1.getText();       // mật khẩu gốc
            String passConfirm = matkhau_kiemtra_1_1.getText();  // mật khẩu để mã hóa

            Message request = new Message();
            request.put("action", "forgot_update");
            request.put("username", username_kiemtra.getText());
            request.put("password", passPlain);   // plain text
            request.put("confirm_password", passConfirm); // để server hash

            Message response = sendRequest(request);

            // kiểm tra 2 mật khẩu nhập lại có giống nhau không
            if (!passPlain.equals(passConfirm)) {
                alert = new Alert(Alert.AlertType.ERROR);
                dl.alertDialog(alert , "Lỗi" , (String) response.get("message") , "thatbai");
                return;
            }

            if ("success".equals(response.get("status"))) {
                alert = new Alert(Alert.AlertType.INFORMATION);
                dl.alertDialog(alert , "Thành công" , (String) response.get("message") , "thanhcong");

                username_kiemtra.setText("");
                phonenumber_kiemtra.setText("");
                matkhau_kiemtra_1_1.setText("");
                makhau_kiemtra_1.setText("");
            } else {
                alert = new Alert(Alert.AlertType.ERROR);
                dl.alertDialog(alert , "Lỗi" , (String) response.get("message") , "thatbai");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    // ================== REGISTER ==================
    public void regBtn() {
        try {
            Message request = new Message();
            request.put("action", "register");
            request.put("username", username_res.getText());
            request.put("firstname" , firstname_res.getText());
            request.put("lastname" , lastname_res.getText());
            request.put("password", password_res.getText());
            request.put("confirm_password", xacnhanmatkhau_res.getText());
            request.put("phone_number", phonenumber_res.getText());

            Message response = sendRequest(request);

            if ("success".equals(response.get("status"))) {
                alert = new Alert(Alert.AlertType.INFORMATION);
                dl.alertDialog(alert , "Thành công" , (String) response.get("message") , "thanhcong");
            } else {
                alert = new Alert(Alert.AlertType.ERROR);
                dl.alertDialog(alert , "Lỗi" , (String) response.get("message") , "thatbai");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    // ========== LOGIN BUTTON ==========
    public void loginBtn() {
        try {
            Message request = new Message();
            request.put("action", "login");
            request.put("username", username.getText());
            request.put("password", password.getText());

            Message response = sendRequest(request);

            if ("success".equals(response.get("status"))) {
                dl.alertDialog(new Alert(Alert.AlertType.INFORMATION),
                        "Thành công", (String) response.get("message"), "thanhcong");

                int id = (int) response.get("id");
                String uname = (String) response.get("username");
                String fullname = (String) response.get("fullname");
                String token = (String) response.get("session_token");

                // ✅ Lưu session vào bộ nhớ và file
                Als.saveSession(id, uname, fullname, token);
                saveSessionToLocal(id, uname, fullname, token);

                // ✅ Đóng form login
                Stage currentStage = (Stage) login.getScene().getWindow();
                currentStage.close();

                // ✅ Mở Browser
                openBrowser(String.valueOf(id), uname, fullname);

            } else {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setHeaderText("Lỗi");
                alert.setContentText((String) response.get("message"));
                alert.showAndWait();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ========== LƯU SESSION ==========
    private void saveSessionToLocal(int id, String username, String fullname, String sessionToken) {
        try {
            Properties props = new Properties();
            props.setProperty("id", String.valueOf(id));
            props.setProperty("username", username);
            props.setProperty("fullname", fullname);
            props.setProperty("session_token", sessionToken);

            try (FileOutputStream fos = new FileOutputStream("session.properties")) {
                props.store(fos, "User session data");
            }

            System.out.println("✅ Session đã lưu vào file session.properties");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadSessionFromFile() {
        File file = new File("session.properties");
        if (file.exists()) {
            try (FileInputStream fis = new FileInputStream(file)) {
                Properties props = new Properties();
                props.load(fis);

                int id = Integer.parseInt(props.getProperty("id", "0"));
                String username = props.getProperty("username");
                String fullname = props.getProperty("fullname");
                String token = props.getProperty("session_token");

                if (token != null && !token.isEmpty()) {
                    Als.saveSession(id, username, fullname, token);
                    System.out.println("💾 Loaded session from file → " + username + " | " + fullname);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    // ========== MỞ BROWSER ==========
    private void openBrowser(String id, String username, String fullname) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/FXML/web-browser.fxml"));
        Parent root = loader.load();

        ClientController clientController = loader.getController();
        clientController.Id(id);
        clientController.userName(username);
        clientController.fullName(fullname);
        clientController.setSocketClient(socket , out , in);
        clientController.closeTab();

        Stage stage = new Stage();
        stage.setScene(new Scene(root));
        stage.setTitle("Night Wolf");
        stage.getIcons().add(new Image(getClass().getResourceAsStream("/Image/wolf.png")));
        stage.show();

        System.out.println("✅ Đã mở Browser cho user: " + username + " / " + fullname);
    }


    public void openlink(){
        try{
            Desktop.getDesktop().browse(new URI("https://www.facebook.com/profile.php?id=100064415755780"));
        }catch (Exception e){
            e.printStackTrace();
        }
    }
    public void showpass(){
        if(check.isSelected()){
            password_show.setText(password.getText());
            password_show.setVisible(true);
            password.setVisible(false);
            return;
        }
        password.setText(password_show.getText());
        password.setVisible(true);
        password_show.setVisible(false);
    }




    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        username.addEventFilter(KeyEvent.KEY_PRESSED , e->{
            if (e.getCode() == KeyCode.ENTER) {
                try {
                    password.requestFocus();
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
                e.consume();
            }
        });
        password.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.ENTER) {
                try {
                    loginBtn();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
                event.consume();
            }
        });
// 1️⃣ Load session từ file trước
        loadSessionFromFile();

        // 2️⃣ Nếu có session → kiểm tra hợp lệ với server
        Platform.runLater(() -> {
            try {
                if (Als.hasSession()) {
                    System.out.println("🔹 Đã phát hiện session trong bộ nhớ: " + Als.getUsername());

                    Message request = new Message();
                    request.put("action", "validate_session");
                    request.put("session_token", Als.getSessionToken());

                    Message response = sendRequest(request);

                    if ("success".equals(response.get("status"))) {
                        System.out.println("✅ Session hợp lệ — tự động đăng nhập: " + Als.getUsername());
                        Stage currentStage = (Stage) login.getScene().getWindow();
                        currentStage.close();

                        openBrowser(
                                String.valueOf(Als.getUserId()),
                                Als.getUsername(),
                                Als.getFullname()
                        );
                    } else {
                        System.out.println("⚠️ Session hết hạn — cần đăng nhập lại.");
                        Als.clearSession();
                    }
                } else {
                    System.out.println("⚠️ Chưa có session — chờ người dùng đăng nhập.");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
}
