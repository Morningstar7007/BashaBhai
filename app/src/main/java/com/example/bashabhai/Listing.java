package com.example.bashabhai;

import java.io.Serializable;

public class Listing implements Serializable {
    private String id;
    private String landlordId;
    private String title;
    private String type; // e.g., "Room", "Flat", "Mess Seat"
    private double rent;
    private String location; // e.g., Address or Area
    private String description;
    private String amenities; // e.g., "WiFi, Water, Electricity"
    private String genderPreference; // e.g., "Male", "Female", "Any"
    private boolean isRented;
    private double latitude;
    private double longitude;
    private String landlordPhone;

    // Required empty public constructor for Firebase
    public Listing() {
    }

    public Listing(String id, String landlordId, String title, String type, double rent, String location, String description, String amenities, String genderPreference, boolean isRented, double latitude, double longitude, String landlordPhone) {
        this.id = id;
        this.landlordId = landlordId;
        this.title = title;
        this.type = type;
        this.rent = rent;
        this.location = location;
        this.description = description;
        this.amenities = amenities;
        this.genderPreference = genderPreference;
        this.isRented = isRented;
        this.latitude = latitude;
        this.longitude = longitude;
        this.landlordPhone = landlordPhone;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getLandlordId() { return landlordId; }
    public void setLandlordId(String landlordId) { this.landlordId = landlordId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public double getRent() { return rent; }
    public void setRent(double rent) { this.rent = rent; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getAmenities() { return amenities; }
    public void setAmenities(String amenities) { this.amenities = amenities; }

    public String getGenderPreference() { return genderPreference; }
    public void setGenderPreference(String genderPreference) { this.genderPreference = genderPreference; }

    public boolean isRented() { return isRented; }
    public void setRented(boolean rented) { isRented = rented; }

    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }

    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }
    
    public String getLandlordPhone() { return landlordPhone; }
    public void setLandlordPhone(String landlordPhone) { this.landlordPhone = landlordPhone; }
}