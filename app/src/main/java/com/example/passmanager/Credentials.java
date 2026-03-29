package com.example.passmanager;

public class Credentials {
    private String website;
    private String username;
    private String password;



    public String getPassword() {
        return password;
    }
    public String getUsername() {
        return username;
    }
    public String getWebsite() {
        return website;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setWebsite(String website) {
        this.website = website;
    }
}
