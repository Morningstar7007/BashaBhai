package com.example.bashabhai;

public class User {
    private String id;
    private String name;
    private String email;
    private String role; // "STUDENT", "LANDLORD", or "ADMIN"
    private boolean isVerified;
    private String verificationStatus; // "NONE", "PENDING", "APPROVED", "REJECTED"
    private String verificationRejectionReason; // Reason provided by admin
    private String defaultPhone;
    private String defaultAddress;
    private String profilePicBase64;
    
    // Student Specific Fields
    private String contactNumber;
    private String department;
    private String batch;
    
    // Review System
    private double ratingSum;
    private int totalReviews;

    // Default constructor for Firebase
    public User() {
    }

    public User(String id, String name, String email) {
        this(id, name, email, "STUDENT");
    }

    public User(String id, String name, String email, String role) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.role = role != null ? role : "STUDENT";
        this.isVerified = false;
        this.verificationStatus = "NONE";
        this.verificationRejectionReason = "";
        this.defaultPhone = "";
        this.defaultAddress = "";
        this.profilePicBase64 = "";
        this.contactNumber = "";
        this.department = "";
        this.batch = "";
        this.ratingSum = 0.0;
        this.totalReviews = 0;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public boolean isVerified() { return isVerified; }
    public void setVerified(boolean verified) { this.isVerified = verified; }
    
    public String getVerificationStatus() { return verificationStatus; }
    public void setVerificationStatus(String verificationStatus) { this.verificationStatus = verificationStatus; }

    public String getVerificationRejectionReason() { return verificationRejectionReason; }
    public void setVerificationRejectionReason(String reason) { this.verificationRejectionReason = reason; }

    public String getDefaultPhone() { return defaultPhone; }
    public void setDefaultPhone(String defaultPhone) { this.defaultPhone = defaultPhone; }

    public String getDefaultAddress() { return defaultAddress; }
    public void setDefaultAddress(String defaultAddress) { this.defaultAddress = defaultAddress; }

    public String getProfilePicBase64() { return profilePicBase64; }
    public void setProfilePicBase64(String profilePicBase64) { this.profilePicBase64 = profilePicBase64; }

    public String getContactNumber() { return contactNumber; }
    public void setContactNumber(String contactNumber) { this.contactNumber = contactNumber; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public String getBatch() { return batch; }
    public void setBatch(String batch) { this.batch = batch; }
    
    public double getRatingSum() { return ratingSum; }
    public void setRatingSum(double ratingSum) { this.ratingSum = ratingSum; }
    
    public int getTotalReviews() { return totalReviews; }
    public void setTotalReviews(int totalReviews) { this.totalReviews = totalReviews; }
}
