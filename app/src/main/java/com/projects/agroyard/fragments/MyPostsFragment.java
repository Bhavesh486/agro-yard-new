package com.projects.agroyard.fragments;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.projects.agroyard.R;
import com.projects.agroyard.constants.Constants;

import java.util.ArrayList;
import java.util.List;

public class MyPostsFragment extends Fragment {
    private static final String PREFS_NAME = "UserPrefs";
    private static final String TAG = "MyPostsFragment";
    
    private LinearLayout postsContainer;
    private View loadingView;
    private TextView noPostsText;
    
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private String userId;
    
    public MyPostsFragment() {
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
        View view = inflater.inflate(R.layout.fragment_my_posts, container, false);
        
        // Initialize views
        postsContainer = view.findViewById(R.id.posts_container);
        loadingView = view.findViewById(R.id.loading_view);
        noPostsText = view.findViewById(R.id.no_posts_text);
        
        // Show loading
        loadingView.setVisibility(View.VISIBLE);
        postsContainer.setVisibility(View.GONE);
        noPostsText.setVisibility(View.GONE);
        
        // Set back button click listener
        view.findViewById(R.id.back_button).setOnClickListener(v -> {
            requireActivity().getSupportFragmentManager().popBackStack();
        });
        
        // Load posts data
        loadUserPosts();
        
        return view;
    }
    
    private void loadUserPosts() {
        Log.d(TAG, "Attempting to load user posts");
        
        // Clear existing posts
        postsContainer.removeAllViews();
        
        // Get userType for query
        SharedPreferences prefs = requireActivity().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String userType = prefs.getString("userType", Constants.FARMER);
        
        if (userId != null) {
            // Query Firestore for posts created by this user
            Query query = db.collection("posts").whereEqualTo("userId", userId);
            
            query.get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    loadingView.setVisibility(View.GONE);
                    
                    if (queryDocumentSnapshots.isEmpty()) {
                        Log.d(TAG, "No posts found for user");
                        postsContainer.setVisibility(View.GONE);
                        noPostsText.setVisibility(View.VISIBLE);
                        // If no posts found, fallback to sample posts for testing
                        loadSamplePosts(userType);
                    } else {
                        Log.d(TAG, "Posts found: " + queryDocumentSnapshots.size());
                        postsContainer.setVisibility(View.VISIBLE);
                        noPostsText.setVisibility(View.GONE);
                        
                        List<Post> posts = new ArrayList<>();
                        for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                            Post post = documentToPost(document);
                            if (post != null) {
                                posts.add(post);
                                addPostView(post);
                            }
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error getting posts: ", e);
                    loadingView.setVisibility(View.GONE);
                    Toast.makeText(getContext(), "Failed to load posts: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    
                    // Fallback to sample posts
                    loadSamplePosts(userType);
                });
        } else {
            Log.d(TAG, "No userId available, loading sample posts");
            loadingView.setVisibility(View.GONE);
            // Fallback to sample posts
            loadSamplePosts(userType);
        }
    }
    
    private Post documentToPost(DocumentSnapshot document) {
        try {
            String title = document.getString("title");
            String description = document.getString("description");
            String quantity = document.getString("quantity");
            String date = document.getString("datePosted");
            String status = document.getString("status");
            
            if (title == null) title = "Untitled Post";
            if (description == null) description = "No description available";
            if (quantity == null) quantity = "Quantity not specified";
            if (date == null) date = "Unknown date";
            if (status == null) status = "Active"; // Default status
            
            return new Post(title, description, quantity, date, status);
        } catch (Exception e) {
            Log.e(TAG, "Error parsing post document: ", e);
            return null;
        }
    }
    
    private void loadSamplePosts(String userType) {
        Log.d(TAG, "Loading sample posts for userType: " + userType);
        postsContainer.setVisibility(View.VISIBLE);
        noPostsText.setVisibility(View.GONE);
        
        List<Post> posts = getSamplePosts(userType);
        for (Post post : posts) {
            addPostView(post);
        }
    }
    
