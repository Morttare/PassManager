package com.example.passmanager;

/*
Class for storing user credentials,
as well as the initialization vector and salt used for encrypting the password
 */
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

    public void setIv(String iv) {
        this.iv = iv;
    }

    public void setSalt(String salt) {
        this.salt = salt;
    }
}
