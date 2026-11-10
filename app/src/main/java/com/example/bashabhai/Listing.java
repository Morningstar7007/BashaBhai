package com.example.bashabhai;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

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
    private String mapLink; // Landlord's provided Google Maps URL
    private String landlordPhone;
    private String status; // "PENDING", "APPROVED", "REJECTED", "RENTED"
    private int postNumber; // Human-readable unique serial number (e.g. 1001)
    private String rejectionReason; // Provided by admin if REJECTED
    
    // NEW: List of Base64 encoded images
    private List<String> imageBase64List; 

    // Required empty public constructor for Firebase
    public Listing() {
        this.status = "APPROVED"; // Default for legacy/fallback
        this.postNumber = 1000;
        this.rejectionReason = "";
        this.imageBase64List = new ArrayList<>();
    }

    public Listing(String id, String landlordId, String title, String type, double rent, String location, String description, String amenities, String genderPreference, boolean isRented, String mapLink, String landlordPhone, String status, int postNumber, List<String> imageBase64List) {
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
        this.mapLink = mapLink;
        this.landlordPhone = landlordPhone;
        this.status = status != null ? status : "PENDING";
        this.postNumber = postNumber > 0 ? postNumber : 1000;
        this.rejectionReason = "";
        this.imageBase64List = imageBase64List != null ? imageBase64List : new ArrayList<>();
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
    public void setRented(boolean rented) { this.isRented = rented; }

    public String getMapLink() { return mapLink; }
    public void setMapLink(String mapLink) { this.mapLink = mapLink; }
    
    public String getLandlordPhone() { return landlordPhone; }
    public void setLandlordPhone(String landlordPhone) { this.landlordPhone = landlordPhone; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public int getPostNumber() { return postNumber; }
    public void setPostNumber(int postNumber) { this.postNumber = postNumber; }
    
    public String getRejectionReason() { return rejectionReason; }
    public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }
    
    // Use Exclude so RTDB ignores this field, keeping RTDB lightweight
    @com.google.firebase.database.Exclude
    public List<String> getImageBase64List() { return imageBase64List; }
    
    @com.google.firebase.database.Exclude
    public void setImageBase64List(List<String> imageBase64List) { this.imageBase64List = imageBase64List; }

    public String getFormattedPostNumber() {
        return "#BB-" + (postNumber > 0 ? postNumber : 1000);
    }
}
