package com.example.passmanager;


public class PasswordHandler {

    public final org.springframework.security.crypto.password.PasswordEncoder encoder
            = new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder();


}
