package org.example.prjbrowser.client.desginer;

import javafx.scene.control.Alert;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.util.Objects;

public class dialog {
    public void alertDialog(Alert alert, String title , String contentText , String dialog){
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(contentText);
        // Thêm icon cho Alert
        Image icons = new Image(Objects.requireNonNull(getClass().getResourceAsStream("/Image/wolf.png")));
        Stage stage = (Stage) alert.getDialogPane().getScene().getWindow();
        stage.getIcons().add(icons);
        // Thêm CSS
        alert.getDialogPane().getStylesheets().add(
                Objects.requireNonNull(getClass().getResource("/CSS/dialog.css")).toExternalForm()
        );
        // Thêm style class, ví dụ: thành công
        alert.getDialogPane().getStyleClass().add(dialog);
        // Hiển thị Alert
        alert.showAndWait();
    }

}
