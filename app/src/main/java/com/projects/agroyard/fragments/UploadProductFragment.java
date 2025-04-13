package com.projects.agroyard.fragments;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.projects.agroyard.R;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class UploadProductFragment extends Fragment {
    
    private EditText farmerNameInput;
    private EditText farmerMobileInput;
    private EditText productNameInput;
    private EditText harvestingDateInput;
    private Spinner farmingTypeSpinner;
    private EditText quantityInput;
    private EditText priceInput;
    private EditText expectedPriceInput;
    private EditText descriptionInput;
    private ImageView productImageView;
    private FrameLayout imageUploadFrame;
    private Button listProductButton;
    private CheckBox registerForBiddingCheckbox;
    
    // Add flag for auto-registering product for bidding
    private boolean registerForBidding = true;

    private Uri selectedImageUri;
    private Calendar myCalendar = Calendar.getInstance();
    private final OkHttpClient client = new OkHttpClient();
    private static final String API_URL = "http://agroyard.42web.io/agroyard/api/upload_product.php"; // Replace X with your PC's IP
    
    private final ActivityResultLauncher<String> imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    selectedImageUri = uri;
                    productImageView.setImageURI(uri);
                    productImageView.setVisibility(View.VISIBLE);
                }
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_upload_product, container, false);
        
        initializeViews(view);
        setupDatePicker();
        setupImagePicker();
        
        listProductButton.setOnClickListener(v -> {
            if (validateInputs()) {
                submitProductListing();
            }
        });
        
        return view;
    }
    
    private void initializeViews(View view) {
        farmerNameInput = view.findViewById(R.id.farmer_name_input);
        farmerMobileInput = view.findViewById(R.id.farmer_mobile_input);
        productNameInput = view.findViewById(R.id.product_name_input);
        harvestingDateInput = view.findViewById(R.id.harvesting_date_input);
        farmingTypeSpinner = view.findViewById(R.id.farming_type_spinner);
        quantityInput = view.findViewById(R.id.quantity_input);
        priceInput = view.findViewById(R.id.price_input);
        expectedPriceInput = view.findViewById(R.id.expected_price_input);
        descriptionInput = view.findViewById(R.id.description_input);
        productImageView = view.findViewById(R.id.product_image_view);
        imageUploadFrame = view.findViewById(R.id.image_upload_frame);
        listProductButton = view.findViewById(R.id.list_product_button);
        registerForBiddingCheckbox = view.findViewById(R.id.register_for_bidding_checkbox);
        
        // Set checkbox change listener
        registerForBiddingCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            registerForBidding = isChecked;
        });
    }
    
    private void setupDatePicker() {
        DatePickerDialog.OnDateSetListener date = (view, year, month, dayOfMonth) -> {
            myCalendar.set(Calendar.YEAR, year);
            myCalendar.set(Calendar.MONTH, month);
            myCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
            updateHarvestingDateLabel();
        };
        
        harvestingDateInput.setOnClickListener(v -> {
            new DatePickerDialog(requireContext(), date, 
                    myCalendar.get(Calendar.YEAR), 
                    myCalendar.get(Calendar.MONTH), 
                    myCalendar.get(Calendar.DAY_OF_MONTH)).show();
        });
    }
    
    private void updateHarvestingDateLabel() {
        String myFormat = "yyyy-MM-dd";
        SimpleDateFormat sdf = new SimpleDateFormat(myFormat, Locale.getDefault());
        harvestingDateInput.setText(sdf.format(myCalendar.getTime()));
    }
    
    private void setupImagePicker() {
        imageUploadFrame.setOnClickListener(v -> {
            imagePickerLauncher.launch("image/*");
        });
    }
    
    private boolean validateInputs() {
        boolean isValid = true;
        
        if (farmerNameInput.getText().toString().trim().isEmpty()) {
            farmerNameInput.setError("Please enter your name");
            isValid = false;
        }
        
        String mobile = farmerMobileInput.getText().toString().trim();
        if (mobile.isEmpty() || mobile.length() < 10) {
            farmerMobileInput.setError("Please enter a valid mobile number");
            isValid = false;
        }
        
        if (productNameInput.getText().toString().trim().isEmpty()) {
            productNameInput.setError("Please enter product name");
            isValid = false;
        }
        
        if (harvestingDateInput.getText().toString().trim().isEmpty()) {
            harvestingDateInput.setError("Please select harvesting date");
            isValid = false;
        }
        
        if (quantityInput.getText().toString().trim().isEmpty()) {
            quantityInput.setError("Please enter quantity");
            isValid = false;
        }
        
        if (priceInput.getText().toString().trim().isEmpty()) {
            priceInput.setError("Please enter price per kg");
            isValid = false;
        }
        
        if (expectedPriceInput.getText().toString().trim().isEmpty()) {
            expectedPriceInput.setError("Please enter expected price");
            isValid = false;
        }
        
        if (descriptionInput.getText().toString().trim().isEmpty()) {
            descriptionInput.setError("Please enter product description");
            isValid = false;
        }
        
        if (selectedImageUri == null) {
            Toast.makeText(requireContext(), "Please select a product image", Toast.LENGTH_SHORT).show();
            isValid = false;
        }
        
        return isValid;
    }
    
    private void submitProductListing() {
        try {
            // Convert image to base64
            String imageBase64 = "";
            if (selectedImageUri != null) {
                InputStream inputStream = requireContext().getContentResolver().openInputStream(selectedImageUri);
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                byte[] buffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
                byte[] imageBytes = outputStream.toByteArray();
                imageBase64 = Base64.encodeToString(imageBytes, Base64.DEFAULT);
                
                // Log the base64 data length for debugging
                Log.d("UploadProduct", "Image Base64 length: " + imageBase64.length());
            } else {
                Toast.makeText(requireContext(), 
                    "Please select a product image", 
                    Toast.LENGTH_SHORT).show();
                return;
            }

            // Create JSON object
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("farmer_name", farmerNameInput.getText().toString().trim());
            jsonObject.put("farmer_mobile", farmerMobileInput.getText().toString().trim());
            jsonObject.put("product_name", productNameInput.getText().toString().trim());
            jsonObject.put("harvesting_date", harvestingDateInput.getText().toString().trim());
            jsonObject.put("farming_type", farmingTypeSpinner.getSelectedItem().toString());
            jsonObject.put("quantity", Double.parseDouble(quantityInput.getText().toString().trim()));
            jsonObject.put("price", Double.parseDouble(priceInput.getText().toString().trim()));
            jsonObject.put("expected_price", Double.parseDouble(expectedPriceInput.getText().toString().trim()));
            jsonObject.put("description", descriptionInput.getText().toString().trim());
            jsonObject.put("image_base64", imageBase64);
            jsonObject.put("register_for_bidding", registerForBidding); // Add field to register for bidding

            // Create request
            RequestBody body = RequestBody.create(
                MediaType.parse("application/json"), 
                jsonObject.toString()
            );

            Request request = new Request.Builder()
                .url(API_URL)
                .post(body)
                .build();

            // Execute request
            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    requireActivity().runOnUiThread(() -> {
                        Toast.makeText(requireContext(), 
                            "Error: " + e.getMessage(), 
                            Toast.LENGTH_LONG).show();
                        Log.e("UploadProduct", "Network error: " + e.getMessage(), e);
                    });
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    String responseBody = response.body().string();
                    Log.d("UploadProduct", "Server response: " + responseBody);
                    
                    try {
                        JSONObject jsonResponse = new JSONObject(responseBody);
                        requireActivity().runOnUiThread(() -> {
                            if (jsonResponse.optBoolean("success")) {
                                Toast.makeText(requireContext(), 
                                    "Product listed successfully!", 
                                    Toast.LENGTH_SHORT).show();
                                clearForm();
                                
                                // Navigate back to products list to see the newly added product
                                requireActivity().getSupportFragmentManager().popBackStack();
                            } else {
                                Toast.makeText(requireContext(), 
                                    "Error: " + jsonResponse.optString("message"), 
                                    Toast.LENGTH_LONG).show();
                                
                                // Log detailed error information
                                Log.e("UploadProduct", "Upload failed: " + jsonResponse.optString("message"));
                                if (jsonResponse.has("debug_info")) {
                                    try {
                                        Log.e("UploadProduct", "Debug info: " + 
                                            jsonResponse.getJSONObject("debug_info").toString(2));
                                    } catch (JSONException e) {
                                        Log.e("UploadProduct", "Error parsing debug info", e);
                                    }
                                }
                            }
                        });
                    } catch (Exception e) {
                        requireActivity().runOnUiThread(() -> {
                            Toast.makeText(requireContext(), 
                                "Error parsing response", 
                                Toast.LENGTH_LONG).show();
                            Log.e("UploadProduct", "Parse error: " + e.getMessage(), e);
                        });
                    }
                }
            });

        } catch (Exception e) {
            Toast.makeText(requireContext(), 
                "Error: " + e.getMessage(), 
                Toast.LENGTH_LONG).show();
            Log.e("UploadProduct", "General error: " + e.getMessage(), e);
        }
    }
    
    private void clearForm() {
        farmerNameInput.setText("");
        farmerMobileInput.setText("");
        productNameInput.setText("");
        harvestingDateInput.setText("");
        farmingTypeSpinner.setSelection(0);
        quantityInput.setText("");
        priceInput.setText("");
        expectedPriceInput.setText("");
        descriptionInput.setText("");
        selectedImageUri = null;
        productImageView.setVisibility(View.GONE);
    }
} 