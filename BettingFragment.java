package com.projects.agroyard.fragments;

import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.bumptech.glide.Glide;
import com.projects.agroyard.R;
import com.projects.agroyard.models.Product;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class BettingFragment extends Fragment {
    private static final String TAG = "BettingFragment";
    
    private LinearLayout productCardsContainer;
    private SwipeRefreshLayout swipeRefreshLayout;
    
    private static final long INITIAL_BIDDING_TIME = 45000; // 45 seconds in milliseconds
    private static final long SUBSEQUENT_BIDDING_TIME = 10000; // 10 seconds in milliseconds
    private static final float MINIMUM_BID_INCREMENT = 1.0f; // Minimum bid increment in rupees
    
    // Maps to track product-specific timers, bid status, and bids
    private Map<Integer, CountDownTimer> productTimers = new HashMap<>();
    private Map<Integer, Boolean> productBidStatus = new HashMap<>();
    private Map<Integer, Boolean> productBiddingActive = new HashMap<>();
    private Map<Integer, TextView> productTimerViews = new HashMap<>();
    private Map<Integer, EditText> productBidInputs = new HashMap<>();
    private Map<Integer, TextView> productYourBidViews = new HashMap<>();
    
    private List<Product> productList = new ArrayList<>();
    private static final String API_URL = Constants.DB_URL_BASE + "/get_products.php"; // Replace X with your PC's IP

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_betting_new, container, false);
        
        // Initialize views
        productCardsContainer = view.findViewById(R.id.product_cards_container);
        swipeRefreshLayout = view.findViewById(R.id.swipe_refresh_layout);
        
        // Setup SwipeRefreshLayout
        swipeRefreshLayout.setOnRefreshListener(this::fetchBiddableProducts);
        
        // Fetch all products for bidding
        fetchBiddableProducts();
        
        return view;
    }
    
    private void fetchBiddableProducts() {
        swipeRefreshLayout.setRefreshing(true);
        
        OkHttpClient client = new OkHttpClient.Builder()
                .retryOnConnectionFailure(true)
                .build();
        
        Request request = new Request.Builder()
                .url(API_URL)
                .build();
                
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "Network error: " + e.getMessage(), e);
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        swipeRefreshLayout.setRefreshing(false);
                        Toast.makeText(getContext(), "Error fetching products: " + e.getMessage(), 
                            Toast.LENGTH_SHORT).show();
                    });
                }
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    Log.e(TAG, "HTTP error: " + response.code());
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            swipeRefreshLayout.setRefreshing(false);
                            Toast.makeText(getContext(), "Error: " + response.code(), 
                                Toast.LENGTH_SHORT).show();
                        });
                    }
                    return;
                }

                try {
                    if (response.body() == null) {
                        Log.e(TAG, "Empty response body");
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                swipeRefreshLayout.setRefreshing(false);
                                Toast.makeText(getContext(), "Error: Empty response from server", 
                                    Toast.LENGTH_SHORT).show();
                            });
                        }
                        return;
                    }

                    String responseData = response.body().string();
                    Log.d(TAG, "API Response: " + responseData);
                    
                    JSONObject jsonResponse = new JSONObject(responseData);
                    
                    if (jsonResponse.getBoolean("success")) {
                        JSONArray productsArray = jsonResponse.getJSONArray("products");
                        List<Product> newProducts = new ArrayList<>();
                        
                        Log.d(TAG, "Found " + productsArray.length() + " products");
                        
                        for (int i = 0; i < productsArray.length(); i++) {
                            JSONObject productJson = productsArray.getJSONObject(i);
                            
                            // Filter products that are registered for bidding
                            boolean isForBidding = productJson.optBoolean("register_for_bidding", true);
                            if (isForBidding) {
                                newProducts.add(new Product(productJson));
                            }
                        }
                        
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                productList.clear();
                                productList.addAll(newProducts);
                                displayProductsForBidding();
                                swipeRefreshLayout.setRefreshing(false);
                                
                                if (newProducts.isEmpty()) {
                                    Toast.makeText(getContext(), "No products available for bidding", 
                                        Toast.LENGTH_SHORT).show();
                                } else {
                                    Log.d(TAG, "Loaded " + newProducts.size() + " products for bidding");
                                }
                            });
                        }
                    } else {
                        final String errorMessage = jsonResponse.has("message") ? 
                            jsonResponse.optString("message", "Unknown error") : "Unknown error";
                        
                        Log.e(TAG, "API error: " + errorMessage);
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                swipeRefreshLayout.setRefreshing(false);
                                Toast.makeText(getContext(), 
                                    "Error: " + errorMessage, 
                                    Toast.LENGTH_SHORT).show();
                            });
                        }
                    }
                } catch (JSONException e) {
                    Log.e(TAG, "JSON parsing error: " + e.getMessage(), e);
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            swipeRefreshLayout.setRefreshing(false);
                            Toast.makeText(getContext(), 
                                "Error parsing data: " + e.getMessage(), 
                                Toast.LENGTH_SHORT).show();
                        });
                    }
                }
            }
        });
    }
    
    private void displayProductsForBidding() {
        // Clear existing views and timer maps
        productCardsContainer.removeAllViews();
        productTimerViews.clear();
        productBidInputs.clear();
        productYourBidViews.clear();
        
        // Cancel all running timers
        for (CountDownTimer timer : productTimers.values()) {
            if (timer != null) {
                timer.cancel();
            }
        }
        productTimers.clear();
        
        // Add product cards for each product
        for (Product product : productList) {
            View productCard = createProductBidCard(product);
            productCardsContainer.addView(productCard);
            
            // Initialize bid status
            int productId = product.getId();
            productBidStatus.put(productId, false);
            productBiddingActive.put(productId, false);
            
            // Start bidding session for this product
            startBiddingSession(productId, INITIAL_BIDDING_TIME);
        }
    }
    
    private View createProductBidCard(Product product) {
        LayoutInflater inflater = LayoutInflater.from(getContext());
        View productCard = inflater.inflate(R.layout.item_product_bid_card, productCardsContainer, false);
        
        // Get views
        TextView productNameView = productCard.findViewById(R.id.product_name);
        TextView quantityView = productCard.findViewById(R.id.product_quantity);
        TextView farmerView = productCard.findViewById(R.id.product_farmer);
        TextView basePriceView = productCard.findViewById(R.id.product_base_price);
        TextView currentBidView = productCard.findViewById(R.id.product_current_bid);
        TextView yourBidView = productCard.findViewById(R.id.product_your_bid);
        EditText bidInputView = productCard.findViewById(R.id.product_bid_input);
        Button bidButton = productCard.findViewById(R.id.product_bid_button);
        TextView timerView = productCard.findViewById(R.id.product_timer);
        ImageView productImage = productCard.findViewById(R.id.product_image);
        
        // Store references to views we need to update
        int productId = product.getId();
        productTimerViews.put(productId, timerView);
        productBidInputs.put(productId, bidInputView);
        productYourBidViews.put(productId, yourBidView);
        
        // Set product info
        productNameView.setText(product.getProductName());
        quantityView.setText(String.format("Quantity: %.0fkg", product.getQuantity()));
        farmerView.setText(String.format("Farmer: %s", product.getFarmerName()));
        basePriceView.setText(String.format("Base Price: ₹%.0f/kg", product.getPrice()));
        
        // Set current bid slightly higher than base price
        double currentBid = product.getPrice() + 2;
        currentBidView.setText(String.format("₹%.0f/kg", currentBid));
        
        // Set initial your bid
        yourBidView.setText("None/kg");
        
        // Load product image
        String imagePath = product.getImagePath();
        if (imagePath != null && !imagePath.isEmpty()) {
            String fullImageUrl = Constants.DB_URL_BASE + imagePath;
            Glide.with(this)
                 .load(fullImageUrl)
                 .placeholder(R.drawable.ic_image_placeholder)
                 .error(R.drawable.ic_image_error)
                 .into(productImage);
        }
        
        // Set bid button listener
        bidButton.setOnClickListener(v -> {
            String bidAmount = bidInputView.getText().toString().trim();
            if (!bidAmount.isEmpty()) {
                placeBid(productId, product.getProductName(), "₹" + bidAmount, yourBidView);
            } else {
                Toast.makeText(requireContext(), "Please enter a bid amount", Toast.LENGTH_SHORT).show();
            }
        });
        
        return productCard;
    }
    
    /**
     * Start the bidding session for a specific product with specified time
     */
    private void startBiddingSession(int productId, long timeInMillis) {
        productBiddingActive.put(productId, true);
        
        // Get the timer text view
        TextView timerText = productTimerViews.get(productId);
        if (timerText == null) return;
        
        // Reset visual state for new bidding session
        timerText.setTextColor(getResources().getColor(android.R.color.holo_red_light));
        
        // Cancel existing timer if any
        if (productTimers.containsKey(productId) && productTimers.get(productId) != null) {
            productTimers.get(productId).cancel();
        }
        
        // Create new timer
        CountDownTimer timer = new CountDownTimer(timeInMillis, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                // Update the timer text
                updateTimerText(productId, millisUntilFinished);
            }

            @Override
            public void onFinish() {
                // When timer completes
                if (timerText != null) {
                    timerText.setText("00:00");
                }
                productBiddingActive.put(productId, false);
                handleBiddingCompletion(productId);
            }
        }.start();
        
        // Store the timer
        productTimers.put(productId, timer);
        
        // Find the product name
        String productName = "";
        for (Product product : productList) {
            if (product.getId() == productId) {
                productName = product.getProductName();
                break;
            }
        }
        
        // Toast notification
        Toast.makeText(requireContext(), 
                "Bidding session started for " + productName + "! 45 seconds to bid.", 
                Toast.LENGTH_SHORT).show();
    }
    
    /**
     * Reset bidding timer for a specific product to 10 seconds
     */
    private void resetBiddingTimer(int productId) {
        if (productTimers.containsKey(productId) && productTimers.get(productId) != null) {
            productTimers.get(productId).cancel();
        }
        
        startBiddingSession(productId, SUBSEQUENT_BIDDING_TIME);
        
        // Find the product name
        String productName = "";
        for (Product product : productList) {
            if (product.getId() == productId) {
                productName = product.getProductName();
                break;
            }
        }
        
        Toast.makeText(requireContext(), 
                "Bidding extended for " + productName + " for 10 seconds!", 
                Toast.LENGTH_SHORT).show();
    }
    
    /**
     * Handle completion of bidding session for a specific product
     */
    private void handleBiddingCompletion(int productId) {
        // Find the product name
        String productName = "";
        for (Product product : productList) {
            if (product.getId() == productId) {
                productName = product.getProductName();
                break;
            }
        }
        
        if (productName.isEmpty()) return;
        
        if (!productBidStatus.getOrDefault(productId, false)) {
            Toast.makeText(requireContext(), productName + " is sold out!", Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(requireContext(), "Bidding for " + productName + " ended!", Toast.LENGTH_LONG).show();
        }
        
        disableBidding(productId);
    }
    
    /**
     * Update the timer text with the remaining time for a specific product
     */
    private void updateTimerText(int productId, long millisUntilFinished) {
        long seconds = TimeUnit.MILLISECONDS.toSeconds(millisUntilFinished) % 60;
        String timeLeftFormatted = String.format(Locale.getDefault(), "00:%02d", seconds);
        
        TextView timerText = productTimerViews.get(productId);
        if (timerText == null) return;
        
        timerText.setText(timeLeftFormatted);
        
        // Change color to red when less than 10 seconds left
        if (seconds < 10) {
            timerText.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
        } else {
            timerText.setTextColor(getResources().getColor(android.R.color.holo_red_light));
        }
    }
    
    /**
     * Disable bidding for a specific product when timer ends
     */
    private void disableBidding(int productId) {
        // Find the product views
        EditText bidInput = productBidInputs.get(productId);
        if (bidInput == null) return;
        
        TextView timerText = productTimerViews.get(productId);
        if (timerText == null) return;
        
        // Find the bid button
        View productCard = bidInput.getRootView();
        Button bidButton = productCard.findViewById(R.id.product_bid_button);
        if (bidButton == null) return;
        
        // Disable bidding controls
        bidButton.setEnabled(false);
        bidInput.setEnabled(false);
        timerText.setText("CLOSED");
        timerText.setTextColor(getResources().getColor(android.R.color.darker_gray));
        
        // Find the product name
        String productName = "";
        for (Product product : productList) {
            if (product.getId() == productId) {
                productName = product.getProductName();
                break;
            }
        }
        
        if (!productName.isEmpty()) {
            Toast.makeText(requireContext(), 
                    "Bidding for " + productName + " is now closed.", 
                    Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * Handle bid placement
     */
    private void placeBid(int productId, String productName, String bidAmount, TextView yourBidText) {
        try {
            // Mark this product as bid on
            productBidStatus.put(productId, true);
            
            // Remove currency symbol and parse the bid amount
            float newBid = Float.parseFloat(bidAmount.replace("₹", ""));
            
            // Get current bid from the "Your Bid" text
            String currentBidText = yourBidText.getText().toString();
            float currentBid = 0.0f;
            if (!currentBidText.equals("None/kg")) {
                currentBid = Float.parseFloat(currentBidText.replace("₹", "").replace("/kg", ""));
            }
            
            // Validate that the new bid is higher than the current bid
            if (newBid <= currentBid) {
                Toast.makeText(
                    requireContext(),
                    "Your bid must be higher than the current bid of ₹" + currentBid + "/kg",
                    Toast.LENGTH_SHORT
                ).show();
                return;
            }
            
            // Ensure the bid meets minimum increment
            if (currentBid > 0 && (newBid - currentBid) < MINIMUM_BID_INCREMENT) {
                Toast.makeText(
                    requireContext(),
                    "Bid must increase by at least ₹" + MINIMUM_BID_INCREMENT + "/kg",
                    Toast.LENGTH_SHORT
                ).show();
                return;
            }
            
            // Update the "Your Bid" text - ensure no duplicated currency symbol
            // The bidAmount might already contain ₹, so remove it first
            String cleanBidAmount = bidAmount.replace("₹", "");
            yourBidText.setText("₹" + cleanBidAmount + "/kg");
            
            Toast.makeText(
                requireContext(),
                "Bid of ₹" + cleanBidAmount + " placed for " + productName,
                Toast.LENGTH_SHORT
            ).show();
            
            // Reset the timer for this product
            resetBiddingTimer(productId);
            
        } catch (NumberFormatException e) {
            Toast.makeText(
                requireContext(),
                "Please enter a valid bid amount",
                Toast.LENGTH_SHORT
            ).show();
        }
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        // Cancel all timers to prevent memory leaks
        for (CountDownTimer timer : productTimers.values()) {
            if (timer != null) {
                timer.cancel();
            }
        }
    }
} 