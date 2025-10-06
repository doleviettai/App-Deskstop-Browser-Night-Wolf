package org.example.prjbrowser.model;

public class Urls {
    private Integer id;
    private String url;
    private String title;
    private Integer visit_count;
    private Integer typed_count;
    private boolean hidden;

    public Urls(boolean hidden, Integer id, String title, Integer typed_count, String url, Integer visit_count) {
        this.hidden = hidden;
        this.id = id;
        this.title = title;
        this.typed_count = typed_count;
        this.url = url;
        this.visit_count = visit_count;
    }

    public boolean isHidden() {
        return hidden;
    }

    public void setHidden(boolean hidden) {
        this.hidden = hidden;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Integer getTyped_count() {
        return typed_count;
    }

    public void setTyped_count(Integer typed_count) {
        this.typed_count = typed_count;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public Integer getVisit_count() {
        return visit_count;
    }

    public void setVisit_count(Integer visit_count) {
        this.visit_count = visit_count;
    }
}
