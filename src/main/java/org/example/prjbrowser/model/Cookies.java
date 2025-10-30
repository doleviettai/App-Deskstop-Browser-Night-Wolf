package org.example.prjbrowser.model;

import java.sql.Timestamp;

public class Cookies {
    private Integer id;
    private Integer user_id;
    private String host_key;
    private String name;
    private String value;
    private String path;
    private Boolean secure;
    private Boolean http_only;
    private Timestamp expiry;
    private Timestamp creation_time;
    private Timestamp last_access_time;
    private Boolean persistent;

    // ===== Constructor đầy đủ =====
    public Cookies(Integer id, Integer user_id, String host_key, String name, String value, String path,
                   Boolean secure, Boolean http_only, Timestamp expiry, Timestamp creation_time,
                   Timestamp last_access_time, Boolean persistent) {
        this.id = id;
        this.user_id = user_id;
        this.host_key = host_key;
        this.name = name;
        this.value = value;
        this.path = path;
        this.secure = secure;
        this.http_only = http_only;
        this.expiry = expiry;
        this.creation_time = creation_time;
        this.last_access_time = last_access_time;
        this.persistent = persistent;
    }

    // ===== Constructor rút gọn (nếu chỉ cần khi insert) =====
    public Cookies(Integer user_id, String host_key, String name, String value, String path,
                   Boolean secure, Boolean http_only, Timestamp expiry, Boolean persistent) {
        this.user_id = user_id;
        this.host_key = host_key;
        this.name = name;
        this.value = value;
        this.path = path;
        this.secure = secure;
        this.http_only = http_only;
        this.expiry = expiry;
        this.persistent = persistent;
    }

    // ===== Getters & Setters =====
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getUser_id() {
        return user_id;
    }

    public void setUser_id(Integer user_id) {
        this.user_id = user_id;
    }

    public String getHost_key() {
        return host_key;
    }

    public void setHost_key(String host_key) {
        this.host_key = host_key;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public Boolean getSecure() {
        return secure;
    }

    public void setSecure(Boolean secure) {
        this.secure = secure;
    }

    public Boolean getHttp_only() {
        return http_only;
    }

    public void setHttp_only(Boolean http_only) {
        this.http_only = http_only;
    }

    public Timestamp getExpiry() {
        return expiry;
    }

    public void setExpiry(Timestamp expiry) {
        this.expiry = expiry;
    }

    public Timestamp getCreation_time() {
        return creation_time;
    }

    public void setCreation_time(Timestamp creation_time) {
        this.creation_time = creation_time;
    }

    public Timestamp getLast_access_time() {
        return last_access_time;
    }

    public void setLast_access_time(Timestamp last_access_time) {
        this.last_access_time = last_access_time;
    }

    public Boolean getPersistent() {
        return persistent;
    }

    public void setPersistent(Boolean persistent) {
        this.persistent = persistent;
    }

    // ===== ToString để debug dễ dàng =====
    @Override
    public String toString() {
        return "Cookies{" +
                "id=" + id +
                ", user_id=" + user_id +
                ", host_key='" + host_key + '\'' +
                ", name='" + name + '\'' +
                ", value='" + value + '\'' +
                ", path='" + path + '\'' +
                ", secure=" + secure +
                ", http_only=" + http_only +
                ", expiry=" + expiry +
                ", creation_time=" + creation_time +
                ", last_access_time=" + last_access_time +
                ", persistent=" + persistent +
                '}';
    }
}
