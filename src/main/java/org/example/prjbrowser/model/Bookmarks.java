package org.example.prjbrowser.model;

public class Bookmarks {
    private int id;
    private int userId;
    private int urlId;
    private String title;
    private String url;

    // ====== Constructors ======
    public Bookmarks() {
    }

    public Bookmarks(int id, int userId, int urlId, String title, String url) {
        this.id = id;
        this.userId = userId;
        this.urlId = urlId;
        this.title = title;
        this.url = url;
    }

    // ====== Getters & Setters ======
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public int getUrlId() {
        return urlId;
    }

    public void setUrlId(int urlId) {
        this.urlId = urlId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    @Override
    public String toString() {
        return "Bookmarks{" +
                "id=" + id +
                ", userId=" + userId +
                ", urlId=" + urlId +
                ", title='" + title + '\'' +
                ", url='" + url + '\'' +
                '}';
    }
}
