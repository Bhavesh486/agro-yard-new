package com.projects.agroyard.models;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

public class Product {
    private static final String TAG = "Product";
    
    private int id;
    private String farmerName;
    private String farmerMobile;
    private String productName;
    private String harvestingDate;
    private String farmingType;
    private double quantity;
    private double price;
    private double expectedPrice;
    private String description;
    private String imageUrl;
    private String imagePath;

    public Product() {
        // Empty constructor required for JSON parsing
    }

    public Product(JSONObject jsonObject) throws JSONException {
        try {
            this.id = jsonObject.getInt("id");
            this.farmerName = jsonObject.getString("farmer_name");
            this.farmerMobile = jsonObject.optString("farmer_mobile", "");
            this.productName = jsonObject.getString("product_name");
            this.harvestingDate = jsonObject.getString("harvesting_date");
            this.farmingType = jsonObject.getString("farming_type");
            this.quantity = jsonObject.getDouble("quantity");
            this.price = jsonObject.getDouble("price");
            this.expectedPrice = jsonObject.getDouble("expected_price");
            this.description = jsonObject.getString("description");
            
            // Handle image paths with proper logging for debugging
            if (jsonObject.has("image_path")) {
                this.imagePath = jsonObject.getString("image_path");
                Log.d(TAG, "Got image_path from JSON: " + this.imagePath + " for product: " + this.productName);
            } else {
                Log.w(TAG, "No image_path for product: " + this.productName);
            }
            
            // Also check for image_url for backwards compatibility
            if (jsonObject.has("image_url")) {
                this.imageUrl = jsonObject.getString("image_url");
                Log.d(TAG, "Got image_url from JSON: " + this.imageUrl + " for product: " + this.productName);
            }
            
            // For backwards compatibility, also check image_filename
            if (jsonObject.has("image_filename") && (this.imagePath == null || this.imagePath.isEmpty())) {
                this.imagePath = jsonObject.getString("image_filename");
                Log.d(TAG, "Using image_filename as fallback: " + this.imagePath);
            }
        } catch (JSONException e) {
            Log.e(TAG, "Error parsing product JSON: " + e.getMessage(), e);
            throw e;
        }
    }

    // Getters
    public int getId() { return id; }
    public String getFarmerName() { return farmerName; }
    public String getFarmerMobile() { return farmerMobile; }
    public String getProductName() { return productName; }
    public String getHarvestingDate() { return harvestingDate; }
    public String getFarmingType() { return farmingType; }
    public double getQuantity() { return quantity; }
    public double getPrice() { return price; }
    public double getExpectedPrice() { return expectedPrice; }
    public String getDescription() { return description; }
    public String getImageUrl() { return imageUrl; }
    public String getImagePath() { return imagePath; }
    public String getImageFilename() { return imagePath; }
} 