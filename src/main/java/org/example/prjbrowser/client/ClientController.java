package org.example.prjbrowser.client;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import org.example.prjbrowser.common.Message;
import org.example.prjbrowser.model.AutoLoginService;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.URI;
import java.net.URL;
import java.util.ResourceBundle;

public class ClientController implements Initializable {
    @FXML
    private Tab addTab;
    @FXML
    private TabPane tabPane;

    String id = "";
    String fullName = "Chưa đăng nhập";
    String username;
    public void Id(String id){
        this.id = id;
    }

    public void fullName(String fullName){
        this.fullName = fullName;
    }

    public void userName(String username){
        this.username = username;
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

    public void closeTab(){
        tabPane.getTabs().clear();

        Tab addTab = new Tab("+");
        addTab.setClosable(false);
        tabPane.getTabs().add(addTab);

        // Lắng nghe khi chọn tab "+"
        tabPane.getSelectionModel().selectedItemProperty().addListener((obs, oldTab, newTab) -> {
            if (newTab != null && "+".equals(newTab.getText())) {
                createNewBrowserTab("New Tab");
                tabPane.getSelectionModel().select(tabPane.getTabs().size() - 2);
            }
        });
    }

    public void createNewBrowserTab(String title) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/FXML/new-tab.fxml"));
            Parent root = loader.load();

            // Lấy controller của tab mới
            NewTabController controller = loader.getController();
            controller.receiverNickName(id,username, fullName);
            controller.clearBookmarksUI();
            controller.loadUserBookmarks();

            Tab newTab = new Tab(title, root);

            // Set default icon cho tab (chưa load) - ĐÃ FIX
            Image defaultImg = new Image(getClass().getResource("/Image/wolf.png").toExternalForm(), 16, 16, true, true, true);
            ImageView defaultIcon = new ImageView(defaultImg);
            defaultIcon.setFitWidth(16);
            defaultIcon.setFitHeight(16);
            defaultIcon.setPreserveRatio(true);
            defaultIcon.setSmooth(true);
            newTab.setGraphic(defaultIcon);

            // Khi nhập URL → load và đổi tên tab
//            controller.getSearch().setOnKeyPressed(e -> {
//                if (e.getCode() == KeyCode.ENTER) {
//                    controller.loadUrl();
//                }
//            });


            // Khi trang load xong → cập nhật favicon + title
            controller.getEngine().getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
                if(newState == javafx.concurrent.Worker.State.SUCCEEDED) {
                    Platform.runLater(() -> {
                        // Lấy title và rút gọn nếu quá dài - ĐÃ FIX NULL CHECK
                        String pageTitle = (String) controller.getEngine().executeScript("document.title");
                        if(pageTitle != null && pageTitle.length() > 20) {
                            pageTitle = pageTitle.substring(0, 20) + "...";
                        } else if(pageTitle == null || pageTitle.isEmpty()) {
                            pageTitle = "New Tab";
                        }

                        // Lấy favicon từ /favicon.ico - ĐÃ FIX
                        ImageView iconView = getImageView(controller, defaultImg);

                        newTab.setGraphic(iconView);
                        newTab.setText(pageTitle);
                        controller.getSearch().setText(controller.getEngine().getLocation());

//                        controller.setCurrentPageTitle(pageTitle);
                    });
                }
            });

            // Chèn tab ngay trước tab "+"
            int addIndex = tabPane.getTabs().size() - 1;
            if (addIndex < 0) addIndex = 0;
            tabPane.getTabs().add(addIndex, newTab);
            tabPane.getSelectionModel().select(newTab);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static ImageView getImageView(NewTabController controller, Image defaultImg) {
        ImageView iconView;
        try {
            URI uri = new URI(controller.getEngine().getLocation());
            String faviconUrl = uri.getScheme() + "://" + uri.getHost() + "/favicon.ico";

            // Load favicon với size cố định ngay từ đầu - ĐÃ FIX
            Image favicon = new Image(faviconUrl, 16, 16, true, true, true);

            // Kiểm tra favicon có hợp lệ không - ĐÃ FIX
            if(favicon.isError() || favicon.getWidth() == 0) {
                throw new Exception("Favicon not found");
            }

            iconView = new ImageView(favicon);

        } catch(Exception ex) {
            // fallback: giữ wolf.png
            iconView = new ImageView(defaultImg);
        }

        // QUAN TRỌNG: Set tất cả thuộc tính để đảm bảo size cố định
        iconView.setFitWidth(16);
        iconView.setFitHeight(16);
        iconView.setPreserveRatio(true);  // Giữ tỷ lệ ảnh
        iconView.setSmooth(true);         // Làm mịn khi scale

        return iconView;
    }


    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        // Xóa tất cả tab mặc định trong FXML
        this.username = null;
        tabPane.getTabs().clear();

        // Tạo tab đầu tiên
        // Chỉ tạo tab mặc định khi chưa login
        if ("Chưa đăng nhập".equals(fullName) && username == null) {
            createNewBrowserTab("New Tab");
        }

        // Luôn giữ 1 tab "+" cuối cùng
        Tab addTab = new Tab("+");
        addTab.setClosable(false);
        tabPane.getTabs().add(addTab);

        // Lắng nghe khi chọn tab "+"
        tabPane.getSelectionModel().selectedItemProperty().addListener((obs, oldTab, newTab) -> {
            if (newTab != null && "+".equals(newTab.getText())) {
                createNewBrowserTab("New Tab");
                tabPane.getSelectionModel().select(tabPane.getTabs().size() - 2);
            }
        });

        // Đăng ký phím tắt sau khi scene đã sẵn sàng
        Platform.runLater(() -> {
            tabPane.getScene().setOnKeyPressed(event -> {
                // Ctrl + T => mở tab mới
                if (new KeyCodeCombination(KeyCode.T, KeyCombination.CONTROL_DOWN).match(event)) {
                    createNewBrowserTab("New Tab");
                }
                // Ctrl + W => đóng tab hiện tại (trừ tab "+")
                else if (new KeyCodeCombination(KeyCode.W, KeyCombination.CONTROL_DOWN).match(event)) {
                    Tab selected = tabPane.getSelectionModel().getSelectedItem();
                    if (selected != null && !"+".equals(selected.getText())) {
                        tabPane.getTabs().remove(selected);
                    }
                }
                // Ctrl + Tab => chuyển tab kế tiếp
                else if (new KeyCodeCombination(KeyCode.TAB, KeyCombination.CONTROL_DOWN).match(event)) {
                    tabPane.getSelectionModel().selectNext();
                    event.consume(); // chặn focus nhảy lung tung
                }
                // Ctrl + Shift + Tab => chuyển tab trước đó
                else if (new KeyCodeCombination(KeyCode.TAB, KeyCombination.CONTROL_DOWN, KeyCombination.SHIFT_DOWN).match(event)) {
                    tabPane.getSelectionModel().selectPrevious();
                    event.consume();
                }

                // Ctrl + số (1–9) : chuyển trực tiếp tab
                else if (event.getCode().isDigitKey()) {
                    String text = event.getText(); // lấy ký tự số
                    if (!text.isEmpty()) {
                        int num = Integer.parseInt(text);
                        int tabCount = tabPane.getTabs().size() - 1; // bỏ tab "+"
                        if (num >= 1 && num <= 8 && num <= tabCount) {
                            tabPane.getSelectionModel().select(num - 1);
                        } else if (num == 9 && tabCount > 0) {
                            // Ctrl+9 → chọn tab cuối cùng
                            tabPane.getSelectionModel().select(tabCount - 1);
                        }
                    }
                    event.consume();
                }
            });
        });

        AutoLoginService autoLogin = AutoLoginService.getInstance();
        if (autoLogin.hasSession()) {
            this.id = String.valueOf(autoLogin.getUserId());
            this.username = autoLogin.getUsername();
            this.fullName = autoLogin.getFullname();

            System.out.println("✅ ClientController khởi tạo với user: " + username + " | " + fullName);

            // ✅ Tạo tab đầu tiên với thông tin user
//            Platform.runLater(() -> createNewBrowserTab("Home"));
        }
    }
}
