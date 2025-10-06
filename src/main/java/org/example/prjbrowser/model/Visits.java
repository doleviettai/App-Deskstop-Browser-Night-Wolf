package org.example.prjbrowser.model;

public class Visits {
    private Integer id;
    private Integer user_id;
    private Integer url_id;
    private Integer from_visit;
    private Integer transition_type;

    public Visits(Integer from_visit, Integer id, Integer transition_type, Integer url_id, Integer user_id) {
        this.from_visit = from_visit;
        this.id = id;
        this.transition_type = transition_type;
        this.url_id = url_id;
        this.user_id = user_id;
    }

    public Integer getFrom_visit() {
        return from_visit;
    }

    public void setFrom_visit(Integer from_visit) {
        this.from_visit = from_visit;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getTransition_type() {
        return transition_type;
    }

    public void setTransition_type(Integer transition_type) {
        this.transition_type = transition_type;
    }

    public Integer getUrl_id() {
        return url_id;
    }

    public void setUrl_id(Integer url_id) {
        this.url_id = url_id;
    }

    public Integer getUser_id() {
        return user_id;
    }

    public void setUser_id(Integer user_id) {
        this.user_id = user_id;
    }
}