    private void addPostView(Post post) {
        View postView = getLayoutInflater().inflate(R.layout.item_post, postsContainer, false);
        
        // Set post details
        TextView titleView = postView.findViewById(R.id.post_title);
        TextView descriptionView = postView.findViewById(R.id.post_description);
        TextView dateView = postView.findViewById(R.id.post_date);
        TextView statusView = postView.findViewById(R.id.post_status);
        ImageView productImageView = postView.findViewById(R.id.product_image);
        
        titleView.setText(post.title);
        descriptionView.setText(post.description);
        dateView.setText(post.date);
        statusView.setText(post.status);
        
        // Set product image based on product type (this is a simplified approach)
        if (post.title.toLowerCase().contains("wheat")) {
            productImageView.setImageResource(R.drawable.wheat_placeholder);
        } else if (post.title.toLowerCase().contains("rice")) {
            productImageView.setImageResource(R.drawable.rice_placeholder);
        } else if (post.title.toLowerCase().contains("tomato")) {
            productImageView.setImageResource(R.drawable.tomato_placeholder);
        } else {
            productImageView.setImageResource(R.drawable.product_placeholder);
        }
        
        // Set status color based on status
        if ("Active".equalsIgnoreCase(post.status)) {
            statusView.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
            statusView.setBackgroundResource(R.drawable.green_rounded_background);
        } else if ("Pending".equalsIgnoreCase(post.status)) {
            statusView.setTextColor(getResources().getColor(android.R.color.holo_orange_dark));
            statusView.setBackgroundResource(R.drawable.orange_rounded_background);
        } else if ("Sold".equalsIgnoreCase(post.status)) {
            statusView.setTextColor(getResources().getColor(android.R.color.holo_blue_dark));
            statusView.setBackgroundResource(R.drawable.blue_rounded_background);
        }
        
        // Add action button clicks
        postView.findViewById(R.id.edit_button).setOnClickListener(v -> {
            Toast.makeText(getContext(), "Edit post: " + post.title, Toast.LENGTH_SHORT).show();
            // TODO: Implement edit functionality
        });
        
        postView.findViewById(R.id.delete_button).setOnClickListener(v -> {
            Toast.makeText(getContext(), "Delete post: " + post.title, Toast.LENGTH_SHORT).show();
            // TODO: Implement delete functionality
        });
        
        postView.findViewById(R.id.view_bids_button).setOnClickListener(v -> {
            Toast.makeText(getContext(), "View bids for: " + post.title, Toast.LENGTH_SHORT).show();
            // TODO: Implement view bids functionality
        });
        
        // Add click listener for the post
        CardView cardView = postView.findViewById(R.id.post_card);
        cardView.setOnClickListener(v -> {
            Toast.makeText(getContext(), "Viewing details for " + post.title, Toast.LENGTH_SHORT).show();
            // In a real app, navigate to post details
        });
        
        // Add the view to the container
        postsContainer.addView(postView);
    }
    
    private List<Post> getSamplePosts(String userType) {
        List<Post> posts = new ArrayList<>();
        
        if (userType.equals(Constants.FARMER)) {
            // Farmer posts are products they're selling
            posts.add(new Post(
                    "Premium Wheat - Organic",
                    "High-quality wheat grown without pesticides. Available in bulk quantities.",
                    "500 kg at ₹25/kg",
                    "April 10, 2023",
                    "Active"
            ));
            
            posts.add(new Post(
                    "Basmati Rice - Premium Quality",
                    "Aromatic basmati rice, aged for 2 years for best flavor and texture.",
                    "750 kg at ₹80/kg",
                    "March 25, 2023",
                    "Active"
            ));
            
            posts.add(new Post(
                    "Fresh Tomatoes",
                    "Locally grown, vine-ripened tomatoes. Perfect for restaurants and food processing.",
                    "200 kg at ₹40/kg",
                    "April 2, 2023",
                    "Pending"
            ));
            
            posts.add(new Post(
                    "Yellow Corn",
                    "Sweet yellow corn, freshly harvested. Ideal for animal feed or food production.",
                    "1000 kg at ₹18/kg",
                    "February 15, 2023",
                    "Sold"
            ));
        } else {
            // Member posts are inquiries or bids
            posts.add(new Post(
                    "Seeking Organic Wheat",
                    "Looking for suppliers of certified organic wheat. Need regular monthly supply.",
                    "Requirement: 200-500 kg per month",
                    "April 8, 2023",
                    "Active"
            ));
            
            posts.add(new Post(
                    "Bid: Premium Rice",
                    "Placed bid on Premium Basmati Rice from FarmerOne.",
                    "Bid: ₹85/kg for 500 kg",
                    "April 1, 2023",
                    "Pending"
            ));
            
            posts.add(new Post(
                    "Seasonal Vegetables Required",
                    "Restaurant chain looking for regular supply of seasonal vegetables.",
                    "Requirement: Various quantities",
                    "March 20, 2023",
                    "Active"
            ));
        }
        
        return posts;
    }
    
    // Simple data class for posts
    private static class Post {
        String title;
        String description;
        String quantity;
        String date;
        String status;
        
        Post(String title, String description, String quantity, String date, String status) {
            this.title = title;
            this.description = description;
            this.quantity = quantity;
            this.date = date;
            this.status = status;
        }
    }
} 