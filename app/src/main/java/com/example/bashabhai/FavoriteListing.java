package com.example.bashabhai;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "favorites")
public class FavoriteListing {

    @PrimaryKey
    @NonNull
    private String id;
    private String title;
    private String type;
    private double rent;
    private String location;
    private String landlordPhone;

    public FavoriteListing() {
        this.id = "";
    }

    public FavoriteListing(@NonNull String id, String title, String type, double rent, String location, String landlordPhone) {
        this.id = id;
        this.title = title;
        this.type = type;
        this.rent = rent;
        this.location = location;
        this.landlordPhone = landlordPhone;
    }

    @NonNull
    public String getId() { return id; }
    public void setId(@NonNull String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public double getRent() { return rent; }
    public void setRent(double rent) { this.rent = rent; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getLandlordPhone() { return landlordPhone; }
    public void setLandlordPhone(String landlordPhone) { this.landlordPhone = landlordPhone; }
}
