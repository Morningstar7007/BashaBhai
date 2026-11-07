package com.example.bashabhai;

import androidx.annotation.NonNull;
import androidx.room.Entity;

@Entity(tableName = "favorites", primaryKeys = {"id", "userId"})
public class FavoriteListing {

    @NonNull
    private String id;
    
    @NonNull
    private String userId;
    
    private String landlordId;
    private String title;
    private String type;
    private double rent;
    private String location;
    private String landlordPhone;
    
    private String amenities;
    private String genderPreference;
    private String description;
    private String mapLink;
    private int postNumber;

    public FavoriteListing() {
        this.id = "";
        this.userId = "";
        this.postNumber = 1000;
    }

    public FavoriteListing(@NonNull String id, @NonNull String userId, String landlordId, String title, String type, double rent, String location, String landlordPhone, String amenities, String genderPreference, String description, String mapLink, int postNumber) {
        this.id = id;
        this.userId = userId;
        this.landlordId = landlordId;
        this.title = title;
        this.type = type;
        this.rent = rent;
        this.location = location;
        this.landlordPhone = landlordPhone;
        this.amenities = amenities;
        this.genderPreference = genderPreference;
        this.description = description;
        this.mapLink = mapLink;
        this.postNumber = postNumber > 0 ? postNumber : 1000;
    }

    @NonNull
    public String getId() { return id; }
    public void setId(@NonNull String id) { this.id = id; }
    
    @NonNull
    public String getUserId() { return userId; }
    public void setUserId(@NonNull String userId) { this.userId = userId; }

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

    public String getLandlordPhone() { return landlordPhone; }
    public void setLandlordPhone(String landlordPhone) { this.landlordPhone = landlordPhone; }

    public String getAmenities() { return amenities; }
    public void setAmenities(String amenities) { this.amenities = amenities; }

    public String getGenderPreference() { return genderPreference; }
    public void setGenderPreference(String genderPreference) { this.genderPreference = genderPreference; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getMapLink() { return mapLink; }
    public void setMapLink(String mapLink) { this.mapLink = mapLink; }

    public int getPostNumber() { return postNumber; }
    public void setPostNumber(int postNumber) { this.postNumber = postNumber; }

    public String getFormattedPostNumber() {
        return "#BB-" + (postNumber > 0 ? postNumber : 1000);
    }
}
