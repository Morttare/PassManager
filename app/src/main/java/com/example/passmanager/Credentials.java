package com.example.passmanager;

public class Credentials {
    private String website;
    private String username;
    private String password;
    private String iv;
    private String salt;

    public Credentials(String web, String name, String pass){
        this.website = web;
        this.username = name;
        this.password = pass;
    }

    public String getIv() {
        return iv;
    }

    public String getSalt() {
        return salt;
    }

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

    public void setIv(String iv) {
        this.iv = iv;
    }

    public void setSalt(String salt) {
        this.salt = salt;
    }
}
