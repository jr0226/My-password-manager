package com.example.mypasswordmanager;

public class PasswordEntry {
    private int id;
    private String siteName;
    private String username;
    private String password;
    private String note;
    private String logoData; // NEW: Holds the image as text!

    public PasswordEntry(int id, String siteName, String username, String password, String note, String logoData) {
        this.id = id;
        this.siteName = siteName;
        this.username = username;
        this.password = password;
        this.note = note;
        this.logoData = logoData;
    }

    // --- GETTERS & SETTERS ---
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getSiteName() { return siteName; }

    public String getUsername() { return username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getNote() { return note; }

    public String getLogoData() { return logoData; }
}