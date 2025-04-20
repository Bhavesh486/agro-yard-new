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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.projects.agroyard.R;
import com.projects.agroyard.constants.Constants;
import com.projects.agroyard.models.Receipt;
import com.projects.agroyard.services.ReceiptService;

import android.widget.LinearLayout;
import android.view.ViewGroup.LayoutParams;
import android.widget.ImageView;
import java.util.ArrayList;
import java.util.List;
import java.text.NumberFormat;
import java.util.Locale;

public class ReceiptsFragment extends Fragment {
    private static final String PREFS_NAME = "UserPrefs";
    private static final String KEY_USER_TYPE = "userType";
    private static final String TAG = "ReceiptsFragment";
    
    private String userType;
    private TextView titleText;
    private TextView emptyView;
    private LinearLayout receiptsList;
    private SwipeRefreshLayout swipeRefreshLayout;
    private ReceiptService receiptService;
    
    // Add button to create test receipt
    private View createTestButton;
    
    public ReceiptsFragment() {
        // Required empty public constructor
    }
    
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Get user type from SharedPreferences
        SharedPreferences prefs = requireActivity().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        userType = prefs.getString(KEY_USER_TYPE, Constants.FARMER); // Default to Farmer if not set
        
        // Initialize receipt service
        receiptService = new ReceiptService();
    }
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_receipts, container, false);
        
        try {
            titleText = view.findViewById(R.id.title_text);
            emptyView = view.findViewById(R.id.empty_view);
            receiptsList = view.findViewById(R.id.receipts_list);
            swipeRefreshLayout = view.findViewById(R.id.swipe_refresh_layout);
            
            // Safety check for views
            if (receiptsList == null || swipeRefreshLayout == null) {
                Log.e("RECEIPT_DEBUG", "Critical views are null! Check your layout XML");
                return view;
            }
            
            // Set title based on user type
            if (userType.equals(Constants.FARMER)) {
                if (titleText != null) titleText.setText("Your Sales Receipts");
            } else {
                if (titleText != null) titleText.setText("Your Purchase Receipts");
            }
            
            // Set up swipe refresh
            swipeRefreshLayout.setOnRefreshListener(this::loadReceipts);
            
            // Safely add the button
            try {
                // Create button programmatically
                createTestButton = new android.widget.Button(requireContext());
                ((android.widget.Button) createTestButton).setText("Create Real Receipt");
                createTestButton.setOnClickListener(v -> createTestReceipt());
                
                // Add to layout before receipts list
                ViewGroup parent = (ViewGroup) receiptsList.getParent();
                if (parent != null) {
                    int index = parent.indexOfChild(receiptsList);
                    parent.addView(createTestButton, index);
                } else {
                    Log.e("RECEIPT_DEBUG", "Parent view is null, can't add button");
                }
            } catch (Exception e) {
                Log.e("RECEIPT_DEBUG", "Error adding button: " + e.getMessage());
            }
            
            // Try to load real receipts (if they exist)
            loadReceipts();
            
        } catch (Exception e) {
            Log.e("RECEIPT_DEBUG", "Error in onCreateView: " + e.getMessage(), e);
            // Show error toast
            Toast.makeText(requireContext(), "Error loading receipts view", Toast.LENGTH_SHORT).show();
        }
        
        return view;
    }
    
    private void loadReceipts() {
        try {
            // Clear existing receipts
            if (receiptsList != null) {
                receiptsList.removeAllViews();
            }
            
            // Set views to correct initial state
            if (emptyView != null) {
                emptyView.setVisibility(View.GONE);
            }
            
            // Show loading indicator
            if (swipeRefreshLayout != null) {
                swipeRefreshLayout.setRefreshing(true);
            }
            
            // Hide demo button while loading
            if (createTestButton != null) {
                createTestButton.setVisibility(View.GONE);
            }
            
            // Check if we're not attached to activity
            if (getActivity() == null || !isAdded()) {
                Log.e("RECEIPT_DEBUG", "Fragment not attached to activity");
                return;
            }
            
            // DEBUGGING: Log user info
            FirebaseAuth auth = FirebaseAuth.getInstance();
            if (auth.getCurrentUser() == null) {
                Log.e("RECEIPT_DEBUG", "User not logged in!");
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        Toast.makeText(requireContext(), "Please login first", Toast.LENGTH_SHORT).show();
                        if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(false);
                        if (emptyView != null) emptyView.setVisibility(View.VISIBLE);
                        if (createTestButton != null) createTestButton.setVisibility(View.VISIBLE);
                    });
                }
                return;
            }
            
            String userId = auth.getCurrentUser().getUid();
            Log.d("RECEIPT_DEBUG", "========= RECEIPT DEBUGGING =========");
            Log.d("RECEIPT_DEBUG", "Current user ID: " + userId);
            Log.d("RECEIPT_DEBUG", "User type: " + userType);
            
            if (userType.equals(Constants.FARMER)) {
                // Load farmer receipts
                Log.d("RECEIPT_DEBUG", "Attempting to load FARMER receipts");
                receiptService.getReceiptsForFarmer()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        if (!isAdded() || getActivity() == null) {
                            Log.e("RECEIPT_DEBUG", "Fragment detached during query");
                            return;
                        }
                        
                        Log.d("RECEIPT_DEBUG", "Query successful, document count: " + 
                              queryDocumentSnapshots.size());
                        for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                            Log.d("RECEIPT_DEBUG", "Receipt document ID: " + doc.getId());
                            Log.d("RECEIPT_DEBUG", "Receipt data: " + doc.getData());
                        }
                        
                        if (queryDocumentSnapshots.isEmpty()) {
                            Log.d("RECEIPT_DEBUG", "No real receipts found for farmer");
                            // Don't show demo receipts immediately
                            if (emptyView != null) emptyView.setVisibility(View.VISIBLE);
                            if (createTestButton != null) createTestButton.setVisibility(View.VISIBLE);
                            if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(false);
                        } else {
                            processReceipts(queryDocumentSnapshots);
                        }
                    })
                    .addOnFailureListener(e -> {
                        if (!isAdded() || getActivity() == null) return;
                        
                        Log.e("RECEIPT_DEBUG", "Query FAILED: " + e.getMessage());
                        Log.e("RECEIPT_DEBUG", "Error details: ", e);
                        handleError(e);
                    });
            } else {
                // Load member receipts
                Log.d("RECEIPT_DEBUG", "Attempting to load MEMBER receipts");
                receiptService.getReceiptsForMember()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        if (!isAdded() || getActivity() == null) {
                            Log.e("RECEIPT_DEBUG", "Fragment detached during query");
                            return;
                        }
                        
                        Log.d("RECEIPT_DEBUG", "Query successful, document count: " + 
                              queryDocumentSnapshots.size());
                        for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                            Log.d("RECEIPT_DEBUG", "Receipt document ID: " + doc.getId());
                            Log.d("RECEIPT_DEBUG", "Receipt data: " + doc.getData());
                        }
                        
                        if (queryDocumentSnapshots.isEmpty()) {
                            Log.d("RECEIPT_DEBUG", "No real receipts found for member");
                            // Don't show demo receipts immediately
                            if (emptyView != null) emptyView.setVisibility(View.VISIBLE);
                            if (createTestButton != null) createTestButton.setVisibility(View.VISIBLE);
                            if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(false);
                        } else {
                            processReceipts(queryDocumentSnapshots);
                        }
                    })
                    .addOnFailureListener(e -> {
                        if (!isAdded() || getActivity() == null) return;
                        
                        Log.e("RECEIPT_DEBUG", "Query FAILED: " + e.getMessage());
                        Log.e("RECEIPT_DEBUG", "Error details: ", e);
                        handleError(e);
                    });
            }
        } catch (Exception e) {
            Log.e("RECEIPT_DEBUG", "Critical error in loadReceipts: " + e.getMessage(), e);
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    Toast.makeText(requireContext(), "Error loading receipts: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(false);
                    if (emptyView != null) emptyView.setVisibility(View.VISIBLE);
                    if (createTestButton != null) createTestButton.setVisibility(View.VISIBLE);
                });
            }
        }
    }
    
    private void processReceipts(com.google.firebase.firestore.QuerySnapshot queryDocumentSnapshots) {
        if (getActivity() == null || !isAdded()) {
            Log.e("RECEIPT_DEBUG", "Activity is null or fragment detached, can't process receipts");
            return;
        }
        
        getActivity().runOnUiThread(() -> {
            try {
                List<Receipt> receipts = new ArrayList<>();
                
                for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                    try {
                        Receipt receipt = document.toObject(Receipt.class);
                        receipts.add(receipt);
                        Log.d("RECEIPT_DEBUG", "Receipt processed: " + receipt.getProductName() + 
                              ", ID: " + receipt.getId());
                    } catch (Exception e) {
                        Log.e("RECEIPT_DEBUG", "Error converting document: " + e.getMessage());
                        Log.e("RECEIPT_DEBUG", "Document data: " + document.getData().toString());
                    }
                }
                
                // Add receipts to the list
                if (receipts.isEmpty()) {
                    // If no receipts, show empty view
                    Log.d("RECEIPT_DEBUG", "No receipts found, showing empty view");
                    if (emptyView != null) emptyView.setVisibility(View.VISIBLE);
                } else {
                    // Add the receipts to the view
                    Log.d("RECEIPT_DEBUG", "Found " + receipts.size() + " receipts, displaying");
                    if (emptyView != null) emptyView.setVisibility(View.GONE);
                    if (receiptsList != null) {
                        for (Receipt receipt : receipts) {
                            displayReceipt(receipt);
                        }
                    }
                }
                
                // Hide loading indicator
                if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(false);
                if (createTestButton != null) createTestButton.setVisibility(View.VISIBLE);
            } catch (Exception e) {
                Log.e("RECEIPT_DEBUG", "Error processing receipts: " + e.getMessage(), e);
                if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(false);
                if (emptyView != null) emptyView.setVisibility(View.VISIBLE);
                if (createTestButton != null) createTestButton.setVisibility(View.VISIBLE);
            }
        });
    }
    
    private void handleError(Exception e) {
        if (getActivity() == null || !isAdded()) return;
        
        getActivity().runOnUiThread(() -> {
            Log.e(TAG, "Error loading receipts", e);
            Toast.makeText(requireContext(), "Error loading receipts: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            
            // Show empty view
            if (emptyView != null) emptyView.setVisibility(View.VISIBLE);
            if (createTestButton != null) createTestButton.setVisibility(View.VISIBLE);
            
            // Hide loading indicator
            if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(false);
        });
    }
    
    private void displayReceipt(Receipt receipt) {
        View receiptItem = getLayoutInflater().inflate(R.layout.item_receipt, receiptsList, false);
        
        TextView productNameView = receiptItem.findViewById(R.id.product_name);
        TextView quantityView = receiptItem.findViewById(R.id.quantity);
        TextView pricePerKgView = receiptItem.findViewById(R.id.price_per_kg);
        TextView totalPriceView = receiptItem.findViewById(R.id.total_price);
        TextView personNameView = receiptItem.findViewById(R.id.person_name);
        TextView phoneNumberView = receiptItem.findViewById(R.id.phone_number);
        
        // Format currency
        NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("en", "IN"));
        String formattedTotalPrice = currencyFormat.format(receipt.getTotalPrice()).replace("INR", "₹");
        
        // Set product information
        productNameView.setText(receipt.getProductName());
        quantityView.setText("Quantity: " + receipt.getQuantity() + "kg");
        pricePerKgView.setText("Price: ₹" + receipt.getPricePerKg() + "/kg");
        totalPriceView.setText(formattedTotalPrice);
        
        // Set person information based on user type
        if (userType.equals(Constants.FARMER)) {
            // Farmers see member (buyer) information
            personNameView.setText(receipt.getMemberName());
            phoneNumberView.setText(receipt.getMemberPhone());
        } else {
            // Members see farmer (seller) information
            personNameView.setText(receipt.getFarmerName());
            phoneNumberView.setText(receipt.getFarmerPhone());
        }
        
        receiptsList.addView(receiptItem);
    }
    
    // Fallback methods for demo purposes
    private void addFarmerReceipts() {
        // Clear any existing receipt items first
        receiptsList.removeAllViews();
        
        // Add dummy data that's clearly labeled as such
        addReceiptItem("Rice (DEMO)", 50, 50, "Demo Member", "+91 9876543210");
        addReceiptItem("Wheat (DEMO)", 100, 42, "Demo Member", "+91 9876543211");
        
        // Hide empty view if there are receipts
        if (receiptsList.getChildCount() > 0) {
            emptyView.setVisibility(View.GONE);
        }
        
        // Show toast to indicate this is demo data
        Toast.makeText(requireContext(), "Showing demo receipts (real receipts not available)", Toast.LENGTH_LONG).show();
    }
    
    private void addMemberReceipts() {
        // Clear any existing receipt items first
        receiptsList.removeAllViews();
        
        // Add dummy data that's clearly labeled as such
        addReceiptItem("Rice (DEMO)", 50, 50, "Demo Farmer", "+91 9876543212");
        addReceiptItem("Tomatoes (DEMO)", 25, 72, "Demo Farmer", "+91 9876543213");
        
        // Hide empty view if there are receipts
        if (receiptsList.getChildCount() > 0) {
            emptyView.setVisibility(View.GONE);
        }
        
        // Show toast to indicate this is demo data
        Toast.makeText(requireContext(), "Showing demo receipts (real receipts not available)", Toast.LENGTH_LONG).show();
    }
    
    private void addReceiptItem(String productName, int quantity, int pricePerKg, String personName, String phoneNumber) {
        View receiptItem = getLayoutInflater().inflate(R.layout.item_receipt, receiptsList, false);
        
        TextView productNameView = receiptItem.findViewById(R.id.product_name);
        TextView quantityView = receiptItem.findViewById(R.id.quantity);
        TextView pricePerKgView = receiptItem.findViewById(R.id.price_per_kg);
        TextView totalPriceView = receiptItem.findViewById(R.id.total_price);
        TextView personNameView = receiptItem.findViewById(R.id.person_name);
        TextView phoneNumberView = receiptItem.findViewById(R.id.phone_number);
        
        // Calculate total price
        int totalPrice = quantity * pricePerKg;
        
        // Format currency
        NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("en", "IN"));
        String formattedTotalPrice = currencyFormat.format(totalPrice).replace("INR", "₹");
        
        // Set values
        productNameView.setText(productName);
        quantityView.setText("Quantity: " + quantity + "kg");
        pricePerKgView.setText("Price: ₹" + pricePerKg + "/kg");
        totalPriceView.setText(formattedTotalPrice);
        personNameView.setText(personName);
        phoneNumberView.setText(phoneNumber);
        
        receiptsList.addView(receiptItem);
    }
    
    // Add method to create a test receipt
    private void createTestReceipt() {
        // Show loading
        swipeRefreshLayout.setRefreshing(true);
        
        // Get current user ID
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser() != null ? 
                              FirebaseAuth.getInstance().getCurrentUser().getUid() : "";
        
        if (currentUserId.isEmpty()) {
            Toast.makeText(requireContext(), "You must be logged in to create receipts", Toast.LENGTH_SHORT).show();
            swipeRefreshLayout.setRefreshing(false);
            return;
        }
        
        // Create receipt based on user type
        Receipt receipt;
        if (userType.equals(Constants.FARMER)) {
            // Create a receipt where current user is the farmer
            receipt = new Receipt(
                "testProduct" + System.currentTimeMillis(),  // unique product ID
                "Test Product (Real)",                       // product name
                75,                                         // quantity
                60,                                         // price per kg
                currentUserId,                              // farmer ID (current user)
                "Your Farm",                                // farmer name
                "+91 9876543200",                           // farmer phone
                "testMember" + System.currentTimeMillis(),  // member ID (random)
                "Test Member",                              // member name
                "+91 9876543201"                            // member phone
            );
        } else {
            // Create a receipt where current user is the member
            receipt = new Receipt(
                "testProduct" + System.currentTimeMillis(),  // unique product ID
                "Test Product (Real)",                       // product name
                75,                                         // quantity
                60,                                         // price per kg
                "testFarmer" + System.currentTimeMillis(),  // farmer ID (random)
                "Test Farmer",                              // farmer name
                "+91 9876543202",                           // farmer phone
                currentUserId,                              // member ID (current user)
                "Your Account",                             // member name
                "+91 9876543203"                            // member phone
            );
        }
        
        // Save the receipt to Firestore
        receiptService.createReceipt(receipt)
            .addOnSuccessListener(documentReference -> {
                Toast.makeText(requireContext(), 
                    "Test receipt created successfully! Pull down to refresh.",
                    Toast.LENGTH_LONG).show();
                swipeRefreshLayout.setRefreshing(false);
                
                // Log success
                Log.d("RECEIPT_DEBUG", "Test receipt created with ID: " + documentReference.getId());
            })
            .addOnFailureListener(e -> {
                Toast.makeText(requireContext(),
                    "Failed to create test receipt: " + e.getMessage(),
                    Toast.LENGTH_LONG).show();
                swipeRefreshLayout.setRefreshing(false);
                
                // Log error
                Log.e("RECEIPT_DEBUG", "Error creating test receipt", e);
            });
    }
} 