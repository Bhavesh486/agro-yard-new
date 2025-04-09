package com.projects.agroyard.fragments;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.projects.agroyard.R;
import com.projects.agroyard.constants.Constants;

public class MyProfileFragment extends Fragment {
    private static final String PREFS_NAME = "UserPrefs";
    private static final String TAG = "MyProfileFragment";
    
    private TextView nameTextView;
    private TextView emailTextView;
    private TextView phoneTextView;
    private TextView locationTextView;
    private TextView userTypeTextView;
    private View loadingView;
    private View contentView;
    
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private String userId;
    
    public MyProfileFragment() {
        // Required empty public constructor
    }
    
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        
        // Get current user ID if logged in
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            userId = currentUser.getUid();
            Log.d(TAG, "Current user ID: " + userId);
        } else {
            Log.d(TAG, "No current Firebase user found");
        }
    }
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_my_profile, container, false);
        
        // Initialize views
        nameTextView = view.findViewById(R.id.profile_name);
        emailTextView = view.findViewById(R.id.profile_email);
        phoneTextView = view.findViewById(R.id.profile_phone);
        locationTextView = view.findViewById(R.id.profile_location);
        userTypeTextView = view.findViewById(R.id.profile_user_type);
        loadingView = view.findViewById(R.id.loading_view);
        contentView = view.findViewById(R.id.profile_content);
        
        // Show loading initially
        loadingView.setVisibility(View.VISIBLE);
        contentView.setVisibility(View.GONE);
        
        // Set back button click listener
        view.findViewById(R.id.back_button).setOnClickListener(v -> {
            requireActivity().getSupportFragmentManager().popBackStack();
        });
        
        // Set edit profile button click listener
        view.findViewById(R.id.edit_profile_button).setOnClickListener(v -> {
            Toast.makeText(requireContext(), "Edit profile - Coming soon", Toast.LENGTH_SHORT).show();
        });
        
        // Load user profile data
        loadUserProfile();
        
        return view;
    }
    
    private void loadUserProfile() {
        if (userId != null) {
            db.collection("users").document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    loadingView.setVisibility(View.GONE);
                    contentView.setVisibility(View.VISIBLE);
                    
                    if (documentSnapshot.exists()) {
                        updateProfileWithFirestoreData(documentSnapshot);
                    } else {
                        // Fallback to shared preferences
                        loadProfileFromPrefs();
                    }
                })
                .addOnFailureListener(e -> {
                    loadingView.setVisibility(View.GONE);
                    contentView.setVisibility(View.VISIBLE);
                    
                    // Fallback to shared preferences
                    loadProfileFromPrefs();
                    Toast.makeText(requireContext(), "Error loading profile: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
        } else {
            // No Firebase user, use shared preferences
            loadingView.setVisibility(View.GONE);
            contentView.setVisibility(View.VISIBLE);
            loadProfileFromPrefs();
        }
    }
    
    private void updateProfileWithFirestoreData(DocumentSnapshot document) {
        String name = document.getString("name");
        String email = document.getString("email");
        String phone = document.getString("phone");
        String location = document.getString("location");
        String userType = document.getString("userType");
        
        if (name != null) nameTextView.setText(name);
        if (email != null) emailTextView.setText(email);
        if (phone != null) phoneTextView.setText(phone);
        if (location != null) locationTextView.setText(location);
        if (userType != null) userTypeTextView.setText(userType);
        
        // Save to SharedPreferences for future use
        SharedPreferences.Editor editor = requireActivity().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit();
        if (name != null) editor.putString("userName", name);
        if (email != null) editor.putString("userEmail", email);
        if (phone != null) editor.putString("userPhone", phone);
        if (location != null) editor.putString("userLocation", location);
        if (userType != null) editor.putString("userType", userType);
        editor.apply();
    }
    
    private void loadProfileFromPrefs() {
        SharedPreferences prefs = requireActivity().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String name = prefs.getString("userName", "User");
        String email = prefs.getString("userEmail", "Not available");
        String phone = prefs.getString("userPhone", "Not available");
        String location = prefs.getString("userLocation", "Not available");
        String userType = prefs.getString("userType", Constants.FARMER);
        
        nameTextView.setText(name);
        emailTextView.setText(email);
        phoneTextView.setText(phone);
        locationTextView.setText(location);
        userTypeTextView.setText(userType);
    }
} 