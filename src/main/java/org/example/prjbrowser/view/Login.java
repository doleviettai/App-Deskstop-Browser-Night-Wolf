package org.example.prjbrowser.view;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

public class Login extends Application {
    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader fxmlLoader = new FXMLLoader(App.class.getResource("/FXML/login.fxml"));
        Scene scene = new Scene(fxmlLoader.load());
        stage.setTitle("Night Wolf");
//        stage.setMinHeight(425);
//        stage.setMinWidth(615);
        stage.setScene(scene);
        Image icon = new Image(getClass().getResourceAsStream("/Image/wolf.png"));
        stage.getIcons().add(icon);
        stage.show();
    }
}
