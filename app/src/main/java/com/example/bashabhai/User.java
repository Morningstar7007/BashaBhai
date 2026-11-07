package com.example.bashabhai;

public class User {
    private String id;
    private String name;
    private String email;
    private String role; // "STUDENT", "LANDLORD", or "ADMIN"
    private boolean isVerified;
    private String verificationStatus; // "NONE", "PENDING", "APPROVED", "REJECTED"
    private String defaultPhone;
    private String defaultAddress;
    private String profilePicBase64;

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
        this.defaultPhone = "";
        this.defaultAddress = "";
        this.profilePicBase64 = "";
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
    public void setVerified(boolean verified) { isVerified = verified; }
    
    public String getVerificationStatus() { return verificationStatus; }
    public void setVerificationStatus(String verificationStatus) { this.verificationStatus = verificationStatus; }

    public String getDefaultPhone() { return defaultPhone; }
    public void setDefaultPhone(String defaultPhone) { this.defaultPhone = defaultPhone; }

    public String getDefaultAddress() { return defaultAddress; }
    public void setDefaultAddress(String defaultAddress) { this.defaultAddress = defaultAddress; }

    public String getProfilePicBase64() { return profilePicBase64; }
    public void setProfilePicBase64(String profilePicBase64) { this.profilePicBase64 = profilePicBase64; }
}
