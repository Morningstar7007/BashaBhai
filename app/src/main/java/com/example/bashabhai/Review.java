package com.example.bashabhai;

public class Review {
    private String reviewId;
    private String landlordId;
    private String studentId;
    private String studentName;
    private String studentInfo; // e.g. "CSE - 2K20"
    private float rating;
    private String reviewText;
    private long timestamp;

    public Review() {
    }

    public Review(String reviewId, String landlordId, String studentId, String studentName, String studentInfo, float rating, String reviewText, long timestamp) {
        this.reviewId = reviewId;
        this.landlordId = landlordId;
        this.studentId = studentId;
        this.studentName = studentName;
        this.studentInfo = studentInfo;
        this.rating = rating;
        this.reviewText = reviewText;
        this.timestamp = timestamp;
    }

    public String getReviewId() { return reviewId; }
    public void setReviewId(String reviewId) { this.reviewId = reviewId; }

    public String getLandlordId() { return landlordId; }
    public void setLandlordId(String landlordId) { this.landlordId = landlordId; }

    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }

    public String getStudentName() { return studentName; }
    public void setStudentName(String studentName) { this.studentName = studentName; }

    public String getStudentInfo() { return studentInfo; }
    public void setStudentInfo(String studentInfo) { this.studentInfo = studentInfo; }

    public float getRating() { return rating; }
    public void setRating(float rating) { this.rating = rating; }

    public String getReviewText() { return reviewText; }
    public void setReviewText(String reviewText) { this.reviewText = reviewText; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}
