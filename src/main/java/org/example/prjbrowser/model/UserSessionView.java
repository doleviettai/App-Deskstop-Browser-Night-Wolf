package org.example.prjbrowser.model;

import java.sql.Timestamp;

public class UserSessionView {

    private Integer id;
    private String username;
    private String firstname;
    private String lastname;
    private String password;
    private Integer phone_number;
    private byte[] avatar;

    private String sessionToken;
    private Timestamp createdAt;
    private Timestamp expiresAt;

    // Constructor đầy đủ
    public UserSessionView(Integer id, String username, String firstname, String lastname,
                           String password, Integer phone_number, byte[] avatar,
                           String sessionToken, Timestamp createdAt, Timestamp expiresAt) {

        this.id = id;
        this.username = username;
        this.firstname = firstname;
        this.lastname = lastname;
        this.password = password;
        this.phone_number = phone_number;
        this.avatar = avatar;

        this.sessionToken = sessionToken;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
    }

    // ================== GETTER / SETTER ==================

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getFirstname() {
        return firstname;
    }

    public void setFirstname(String firstname) {
        this.firstname = firstname;
    }

    public String getLastname() {
        return lastname;
    }

    public void setLastname(String lastname) {
        this.lastname = lastname;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Integer getPhone_number() {
        return phone_number;
    }

    public void setPhone_number(Integer phone_number) {
        this.phone_number = phone_number;
    }

    public byte[] getAvatar() {
        return avatar;
    }

    public void setAvatar(byte[] avatar) {
        this.avatar = avatar;
    }

    public String getSessionToken() {
        return sessionToken;
    }

    public void setSessionToken(String sessionToken) {
        this.sessionToken = sessionToken;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public Timestamp getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Timestamp expiresAt) {
        this.expiresAt = expiresAt;
    }
}
