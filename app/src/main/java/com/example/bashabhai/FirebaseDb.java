package com.example.bashabhai;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class FirebaseDb {

    public static final String DATABASE_URL = "https://bashabhai-f2a1f-default-rtdb.asia-southeast1.firebasedatabase.app";
    private static FirebaseDatabase instance;

    public static synchronized FirebaseDatabase getInstance() {
        if (instance == null) {
            try {
                instance = FirebaseDatabase.getInstance(DATABASE_URL);
                instance.setPersistenceEnabled(true);
            } catch (Exception e) {
                instance = FirebaseDatabase.getInstance(DATABASE_URL);
            }
        }
        return instance;
    }

    public static DatabaseReference getRef(String path) {
        return getInstance().getReference(path);
    }
}
