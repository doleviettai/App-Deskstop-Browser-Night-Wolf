package org.example.prjbrowser.server;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public class ServerController implements Initializable {
    @FXML
    private VBox Server_Vbox;

    private ServerSocket serverSocket;
    private final List<ClientHandler> clientHandlers = new ArrayList<>();

    private final int PORT = 12345; // cổng server

    /**
     * Thêm log vào UI
     */
    public void addLog(String msg) {
        Platform.runLater(() -> {
            Label label = new Label(msg);
            Server_Vbox.getChildren().add(label);
        });
    }

    /**
     * Khởi chạy server trong 1 thread riêng
     */
    private void startServer() {
        new Thread(() -> {
            try {
                serverSocket = new ServerSocket(PORT);
                addLog("✅ Server đã khởi động tại cổng " + PORT);

                while (!serverSocket.isClosed()) {
                    Socket socket = serverSocket.accept();
                    addLog("🔗 Kết nối mới từ: " + socket.getInetAddress());

                    ClientHandler handler = new ClientHandler(socket, this);
                    clientHandlers.add(handler);

                    Thread thread = new Thread(handler);
                    thread.start();
                }
            } catch (IOException e) {
                addLog("❌ Lỗi server: " + e.getMessage());
                e.printStackTrace();
            }
        }).start();
    }

    /**
     * Khi giao diện server.fxml load xong thì khởi động server
     */
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        startServer();
    }
}
