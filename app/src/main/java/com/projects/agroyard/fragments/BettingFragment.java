package com.projects.agroyard.fragments;

import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.projects.agroyard.R;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class BettingFragment extends Fragment {

    private EditText tomatoBidInput;
    private Button tomatoBidButton;
    private TextView tomatoYourBidText;
    private TextView tomatoTimerText;
    
    private EditText wheatBidInput;
    private Button wheatBidButton;
    private TextView wheatYourBidText;
    private TextView wheatTimerText;
    
    private static final long INITIAL_BIDDING_TIME = 45000; // 45 seconds in milliseconds
    private static final long SUBSEQUENT_BIDDING_TIME = 10000; // 10 seconds in milliseconds
    private static final float MINIMUM_BID_INCREMENT = 1.0f; // Minimum bid increment in rupees
    
    // Maps to track product-specific timers, bid status, and bids
    private Map<String, CountDownTimer> productTimers = new HashMap<>();
    private Map<String, Boolean> productBidStatus = new HashMap<>();
    private Map<String, Boolean> productBiddingActive = new HashMap<>();
    
    // Product identifiers
    private static final String TOMATO = "tomato";
    private static final String WHEAT = "wheat";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_betting, container, false);
        
        // Initialize views
        tomatoBidInput = view.findViewById(R.id.tomato_bid_input);
        tomatoBidButton = view.findViewById(R.id.tomato_bid_button);
        tomatoYourBidText = view.findViewById(R.id.tomato_your_bid_text);
        tomatoTimerText = view.findViewById(R.id.tomato_timer);
        
        wheatBidInput = view.findViewById(R.id.wheat_bid_input);
        wheatBidButton = view.findViewById(R.id.wheat_bid_button);
        wheatYourBidText = view.findViewById(R.id.wheat_your_bid_text);
        wheatTimerText = view.findViewById(R.id.wheat_timer);
        
        // Initialize timer displays
        tomatoTimerText.setText("00:45");
        wheatTimerText.setText("00:45");
        
        // Initialize bid status
        productBidStatus.put(TOMATO, false);
        productBidStatus.put(WHEAT, false);
        
        // Initialize bidding active status
        productBiddingActive.put(TOMATO, false);
        productBiddingActive.put(WHEAT, false);
        
        // Set up bid button listeners
        tomatoBidButton.setOnClickListener(v -> {
            String bidAmount = tomatoBidInput.getText().toString().trim();
            if (!bidAmount.isEmpty()) {
                placeBid("Fresh Tomatoes", "₹" + bidAmount, tomatoYourBidText);
                productBidStatus.put(TOMATO, true);
                
                // Start or restart the tomato bidding session
                startBiddingSession(TOMATO, INITIAL_BIDDING_TIME);
            } else {
                Toast.makeText(requireContext(), "Please enter a bid amount", Toast.LENGTH_SHORT).show();
            }
        });
        
        wheatBidButton.setOnClickListener(v -> {
            String bidAmount = wheatBidInput.getText().toString().trim();
            if (!bidAmount.isEmpty()) {
                placeBid("Organic Wheat", "₹" + bidAmount, wheatYourBidText);
                productBidStatus.put(WHEAT, true);
                
                // Start or restart the wheat bidding session
                startBiddingSession(WHEAT, INITIAL_BIDDING_TIME);
            } else {
                Toast.makeText(requireContext(), "Please enter a bid amount", Toast.LENGTH_SHORT).show();
            }
        });
        
        return view;
    }
    
    /**
     * Start the bidding session for a specific product with specified time
     */
    private void startBiddingSession(String product, long timeInMillis) {
        productBiddingActive.put(product, true);
        
        // Get the appropriate timer text view
        TextView timerText = (product.equals(TOMATO)) ? tomatoTimerText : wheatTimerText;
        
        // Reset visual state for new bidding session
        timerText.setTextColor(getResources().getColor(android.R.color.holo_red_light));
        
        // Cancel existing timer if any
        if (productTimers.containsKey(product) && productTimers.get(product) != null) {
            productTimers.get(product).cancel();
        }
        
        // Create new timer
        CountDownTimer timer = new CountDownTimer(timeInMillis, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                // Update the timer text
                updateTimerText(product, millisUntilFinished);
            }

            @Override
            public void onFinish() {
                // When timer completes
                if (product.equals(TOMATO)) {
                    tomatoTimerText.setText("00:00");
                } else {
                    wheatTimerText.setText("00:00");
                }
                productBiddingActive.put(product, false);
                
                handleBiddingCompletion(product);
            }
        }.start();
        
        // Store the timer
        productTimers.put(product, timer);
        
        String productName = (product.equals(TOMATO)) ? "Fresh Tomatoes" : "Organic Wheat";
        Toast.makeText(requireContext(), "Bidding session started for " + productName + "! 45 seconds to bid.", Toast.LENGTH_SHORT).show();
    }
    
    /**
     * Reset bidding timer for a specific product to 10 seconds
     */
    private void resetBiddingTimer(String product) {
        if (productTimers.containsKey(product) && productTimers.get(product) != null) {
            productTimers.get(product).cancel();
        }
        
        startBiddingSession(product, SUBSEQUENT_BIDDING_TIME);
        String productName = (product.equals(TOMATO)) ? "Fresh Tomatoes" : "Organic Wheat";
        Toast.makeText(requireContext(), "Bidding extended for " + productName + " for 10 seconds!", Toast.LENGTH_SHORT).show();
    }
    
    /**
     * Handle completion of bidding session for a specific product
     */
    private void handleBiddingCompletion(String product) {
        String productName = (product.equals(TOMATO)) ? "Fresh Tomatoes" : "Organic Wheat";
        
        if (!productBidStatus.get(product)) {
            Toast.makeText(requireContext(), productName + " are sold out!", Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(requireContext(), "Bidding for " + productName + " ended!", Toast.LENGTH_LONG).show();
        }
        
        disableBidding(product);
    }
    
    /**
     * Update the timer text with the remaining time for a specific product
     */
    private void updateTimerText(String product, long millisUntilFinished) {
        long seconds = TimeUnit.MILLISECONDS.toSeconds(millisUntilFinished) % 60;
        String timeLeftFormatted = String.format(Locale.getDefault(), "00:%02d", seconds);
        
        TextView timerText = (product.equals(TOMATO)) ? tomatoTimerText : wheatTimerText;
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
    private void disableBidding(String product) {
        if (product.equals(TOMATO)) {
            // Disable tomato bidding
            tomatoBidButton.setEnabled(false);
            tomatoBidInput.setEnabled(false);
            tomatoTimerText.setText("CLOSED");
            tomatoTimerText.setTextColor(getResources().getColor(android.R.color.darker_gray));
            Toast.makeText(requireContext(), "Bidding for Fresh Tomatoes is now closed.", Toast.LENGTH_SHORT).show();
        } else {
            // Disable wheat bidding
            wheatBidButton.setEnabled(false);
            wheatBidInput.setEnabled(false);
            wheatTimerText.setText("CLOSED");
            wheatTimerText.setTextColor(getResources().getColor(android.R.color.darker_gray));
            Toast.makeText(requireContext(), "Bidding for Organic Wheat is now closed.", Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * Handle bid placement
     */
    private void placeBid(String product, String bidAmount, TextView yourBidText) {
        try {
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
                "Bid of ₹" + cleanBidAmount + " placed for " + product,
                Toast.LENGTH_SHORT
            ).show();
            
            // Reset the timer for this product
            if (product.equals("Fresh Tomatoes")) {
                resetBiddingTimer(TOMATO);
            } else {
                resetBiddingTimer(WHEAT);
            }
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