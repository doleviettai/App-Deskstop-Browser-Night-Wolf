package org.example.prjbrowser.model;

import javafx.beans.property.SimpleStringProperty;

public class HistoryItem {
    private final SimpleStringProperty url;
    private final SimpleStringProperty visitTime;

    public HistoryItem(String url, String visitTime) {
        this.url = new SimpleStringProperty(url);
        this.visitTime = new SimpleStringProperty(visitTime);
    }

    public String getUrl() { return url.get(); }
    public void setUrl(String url) { this.url.set(url); }

    public String getVisitTime() { return visitTime.get(); }
    public void setVisitTime(String visitTime) { this.visitTime.set(visitTime); }
}
