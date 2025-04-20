package com.projects.agroyard.fragments;

import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
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
import com.google.firebase.firestore.ListenerRegistration;
import com.projects.agroyard.R;
import com.projects.agroyard.constants.Constants;
import com.projects.agroyard.models.Product;
import com.projects.agroyard.models.Receipt;
import com.projects.agroyard.services.ReceiptService;
import com.projects.agroyard.utils.FirestoreHelper;
import com.projects.agroyard.utils.SessionManager;

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
    private SessionManager sessionManager;
    private String currentUserMobile;
    private String currentUserName;

    private static final long INITIAL_BIDDING_TIME = 90000; // 90 seconds (1:30 minutes) in milliseconds
    private static final long SUBSEQUENT_BIDDING_TIME = 90000; // 90 seconds (1:30 minutes) in milliseconds
    private static final float MINIMUM_BID_INCREMENT = 1.0f; // Minimum bid increment in rupees

    // Maps to track product-specific timers, bid status, and bids
    private Map<Integer, CountDownTimer> productTimers = new HashMap<>();
    private Map<Integer, Boolean> productBidStatus = new HashMap<>();
    private Map<Integer, Boolean> productBiddingActive = new HashMap<>();
    private Map<Integer, TextView> productTimerViews = new HashMap<>();
    private Map<Integer, EditText> productBidInputs = new HashMap<>();
    private Map<Integer, TextView> productYourBidViews = new HashMap<>();
    private Map<Integer, TextView> productCurrentBidViews = new HashMap<>();
    private Map<String, ListenerRegistration> bidListeners = new HashMap<>();
    private Map<String, ListenerRegistration> timerListeners = new HashMap<>();
    private Map<String, Long> productEndTimes = new HashMap<>();

    private List<Product> productList = new ArrayList<>();
    private static final String API_URL = Constants.DB_URL_BASE + "/get_products.php"; // Legacy HTTP endpoint
    private Handler mainHandler = new Handler(Looper.getMainLooper());

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_betting_new, container, false);
        
        // Initialize session manager
        sessionManager = new SessionManager(requireContext());
        currentUserMobile = sessionManager.getMobile();
        currentUserName = sessionManager.getName();
        
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
        
        // Use Firestore to get bidding products
        FirestoreHelper.getBiddingProducts(new FirestoreHelper.ProductsCallback() {
            @Override
            public void onProductsLoaded(List<Map<String, Object>> products) {
                List<Product> newProducts = new ArrayList<>();
                
                Log.d(TAG, "Found " + products.size() + " products from Firestore");
                
                for (Map<String, Object> productData : products) {
                    // Log the image-related fields for debugging
                    Log.d(TAG, "Product: " + productData.get("product_name"));
                    if (productData.containsKey("image_url")) {
                        Log.d(TAG, "  > image_url: " + productData.get("image_url"));
                    } else {
                        Log.d(TAG, "  > No image_url found");
                    }
                    
                    if (productData.containsKey("image_path")) {
                        Log.d(TAG, "  > image_path: " + productData.get("image_path"));
                    } else {
                        Log.d(TAG, "  > No image_path found");
                    }
                    
                    newProducts.add(new Product(productData));
                }
                
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        // Clear previous listeners
                        clearBidListeners();
                        clearTimerListeners();
                        
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
            }
            
            @Override
            public void onError(Exception e) {
                Log.e(TAG, "Error fetching bidding products: " + e.getMessage(), e);
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        swipeRefreshLayout.setRefreshing(false);
                        Toast.makeText(getContext(), "Error fetching products: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                        
                        // If Firestore fails, fall back to HTTP endpoint
                        fetchBiddableProductsFromHttp();
                    });
                }
            }
        });
    }
    
    /**
     * Clear all active bid listeners to prevent memory leaks
     */
    private void clearBidListeners() {
        for (ListenerRegistration listener : bidListeners.values()) {
            if (listener != null) {
                listener.remove();
            }
        }
        bidListeners.clear();
    }
    
    /**
     * Clear all active timer listeners to prevent memory leaks
     */
    private void clearTimerListeners() {
        for (ListenerRegistration listener : timerListeners.values()) {
            if (listener != null) {
                listener.remove();
            }
        }
        timerListeners.clear();
        
        // Also cancel all running timers
        for (CountDownTimer timer : productTimers.values()) {
            if (timer != null) {
                timer.cancel();
            }
        }
        productTimers.clear();
    }
    
    /**
     * Legacy method to fetch products from HTTP endpoint as fallback
     */
    private void fetchBiddableProductsFromHttp() {
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
        productCurrentBidViews.clear();
        productEndTimes.clear();

        // Sort products - ready for bidding first, then waiting
        List<Product> readyProducts = new ArrayList<>();
        List<Product> waitingProducts = new ArrayList<>();

        for (Product product : productList) {
            Map<String, Object> productData = product.getOriginalData();
            if (productData != null && productData.containsKey("bidding_start_time")) {
                // Get the bidding start time
                long biddingStartTime = 0;
                Object startTimeObj = productData.get("bidding_start_time");
                if (startTimeObj instanceof Long) {
                    biddingStartTime = (Long) startTimeObj;
                }
                
                long currentTime = System.currentTimeMillis();
                if (biddingStartTime > currentTime) {
                    // Not ready for bidding yet
                    waitingProducts.add(product);
                } else {
                    // Ready for bidding
                    readyProducts.add(product);
                }
            } else {
                // No start time specified, consider it ready
                readyProducts.add(product);
            }
        }
        
        // First add ready products
        for (Product product : readyProducts) {
            View productCard = createProductBidCard(product, false);
            productCardsContainer.addView(productCard);

            // Initialize bid status
            int productId = product.getId();
            productBidStatus.put(productId, false);
            productBiddingActive.put(productId, false);

            // Check if there's already a timer running in Firestore
            checkOrStartBiddingSession(product);
            
            // Set up real-time bid listener for this product
            setupBidListener(product);
        }
        
        // Then add waiting products
        for (Product product : waitingProducts) {
            View productCard = createProductBidCard(product, true);
            productCardsContainer.addView(productCard);
            
            // Start a waiting timer for this product
            startWaitingTimer(product);
        }
    }
    
    /**
     * Set up real-time bid listener for a specific product
     */
    private void setupBidListener(Product product) {
        String productId = product.getProductId();
        if (productId == null || productId.isEmpty()) {
            Log.w(TAG, "Cannot setup bid listener for product with empty ID");
            return;
        }
        
        // Remove existing listener if any
        if (bidListeners.containsKey(productId)) {
            bidListeners.get(productId).remove();
            bidListeners.remove(productId);
        }
        
        // Create new listener
        ListenerRegistration registration = FirestoreHelper.listenForBidUpdates(
            productId, 
            new FirestoreHelper.BidUpdateListener() {
                @Override
                public void onBidUpdated(FirestoreHelper.BidInfo bidInfo) {
                    if (getActivity() == null) return;
                    
                    getActivity().runOnUiThread(() -> {
                        // Update UI with new bid information
                        updateBidUI(product.getId(), bidInfo);
                    });
                }
                
                @Override
                public void onError(Exception e) {
                    Log.e(TAG, "Error in bid listener for product " + product.getProductName(), e);
                }
            }
        );
        
        // Store the listener registration
        bidListeners.put(productId, registration);
    }
    
    /**
     * Set up real-time timer listener for a specific product
     */
    private void setupTimerListener(Product product) {
        String productId = product.getProductId();
        if (productId == null || productId.isEmpty()) {
            Log.w(TAG, "Cannot setup timer listener for product with empty ID");
            return;
        }
        
        // Remove existing listener if any
        if (timerListeners.containsKey(productId)) {
            timerListeners.get(productId).remove();
            timerListeners.remove(productId);
        }
        
        // Create new listener
        ListenerRegistration registration = FirestoreHelper.listenForTimerUpdates(
            productId, 
            new FirestoreHelper.TimerUpdateListener() {
                @Override
                public void onTimerUpdated(long endTimeMillis) {
                    if (getActivity() == null) return;
                    
                    int internalProductId = product.getId();
                    
                    // Update the local end time
                    productEndTimes.put(productId, endTimeMillis);
                    
                    // Calculate remaining time
                    long currentTime = System.currentTimeMillis();
                    long remainingTime = endTimeMillis - currentTime;
                    
                    mainHandler.post(() -> {
                        // If the timer is active, update it
                        if (remainingTime > 0) {
                            // Cancel any existing timer
                            if (productTimers.containsKey(internalProductId)) {
                                productTimers.get(internalProductId).cancel();
                            }
                            
                            // Create a new timer with the synchronized remaining time
                            startSynchronizedTimer(internalProductId, remainingTime);
                            productBiddingActive.put(internalProductId, true);
                        } else {
                            // If timer has expired, disable bidding
                            if (productBiddingActive.getOrDefault(internalProductId, false)) {
                                productBiddingActive.put(internalProductId, false);
                                handleBiddingCompletion(internalProductId);
                            }
                        }
                    });
                }
                
                @Override
                public void onError(Exception e) {
                    Log.e(TAG, "Error in timer listener for product " + product.getProductName(), e);
                }
            }
        );
        
        // Store the listener registration
        timerListeners.put(productId, registration);
    }
    
    /**
     * Check if there's an existing bidding session or start a new one
     */
    private void checkOrStartBiddingSession(Product product) {
        String productId = product.getProductId();
        int internalProductId = product.getId();
        
        Map<String, Object> productData = product.getOriginalData();
        if (productData != null && productData.containsKey("bid_end_time")) {
            // There's an existing timer in Firestore
            Object endTimeObj = productData.get("bid_end_time");
            if (endTimeObj instanceof Long) {
                long endTimeMillis = (Long) endTimeObj;
                long currentTime = System.currentTimeMillis();
                long remainingTime = endTimeMillis - currentTime;
                
                if (remainingTime > 0) {
                    // There's still time left, start a synchronized timer
                    startSynchronizedTimer(internalProductId, remainingTime);
                    productBiddingActive.put(internalProductId, true);
                    
                    // Store the end time
                    productEndTimes.put(productId, endTimeMillis);
                    
                    // Set up timer listener to keep it synchronized
                    setupTimerListener(product);
                } else {
                    // The timer has already expired
                    handleBiddingCompletion(internalProductId);
                    productBiddingActive.put(internalProductId, false);
                }
            } else {
                // Invalid end time, start a new session
                startNewBiddingSession(product);
            }
        } else {
            // No existing timer, start a new session
            startNewBiddingSession(product);
        }
    }
    
    /**
     * Start a completely new bidding session
     */
    private void startNewBiddingSession(Product product) {
        int internalProductId = product.getId();
        String productId = product.getProductId();
        
        // Calculate the end time
        long endTime = System.currentTimeMillis() + INITIAL_BIDDING_TIME;
        
        // Store it in Firestore
        FirestoreHelper.updateBidTimerInfo(productId, endTime, new FirestoreHelper.SaveCallback() {
            @Override
            public void onSuccess() {
                // Start the timer locally
                if (getActivity() == null) return;
                
                getActivity().runOnUiThread(() -> {
                    startSynchronizedTimer(internalProductId, INITIAL_BIDDING_TIME);
                    productBiddingActive.put(internalProductId, true);
                    
                    // Store the end time
                    productEndTimes.put(productId, endTime);
                    
                    // Set up timer listener to keep it synchronized
                    setupTimerListener(product);
                    
                    // Toast notification
                    String productName = product.getProductName();
                    Toast.makeText(requireContext(),
                            "Bidding session started for " + productName + "! 1:30 minutes to bid.",
                            Toast.LENGTH_SHORT).show();
                });
            }
            
            @Override
            public void onError(Exception e) {
                Log.e(TAG, "Error starting new bidding session for " + product.getProductName(), e);
            }
        });
    }
    
    /**
     * Start a synchronized timer with the given remaining time
     */
    private void startSynchronizedTimer(int productId, long remainingTimeMillis) {
        // Get the timer text view
        TextView timerText = productTimerViews.get(productId);
        if (timerText == null) return;
        
        // Reset visual state
        timerText.setTextColor(getResources().getColor(android.R.color.holo_red_light));
        
        // Cancel existing timer if any
        if (productTimers.containsKey(productId) && productTimers.get(productId) != null) {
            productTimers.get(productId).cancel();
        }
        
        // Create new timer with the synchronized remaining time
        CountDownTimer timer = new CountDownTimer(remainingTimeMillis, 1000) {
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
                handleBiddingCompletion( productId);
            }
        }.start();
        
        // Store the timer
        productTimers.put(productId, timer);
    }

    /**
     * Update UI with new bid information
     */
    private void updateBidUI(int productId, FirestoreHelper.BidInfo bidInfo) {
        if (bidInfo.getBidAmount() == null) return;
        
        // Update current bid view
        TextView currentBidView = productCurrentBidViews.get(productId);
        if (currentBidView != null) {
            currentBidView.setText(String.format("₹%.0f/kg", bidInfo.getBidAmount()));
        }
        
        // If the bid was placed by current user, update "Your Bid" too
        if (currentUserMobile != null && currentUserMobile.equals(bidInfo.getBidderMobile())) {
            TextView yourBidView = productYourBidViews.get(productId);
            if (yourBidView != null) {
                yourBidView.setText(String.format("₹%.0f/kg", bidInfo.getBidAmount()));
            }
        }
        
        // If a new bid came in and bidding is active, the timer will be updated via the timer listener
        if (bidInfo.getTimestamp() != null && 
            bidInfo.getTimestamp() > System.currentTimeMillis() - 5000 && // Within last 5 seconds
            !currentUserMobile.equals(bidInfo.getBidderMobile())) { // Not our own bid
            
            // Show toast about the new bid
            String bidderName = bidInfo.getBidderName() != null ? 
                bidInfo.getBidderName() : "Another user";
            
            // Find product name
            String productName = "";
            for (Product product : productList) {
                if (product.getId() == productId) {
                    productName = product.getProductName();
                    break;
                }
            }
            
            Toast.makeText(
                requireContext(),
                bidderName + " placed a bid of ₹" + bidInfo.getBidAmount() + 
                    " for " + productName,
                Toast.LENGTH_SHORT
            ).show();
        }
    }

    private View createProductBidCard(Product product, boolean isWaiting) {
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
        productCurrentBidViews.put(productId, currentBidView);

        // Set product info
        productNameView.setText(product.getProductName());
        quantityView.setText(String.format("Quantity: %.0fkg", product.getQuantity()));
        farmerView.setText(String.format("Farmer: %s", product.getFarmerName()));
        basePriceView.setText(String.format("Base Price: ₹%.0f/kg", product.getPrice()));

        // If product is waiting for bidding to start, disable bidding controls
        if (isWaiting) {
            bidButton.setEnabled(false);
            bidInputView.setEnabled(false);
            
            // Get the waiting time
            Map<String, Object> productData = product.getOriginalData();
            if (productData != null && productData.containsKey("bidding_start_time")) {
                long biddingStartTime = 0;
                Object startTimeObj = productData.get("bidding_start_time");
                if (startTimeObj instanceof Long) {
                    biddingStartTime = (Long) startTimeObj;
                }
                
                long currentTime = System.currentTimeMillis();
                long waitTime = biddingStartTime - currentTime;
                
                if (waitTime > 0) {
                    long seconds = TimeUnit.MILLISECONDS.toSeconds(waitTime) % 60;
                    long minutes = TimeUnit.MILLISECONDS.toMinutes(waitTime);
                    timerView.setText(String.format(Locale.getDefault(), 
                        "Bidding starts in: %02d:%02d", minutes, seconds));
                }
            }
            
            currentBidView.setText("Waiting");
            yourBidView.setText("Waiting");
            
            // Set timer color to blue to indicate waiting
            timerView.setTextColor(getResources().getColor(android.R.color.holo_blue_dark));
        } else {
            // Set current bid (check if there's already a current_bid in the product data)
            double currentBid = product.getPrice() + 2; // Default to base price + 2
            
            // Check for existing current_bid in product data
            Map<String, Object> productData = product.getOriginalData();
            if (productData != null && productData.containsKey("current_bid")) {
                Object currentBidObj = productData.get("current_bid");
                if (currentBidObj instanceof Double) {
                    currentBid = (Double) currentBidObj;
                } else if (currentBidObj instanceof Long) {
                    currentBid = ((Long) currentBidObj).doubleValue();
                } else if (currentBidObj instanceof Integer) {
                    currentBid = ((Integer) currentBidObj).doubleValue();
                }
            }
            
            currentBidView.setText(String.format("₹%.0f/kg", currentBid));

            // Check if current user has already bid on this product
            boolean userHasBid = false;
            String userBid = "None/kg";
            
            if (productData != null && 
                productData.containsKey("bidder_mobile") && 
                currentUserMobile != null && 
                currentUserMobile.equals(productData.get("bidder_mobile"))) {
                
                userHasBid = true;
                userBid = String.format("₹%.0f/kg", currentBid);
            }
            
            yourBidView.setText(userHasBid ? userBid : "None/kg");
        }

        // Load product image
        String imagePath = product.getImagePath();
        String imageUrl = product.getImageUrl();
        
        Log.d(TAG, "Loading image for product: " + product.getProductName());
        
        // Set default placeholder
        productImage.setImageResource(R.drawable.ic_image_placeholder);
        
        if (imageUrl != null && !imageUrl.isEmpty()) {
            // Direct Cloudinary URL
            Log.d(TAG, "Loading from Cloudinary URL: " + imageUrl);
            Glide.with(requireContext())
                    .load(imageUrl)
                    .placeholder(R.drawable.ic_image_placeholder)
                    .error(R.drawable.ic_image_error)
                    .into(productImage);
        } else if (imagePath != null && !imagePath.isEmpty()) {
            // Relative path - append to base URL
            String fullImageUrl = Constants.DB_URL_BASE + imagePath;
            Log.d(TAG, "Loading from HTTP path: " + fullImageUrl);
            Glide.with(requireContext())
                    .load(fullImageUrl)
                    .placeholder(R.drawable.ic_image_placeholder)
                    .error(R.drawable.ic_image_error)
                    .into(productImage);
        } else {
            Log.w(TAG, "No image URL or path found for product: " + product.getProductName());
        }

        // Set bid button listener
        bidButton.setOnClickListener(v -> {
            String bidAmount = bidInputView.getText().toString().trim();
            if (!bidAmount.isEmpty()) {
                placeBid(product, bidAmount);
            } else {
                Toast.makeText(requireContext(), "Please enter a bid amount", Toast.LENGTH_SHORT).show();
            }
        });
        
        return productCard;
    }

    /**
     * Reset bidding timer for a specific product to 1:30 minutes
     */
    private void resetBiddingTimer(int internalProductId) {
        // Find the product with this internal ID
        Product foundProduct = null;
        for (Product product : productList) {
            if (product.getId() == internalProductId) {
                foundProduct = product;
                break;
            }
        }
        
        if (foundProduct == null) return;
        
        // Create a final copy for use in the inner class
        final Product targetProduct = foundProduct;
        
        String productId = targetProduct.getProductId();
        if (productId == null || productId.isEmpty()) return;
        
        // Calculate the new end time
        long endTime = System.currentTimeMillis() + SUBSEQUENT_BIDDING_TIME;
        
        // Update it in Firestore - the timer listeners will pick up the change
        FirestoreHelper.updateBidTimerInfo(productId, endTime, new FirestoreHelper.SaveCallback() {
            @Override
            public void onSuccess() {
                if (getActivity() == null) return;
                
                // The timer will be updated via the listener
                String productName = targetProduct.getProductName();
                getActivity().runOnUiThread(() -> {
                    Toast.makeText(requireContext(),
                            "Bidding extended for " + productName + " for 1:30 minutes!",
                            Toast.LENGTH_SHORT).show();
                });
            }
            
            @Override
            public void onError(Exception e) {
                Log.e(TAG, "Error resetting bidding timer for " + targetProduct.getProductName(), e);
            }
        });
    }

    /**
     * Handle completion of bidding session for a specific product
     */
    private void handleBiddingCompletion(int productId) {
        // Find the product
        Product productToUse = null;
        for (Product product : productList) {
            if (product.getId() == productId) {
                productToUse = product;
                break;
            }
        }

        if (productToUse == null) return;
        
        // Create a final copy for use in inner class
        final Product foundProduct = productToUse;
        
        // Get product details
        String productName = foundProduct.getProductName();
        String productIdStr = foundProduct.getProductId();

        // Check if there was a bid placed (i.e., there's a winner)
        if (productBidStatus.getOrDefault(productId, false)) {
            Toast.makeText(requireContext(), "Bidding for " + productName + " ended!", Toast.LENGTH_LONG).show();
            
            // Get the winner bid information from Firestore
            FirestoreHelper.getBidInfo(productIdStr, new FirestoreHelper.BidInfoCallback() {
                @Override
                public void onBidInfoRetrieved(FirestoreHelper.BidInfo bidInfo) {
                    if (bidInfo == null || bidInfo.getBidAmount() == null) return;
                    
                    if (getActivity() == null) return;
                    
                    // Get product quantity from product data
                    int quantity = 0;
                    Map<String, Object> productData = foundProduct.getOriginalData();
                    if (productData != null && productData.containsKey("quantity")) {
                        Object quantityObj = productData.get("quantity");
                        if (quantityObj instanceof Long) {
                            quantity = ((Long) quantityObj).intValue();
                        } else if (quantityObj instanceof Integer) {
                            quantity = (Integer) quantityObj;
                        } else if (quantityObj instanceof String) {
                            try {
                                quantity = Integer.parseInt((String) quantityObj);
                            } catch (NumberFormatException e) {
                                Log.e(TAG, "Error parsing quantity", e);
                            }
                        }
                    }
                    
                    // If quantity is not available, use a default value
                    if (quantity <= 0) {
                        quantity = 50; // Default value
                    }
                    
                    // Get farmer details from product data
                    String farmerId = "";
                    String farmerName = "";
                    String farmerPhone = "";
                    
                    if (productData != null) {
                        if (productData.containsKey("farmer_id")) {
                            farmerId = String.valueOf(productData.get("farmer_id"));
                        }
                        if (productData.containsKey("farmer_name")) {
                            farmerName = String.valueOf(productData.get("farmer_name"));
                        }
                        if (productData.containsKey("farmer_mobile")) {
                            farmerPhone = String.valueOf(productData.get("farmer_mobile"));
                        }
                    }
                    
                    // Get member (winner) details from bid info
                    String memberId = bidInfo.getBidderId() != null ? bidInfo.getBidderId() : "";
                    String memberName = bidInfo.getBidderName() != null ? bidInfo.getBidderName() : "";
                    String memberPhone = bidInfo.getBidderMobile() != null ? bidInfo.getBidderMobile() : "";
                    
                    // Create a receipt for this winning bid
                    final int bidAmount = bidInfo.getBidAmount().intValue();
                    final int finalQuantity = quantity;
                    
                    Receipt receipt = new Receipt(
                        productIdStr,
                        productName,
                        finalQuantity,
                        bidAmount,
                        farmerId,
                        farmerName,
                        farmerPhone,
                        memberId,
                        memberName,
                        memberPhone
                    );
                    
                    // Save the receipt to Firestore
                    ReceiptService receiptService = new ReceiptService();
                    receiptService.createReceipt(receipt).addOnSuccessListener(documentReference -> {
                        if (getActivity() == null) return;
                        
                        getActivity().runOnUiThread(() -> {
                            Toast.makeText(requireContext(),
                                    "Receipt created for " + memberName + "'s winning bid!",
                                    Toast.LENGTH_SHORT).show();
                        });
                    }).addOnFailureListener(e -> {
                        if (getActivity() == null) return;
                        
                        getActivity().runOnUiThread(() -> {
                            Log.e(TAG, "Error creating receipt", e);
                        });
                    });
                }
                
                @Override
                public void onError(Exception e) {
                    Log.e(TAG, "Error getting bid info for receipt creation", e);
                }
            });
        } else {
            Toast.makeText(requireContext(), productName + " is sold out!", Toast.LENGTH_LONG).show();
        }
        
        disableBidding(productId);
    }
    
    /**
     * Update the timer text with the remaining time for a specific product
     */
    private void updateTimerText(int productId, long millisUntilFinished) {
        long minutes = TimeUnit.MILLISECONDS.toMinutes(millisUntilFinished);
        long seconds = TimeUnit.MILLISECONDS.toSeconds(millisUntilFinished) % 60;
        String timeLeftFormatted = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);

        TextView timerText = productTimerViews.get(productId);
        if (timerText == null) return;

        timerText.setText(timeLeftFormatted);
        
        // Change color to red when less than 10 seconds left
        if (minutes == 0 && seconds < 10) {
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
    private void placeBid(Product product, String bidAmountStr) {
        int productId = product.getId();
        String productIdStr = product.getProductId();
        TextView yourBidText = productYourBidViews.get(productId);
        String productName = product.getProductName();
        
        try {
            // Parse bid amount
            float newBid = Float.parseFloat(bidAmountStr);

            // Get current bid from the current bid view
            TextView currentBidView = productCurrentBidViews.get(productId);
            if (currentBidView == null) return;
            
            String currentBidText = currentBidView.getText().toString();
            float currentBid = 0.0f;
            if (!currentBidText.isEmpty()) {
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

            // Mark this product as bid on
            productBidStatus.put(productId, true);
            
            // Update the UI immediately for responsiveness
            if (yourBidText != null) {
                yourBidText.setText("₹" + newBid + "/kg");
            }
            if (currentBidView != null) {
                currentBidView.setText("₹" + newBid + "/kg");
            }
            
            // Update in Firestore
            FirestoreHelper.updateProductBid(productIdStr, newBid, currentUserName, currentUserMobile, 
                new FirestoreHelper.SaveCallback() {
                    @Override
                    public void onSuccess() {
                        if (getActivity() == null) return;
                        
                        getActivity().runOnUiThread(() -> {
                            Toast.makeText(
                                requireContext(),
                                "Bid of ₹" + newBid + " placed for " + productName,
                                Toast.LENGTH_SHORT
                            ).show();
                            
                            // Clear the input field after successful bid
                            EditText bidInput = productBidInputs.get(productId);
                            if (bidInput != null) {
                                bidInput.setText("");
                            }
                            
                            // Reset timer after successful bid
                            resetBiddingTimer(productId);
                        });
                    }
                    
                    @Override
                    public void onError(Exception e) {
                        if (getActivity() == null) return;
                        
                        getActivity().runOnUiThread(() -> {
            Toast.makeText(
                requireContext(),
                                "Failed to place bid: " + e.getMessage(),
                Toast.LENGTH_SHORT
            ).show();
                            
                            Log.e(TAG, "Error updating bid in Firestore", e);
                        });
                    }
                }
            );

        } catch (NumberFormatException e) {
            Toast.makeText(
                requireContext(),
                "Please enter a valid bid amount",
                Toast.LENGTH_SHORT
            ).show();
        }
    }
    
    /**
     * Start a timer to wait until bidding starts for a product
     */
    private void startWaitingTimer(Product product) {
        int productId = product.getId();
        Map<String, Object> productData = product.getOriginalData();
        
        if (productData == null || !productData.containsKey("bidding_start_time")) {
            return;
        }
        
        // Get the bidding start time
        long biddingStartTime = 0;
        Object startTimeObj = productData.get("bidding_start_time");
        if (startTimeObj instanceof Long) {
            biddingStartTime = (Long) startTimeObj;
        }
        
        long currentTime = System.currentTimeMillis();
        long waitTime = biddingStartTime - currentTime;
        
        if (waitTime <= 0) {
            // Already ready for bidding
            refreshBiddingStatus(product);
            return;
        }
        
        // Get the timer text view
        TextView timerText = productTimerViews.get(productId);
        if (timerText == null) return;
        
        // Create a timer to count down until bidding starts
        CountDownTimer timer = new CountDownTimer(waitTime, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                long seconds = TimeUnit.MILLISECONDS.toSeconds(millisUntilFinished) % 60;
                long minutes = TimeUnit.MILLISECONDS.toMinutes(millisUntilFinished);
                timerText.setText(String.format(Locale.getDefault(), 
                    "Bidding starts in: %02d:%02d", minutes, seconds));
            }

            @Override
            public void onFinish() {
                // Refresh the fragment to show active bidding
                refreshBiddingStatus(product);
            }
        }.start();
        
        productTimers.put(productId, timer);
    }
    
    /**
     * Refresh the status of a product when it becomes ready for bidding
     */
    private void refreshBiddingStatus(Product product) {
        // Create a new product card for active bidding
        View oldCard = productCardsContainer.getChildAt(product.getId());
        if (oldCard != null) {
            productCardsContainer.removeView(oldCard);
        }
        
        View newCard = createProductBidCard(product, false);
        productCardsContainer.addView(newCard);
        
        // Initialize bid status
        int productId = product.getId();
        productBidStatus.put(productId, false);
        productBiddingActive.put(productId, false);
        
        // Check if there's already a timer running in Firestore
        checkOrStartBiddingSession(product);
        
        // Set up real-time bid listener for this product
        setupBidListener(product);
        
        // Notify user
        Toast.makeText(requireContext(), 
            "Bidding is now active for " + product.getProductName(), 
            Toast.LENGTH_SHORT).show();
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
        
        // Remove all bid listeners
        clearBidListeners();
        
        // Remove all timer listeners
        clearTimerListeners();
        
        // Clear the handler callbacks
        mainHandler.removeCallbacksAndMessages(null);
    }
} 