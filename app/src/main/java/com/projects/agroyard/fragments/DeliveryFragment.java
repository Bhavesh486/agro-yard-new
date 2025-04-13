package com.projects.agroyard.fragments;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.projects.agroyard.R;
import com.projects.agroyard.constants.Constants;
import com.projects.agroyard.utils.SessionManager;

import java.util.UUID;

public class DeliveryFragment extends Fragment {
    
    private static final String PREF_DELIVERY_ADDRESSES = "DeliveryAddresses";
    
    private EditText nameInput;
    private EditText addressLine1Input;
    private EditText addressLine2Input;
    private EditText cityInput;
    private EditText stateInput;
    private EditText pinCodeInput;
    private EditText phoneNumberInput;
    private EditText alternatePhoneInput;
    private EditText landmarkInput;
    private Button saveButton;
    private ImageView backButton;
    
    private SessionManager sessionManager;
    private SharedPreferences sharedPreferences;
    
    public DeliveryFragment() {
        // Required empty public constructor
    }
    
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sessionManager = new SessionManager(requireContext());
        sharedPreferences = requireActivity().getSharedPreferences(PREF_DELIVERY_ADDRESSES, Context.MODE_PRIVATE);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_delivery, container, false);
        
        // Initialize views
        initializeViews(view);
        
        // Check if user is a member
        if (!Constants.MEMBER.equals(sessionManager.getUserType())) {
            Toast.makeText(requireContext(), "Only members can add delivery addresses", Toast.LENGTH_LONG).show();
            requireActivity().getSupportFragmentManager().popBackStack();
            return view;
        }
        
        // Load existing address if available
        loadSavedAddress();
        
        // Setup back button
        backButton.setOnClickListener(v -> requireActivity().getSupportFragmentManager().popBackStack());
        
        // Setup save button
        saveButton.setOnClickListener(v -> {
            if (validateInputs()) {
                saveAddress();
            }
        });
        
        return view;
    }
    
    private void initializeViews(View view) {
        nameInput = view.findViewById(R.id.input_full_name);
        addressLine1Input = view.findViewById(R.id.input_address_line1);
        addressLine2Input = view.findViewById(R.id.input_address_line2);
        cityInput = view.findViewById(R.id.input_city);
        stateInput = view.findViewById(R.id.input_state);
        pinCodeInput = view.findViewById(R.id.input_pincode);
        phoneNumberInput = view.findViewById(R.id.input_phone);
        alternatePhoneInput = view.findViewById(R.id.input_alternate_phone);
        landmarkInput = view.findViewById(R.id.input_landmark);
        saveButton = view.findViewById(R.id.button_save_address);
        backButton = view.findViewById(R.id.back_button);
    }
    
    private boolean validateInputs() {
        boolean isValid = true;
        
        // Validate required fields
        if (TextUtils.isEmpty(nameInput.getText())) {
            nameInput.setError("Full name is required");
            isValid = false;
        }
        
        if (TextUtils.isEmpty(addressLine1Input.getText())) {
            addressLine1Input.setError("Address is required");
            isValid = false;
        }
        
        if (TextUtils.isEmpty(cityInput.getText())) {
            cityInput.setError("City is required");
            isValid = false;
        }
        
        if (TextUtils.isEmpty(stateInput.getText())) {
            stateInput.setError("State is required");
            isValid = false;
        }
        
        if (TextUtils.isEmpty(pinCodeInput.getText())) {
            pinCodeInput.setError("PIN code is required");
            isValid = false;
        } else if (pinCodeInput.getText().length() != 6) {
            pinCodeInput.setError("PIN code must be 6 digits");
            isValid = false;
        }
        
        if (TextUtils.isEmpty(phoneNumberInput.getText())) {
            phoneNumberInput.setError("Phone number is required");
            isValid = false;
        } else if (phoneNumberInput.getText().length() < 10) {
            phoneNumberInput.setError("Enter a valid phone number");
            isValid = false;
        }
        
        return isValid;
    }
    
    private void saveAddress() {
        // Get the user ID
        String userId = sessionManager.getUserId();
        if (TextUtils.isEmpty(userId)) {
            userId = UUID.randomUUID().toString(); // Generate a unique ID if not available
        }
        
        // Create address JSON
        String addressJson = "{" +
                "\"name\":\"" + nameInput.getText().toString() + "\"," +
                "\"addressLine1\":\"" + addressLine1Input.getText().toString() + "\"," +
                "\"addressLine2\":\"" + addressLine2Input.getText().toString() + "\"," +
                "\"city\":\"" + cityInput.getText().toString() + "\"," +
                "\"state\":\"" + stateInput.getText().toString() + "\"," +
                "\"pinCode\":\"" + pinCodeInput.getText().toString() + "\"," +
                "\"phone\":\"" + phoneNumberInput.getText().toString() + "\"," +
                "\"alternatePhone\":\"" + alternatePhoneInput.getText().toString() + "\"," +
                "\"landmark\":\"" + landmarkInput.getText().toString() + "\"" +
                "}";
        
        // Save to SharedPreferences
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(userId, addressJson);
        editor.apply();
        
        Toast.makeText(requireContext(), "Delivery address saved successfully", Toast.LENGTH_SHORT).show();
        
        // Go back to previous screen
        requireActivity().getSupportFragmentManager().popBackStack();
    }
    
    private void loadSavedAddress() {
        // Get the user ID
        String userId = sessionManager.getUserId();
        if (TextUtils.isEmpty(userId)) {
            return; // No user ID available
        }
        
        // Load address
        String addressJson = sharedPreferences.getString(userId, null);
        if (addressJson == null) {
            return; // No saved address
        }
        
        try {
            // Simple parsing of the JSON (using a proper JSON library would be better)
            String name = extractValue(addressJson, "name");
            String addressLine1 = extractValue(addressJson, "addressLine1");
            String addressLine2 = extractValue(addressJson, "addressLine2");
            String city = extractValue(addressJson, "city");
            String state = extractValue(addressJson, "state");
            String pinCode = extractValue(addressJson, "pinCode");
            String phone = extractValue(addressJson, "phone");
            String alternatePhone = extractValue(addressJson, "alternatePhone");
            String landmark = extractValue(addressJson, "landmark");
            
            // Populate the form
            nameInput.setText(name);
            addressLine1Input.setText(addressLine1);
            addressLine2Input.setText(addressLine2);
            cityInput.setText(city);
            stateInput.setText(state);
            pinCodeInput.setText(pinCode);
            phoneNumberInput.setText(phone);
            alternatePhoneInput.setText(alternatePhone);
            landmarkInput.setText(landmark);
        } catch (Exception e) {
            // Handle parsing error
            Toast.makeText(requireContext(), "Error loading saved address", Toast.LENGTH_SHORT).show();
        }
    }
    
    private String extractValue(String json, String key) {
        int keyIndex = json.indexOf("\"" + key + "\":\"");
        if (keyIndex == -1) return "";
        
        int valueStartIndex = json.indexOf("\":\"", keyIndex) + 3;
        int valueEndIndex = json.indexOf("\"", valueStartIndex);
        
        return json.substring(valueStartIndex, valueEndIndex);
    }
} 