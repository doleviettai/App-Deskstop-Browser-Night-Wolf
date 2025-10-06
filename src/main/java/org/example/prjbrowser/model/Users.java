package org.example.prjbrowser.model;

public class Users {
    private Integer id;
    private String username;
    private String firstname;
    private String lastname;
    private String password;
    private Integer phone_number;
    private String avatar;

    public Users(String avatar, String firstname, Integer id, String lastname, String password, Integer phone_number, String username) {
        this.avatar = avatar;
        this.firstname = firstname;
        this.id = id;
        this.lastname = lastname;
        this.password = password;
        this.phone_number = phone_number;
        this.username = username;
    }

    public String getAvatar() {
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }

    public String getFirstname() {
        return firstname;
    }

    public void setFirstname(String firstname) {
        this.firstname = firstname;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
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

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }
}
