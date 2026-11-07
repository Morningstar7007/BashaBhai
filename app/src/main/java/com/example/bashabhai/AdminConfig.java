package com.example.bashabhai;

public class AdminConfig {

    // You can put YOUR personal email address here to be recognized as Admin!
    public static final String ADMIN_EMAIL = "admin@bashabhai.com";

    public static boolean isAdmin(String email) {
        if (email == null) return false;
        // Recognizes explicit admin email OR any email containing "admin"
        return email.equalsIgnoreCase(ADMIN_EMAIL) || email.toLowerCase().contains("admin");
    }
}
