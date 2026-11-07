package com.example.bashabhai;

public class VerificationRequest {
    private String userId;
    private String nidBase64;
    private String billBase64;
    private String nidPhotoUrl;
    private String billPhotoUrl;

    public VerificationRequest() {
    }

    public VerificationRequest(String userId, String nidBase64, String billBase64) {
        this.userId = userId;
        this.nidBase64 = nidBase64;
        this.billBase64 = billBase64;
    }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getNidBase64() { return nidBase64; }
    public void setNidBase64(String nidBase64) { this.nidBase64 = nidBase64; }

    public String getBillBase64() { return billBase64; }
    public void setBillBase64(String billBase64) { this.billBase64 = billBase64; }

    public String getNidPhotoUrl() { return nidPhotoUrl; }
    public void setNidPhotoUrl(String nidPhotoUrl) { this.nidPhotoUrl = nidPhotoUrl; }

    public String getBillPhotoUrl() { return billPhotoUrl; }
    public void setBillPhotoUrl(String billPhotoUrl) { this.billPhotoUrl = billPhotoUrl; }
}
