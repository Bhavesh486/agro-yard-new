package com.projects.agroyard.fragments;

import android.app.DatePickerDialog;
import android.app.ProgressDialog;
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
import com.projects.agroyard.constants.Constants;
import com.projects.agroyard.utils.CloudinaryHelper;
import com.projects.agroyard.utils.FirestoreHelper;
import com.projects.agroyard.utils.SessionManager;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class UploadProductFragment extends Fragment {
    private static final String TAG = "UploadProductFragment";

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
    private ProgressDialog progressDialog;
    
    // Session manager to get user info
    private SessionManager sessionManager;

    // Add flag for auto-registering product for bidding
    private boolean registerForBidding = true;

    private Uri selectedImageUri;
    private Calendar myCalendar = Calendar.getInstance();
    private final OkHttpClient client = new OkHttpClient();
    private static final String API_URL = Constants.DB_URL_BASE + "upload_product.php"; // Replace X with your PC's IP
    private String cloudinaryImageUrl = "";
    private String cloudinaryPublicId = "";

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

        // Initialize Cloudinary
        CloudinaryHelper.initCloudinary(requireContext());
        
        // Initialize session manager
        sessionManager = new SessionManager(requireContext());
        
        initializeViews(view);
        setupDatePicker();
        setupImagePicker();
        
        // Auto-fill farmer details from session
        autoFillFarmerDetails();

        listProductButton.setOnClickListener(v -> {
            if (validateInputs()) {
                uploadImageToCloudinary();
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
        
        // Initialize progress dialog
        progressDialog = new ProgressDialog(requireContext());
        progressDialog.setTitle("Uploading Product");
        progressDialog.setCancelable(false);

        // Set checkbox change listener
        registerForBiddingCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            registerForBidding = isChecked;
        });
    }
    
    /**
     * Auto-fill farmer name and mobile number from SessionManager
     */
    private void autoFillFarmerDetails() {
        if (sessionManager.isLoggedIn()) {
            String farmerName = sessionManager.getName();
            String farmerMobile = sessionManager.getMobile();
            
            // Only set if not empty
            if (farmerName != null && !farmerName.isEmpty()) {
                farmerNameInput.setText(farmerName);
                Log.d(TAG, "Auto-filled farmer name: " + farmerName);
            }
            
            if (farmerMobile != null && !farmerMobile.isEmpty()) {
                farmerMobileInput.setText(farmerMobile);
                Log.d(TAG, "Auto-filled farmer mobile: " + farmerMobile);
            }
        } else {
            Log.d(TAG, "User not logged in, cannot auto-fill farmer details");
        }
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

    private void uploadImageToCloudinary() {
        if (selectedImageUri == null) {
            Toast.makeText(requireContext(), "Please select a product image", Toast.LENGTH_SHORT).show();
            return;
        }

        progressDialog.setMessage("Uploading image to Cloudinary...");
        progressDialog.show();

        // Generate a unique name for the image using UUID
        String imageName = "product_" + UUID.randomUUID().toString();

        CloudinaryHelper.uploadImage(requireContext(), selectedImageUri, imageName, new CloudinaryHelper.CloudinaryUploadCallback() {
            @Override
            public void onSuccess(String url, String publicId) {
                cloudinaryImageUrl = url;
                cloudinaryPublicId = publicId;
                
                requireActivity().runOnUiThread(() -> {
                    progressDialog.setMessage("Image uploaded successfully. Submitting product details...");
                    submitProductListing();
                });
            }

            @Override
            public void onError(String message) {
                requireActivity().runOnUiThread(() -> {
                    progressDialog.dismiss();
                    Toast.makeText(requireContext(), "Error uploading image: " + message, Toast.LENGTH_LONG).show();
                });
            }

            @Override
            public void onProgress(int progress) {
                requireActivity().runOnUiThread(() -> {
                    progressDialog.setMessage("Uploading image... " + progress + "%");
                });
            }
        });
    }

    private void submitProductListing() {
        try {
            // Create a Map for Firestore
            Map<String, Object> productData = new HashMap<>();
            productData.put("farmer_name", farmerNameInput.getText().toString().trim());
            productData.put("farmer_mobile", farmerMobileInput.getText().toString().trim());
            productData.put("product_name", productNameInput.getText().toString().trim());
            productData.put("harvesting_date", harvestingDateInput.getText().toString().trim());
            productData.put("farming_type", farmingTypeSpinner.getSelectedItem().toString());
            productData.put("quantity", Double.parseDouble(quantityInput.getText().toString().trim()));
            productData.put("price", Double.parseDouble(priceInput.getText().toString().trim()));
            productData.put("expected_price", Double.parseDouble(expectedPriceInput.getText().toString().trim()));
            productData.put("description", descriptionInput.getText().toString().trim());
            
            // Add Cloudinary image information
            productData.put("image_url", cloudinaryImageUrl);
            productData.put("image_public_id", cloudinaryPublicId);
            productData.put("register_for_bidding", registerForBidding);
            
            // Add current timestamp for upload time
            long currentTime = System.currentTimeMillis();
            productData.put("timestamp", currentTime);
            
            // Set bidding to start 1 minute (60000 milliseconds) after upload
            long biddingStartTime = currentTime + 60000; // 1 minute delay
            productData.put("bidding_start_time", biddingStartTime);
            
            // Set initial bidding status to "pending"
            productData.put("bidding_status", "pending");

            // Add to Firestore
            FirestoreHelper.addProduct(productData, new FirestoreHelper.FirestoreCallback() {
                @Override
                public void onSuccess(String documentId) {
                    requireActivity().runOnUiThread(() -> {
                        progressDialog.dismiss();
                        Toast.makeText(requireContext(),
                            "Product listed successfully! Bidding will start in 1 minute.",
                            Toast.LENGTH_SHORT).show();
                        
                        // Add product ID to the map
                        productData.put("product_id", documentId);
                        
                        clearForm();

                        // Navigate back to products list to see the newly added product
                        requireActivity().getSupportFragmentManager().popBackStack();
                    });
                }

                @Override
                public void onFailure(Exception e) {
                    requireActivity().runOnUiThread(() -> {
                        progressDialog.dismiss();
                        Toast.makeText(requireContext(),
                            "Error: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                        Log.e("UploadProduct", "Firestore error: " + e.getMessage(), e);
                    });
                }
            });

        } catch (Exception e) {
            progressDialog.dismiss();
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
        cloudinaryImageUrl = "";
        cloudinaryPublicId = "";
    }
}