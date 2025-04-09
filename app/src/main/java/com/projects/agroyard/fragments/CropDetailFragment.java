package com.projects.agroyard.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.projects.agroyard.R;

public class CropDetailFragment extends Fragment {
    private ImageView backButton;
    private TextView cropTitle;
    private ImageView cropImage;
    private TextView temperatureText;
    private TextView humidityText;
    private TextView sunlightText;
    private TextView waterText;
    private TextView soilTypeText;
    private TextView soilPhText;
    private TextView soilNutrientsText;
    private TextView soilDepthText;
    private TextView growingSeasonText;
    private TextView descriptionText;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_crop_detail, container, false);
        
        // Initialize views
        initializeViews(view);
        
        // Set data from arguments
        setDataFromArguments();
        
        // Set back button click listener
        backButton.setOnClickListener(v -> requireActivity().getSupportFragmentManager().popBackStack());
        
        return view;
    }

    private void initializeViews(View view) {
        try {
            backButton = view.findViewById(R.id.back_button);
            cropTitle = view.findViewById(R.id.crop_title);
            cropImage = view.findViewById(R.id.crop_image);
            temperatureText = view.findViewById(R.id.temperature_text);
            humidityText = view.findViewById(R.id.humidity_text);
            sunlightText = view.findViewById(R.id.sunlight_text);
            waterText = view.findViewById(R.id.water_text);
            soilTypeText = view.findViewById(R.id.soil_type_text);
            soilPhText = view.findViewById(R.id.soil_ph_text);
            soilNutrientsText = view.findViewById(R.id.soil_nutrients_text);
            soilDepthText = view.findViewById(R.id.soil_depth_text);
            growingSeasonText = view.findViewById(R.id.growing_season_text);
            descriptionText = view.findViewById(R.id.description_text);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setDataFromArguments() {
        try {
            Bundle args = getArguments();
            if (args == null) return;
            
            // Set title
            String cropName = args.getString(CropInfoFragment.CROP_NAME);
            if (cropName != null && cropTitle != null) {
                cropTitle.setText(cropName);
            }
            
            // Set image
            int imageResId = args.getInt(CropInfoFragment.CROP_IMAGE, 0);
            if (imageResId != 0 && cropImage != null) {
                try {
                    // Try to set as an image resource
                    cropImage.setImageResource(imageResId);
                } catch (Exception e) {
                    // If it fails, try setting as a background color
                    cropImage.setImageResource(0); // Clear the image
                    cropImage.setBackgroundResource(imageResId);
                }
            }
            
            // Set weather requirements
            String temperature = args.getString("temperature");
            if (temperature != null && temperatureText != null) {
                temperatureText.setText("Temperature: " + temperature);
            }
            
            String humidity = args.getString("humidity");
            if (humidity != null && humidityText != null) {
                humidityText.setText("Humidity: " + humidity);
            }
            
            String sunlight = args.getString("sunlight");
            if (sunlight != null && sunlightText != null) {
                sunlightText.setText("Sunlight: " + sunlight);
            }
            
            String water = args.getString("water");
            if (water != null && waterText != null) {
                waterText.setText("Water: " + water);
            }
            
            // Set soil requirements
            String soilType = args.getString("soil_type");
            if (soilType != null && soilTypeText != null) {
                soilTypeText.setText("Type: " + soilType);
            }
            
            String soilPh = args.getString("soil_ph");
            if (soilPh != null && soilPhText != null) {
                soilPhText.setText("pH: " + soilPh);
            }
            
            String soilNutrients = args.getString("soil_nutrients");
            if (soilNutrients != null && soilNutrientsText != null) {
                soilNutrientsText.setText("Nutrients: " + soilNutrients);
            }
            
            String soilDepth = args.getString("soil_depth");
            if (soilDepth != null && soilDepthText != null) {
                soilDepthText.setText("Depth: " + soilDepth);
            }
            
            // Set growing season
            String growingSeason = args.getString("growing_season");
            if (growingSeason != null && growingSeasonText != null) {
                growingSeasonText.setText(growingSeason);
            }
            
            // Set description
            String description = args.getString("description");
            if (description != null && descriptionText != null) {
                descriptionText.setText(description);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
} 