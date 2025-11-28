module org.example.prjbrowser {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;
    requires javafx.web;
    requires javafx.base;
    requires java.sql;
    requires java.desktop;
    requires mysql.connector.java;
    requires org.fxmisc.richtext;
    requires jdk.jsobject;
    requires com.google.gson;
    requires io.github.cdimascio.dotenv.java;


    opens org.example.prjbrowser to javafx.fxml;
    exports org.example.prjbrowser;

    exports org.example.prjbrowser.client;
    opens org.example.prjbrowser.client to javafx.fxml;

    exports org.example.prjbrowser.client.desginer;
    opens org.example.prjbrowser.client.desginer to javafx.fxml;

    exports org.example.prjbrowser.common;
    opens org.example.prjbrowser.common to javafx.fxml;

    exports org.example.prjbrowser.model;
    opens org.example.prjbrowser.model to javafx.fxml;

    exports org.example.prjbrowser.server;
    opens org.example.prjbrowser.server to javafx.fxml;

    exports org.example.prjbrowser.view;
    opens org.example.prjbrowser.view to javafx.fxml;
}