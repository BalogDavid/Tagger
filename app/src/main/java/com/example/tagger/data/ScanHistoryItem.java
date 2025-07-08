package com.example.tagger.data;

/**
 * CLASA MODEL PENTRU STOCAREA INFORMATIILOR DESPRE O SCANARE
 */
public class ScanHistoryItem {
    private long id;
    private long date;
    private String brandName;
    private String result;
    private float confidence;
    private String imagePath;
    
    // CONSTRUCTOR PENTRU CREAREA UNEI NOI INTRARI
    public ScanHistoryItem(long date, String brandName, String result, float confidence, String imagePath) {
        this.date = date;
        this.brandName = brandName;
        this.result = result;
        this.confidence = confidence;
        this.imagePath = imagePath;
    }
    
    // CONSTRUCTOR PENTRU INCARCAREA DIN BAZA DE DATE
    public ScanHistoryItem(long id, long date, String brandName, String result, float confidence, String imagePath) {
        this.id = id;
        this.date = date;
        this.brandName = brandName;
        this.result = result;
        this.confidence = confidence;
        this.imagePath = imagePath;
    }
    
    // GETTERI SI SETTERI
    public long getId() {
        return id;
    }
    
    public void setId(long id) {
        this.id = id;
    }
    
    public long getDate() {
        return date;
    }
    
    public void setDate(long date) {
        this.date = date;
    }
    
    public String getBrandName() {
        return brandName;
    }
    
    public void setBrandName(String brandName) {
        this.brandName = brandName;
    }
    
    public String getResult() {
        return result;
    }
    
    public void setResult(String result) {
        this.result = result;
    }
    
    public float getConfidence() {
        return confidence;
    }
    
    public void setConfidence(float confidence) {
        this.confidence = confidence;
    }
    
    public String getImagePath() {
        return imagePath;
    }
    
    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }
} 