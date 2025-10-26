package org.example.prjbrowser.model;

import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;

public class HistoryItem {
    private final SimpleIntegerProperty id;
    private final SimpleStringProperty url;
    private final SimpleStringProperty visitTime;

    public HistoryItem(int id, String url, String visitTime) {
        this.id = new SimpleIntegerProperty(id);
        this.url = new SimpleStringProperty(url);
        this.visitTime = new SimpleStringProperty(visitTime);
    }

    public int getId() {
        return id.get();
    }

    public String getUrl() {
        return url.get();
    }

    public String getVisitTime() {
        return visitTime.get();
    }
}
