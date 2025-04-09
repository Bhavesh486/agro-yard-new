package com.projects.agroyard.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;

import com.projects.agroyard.R;

import java.util.ArrayList;
import java.util.List;

public class BidMonitorFragment extends Fragment {

    private LinearLayout bidsContainer;
    private TextView activeBidsCount;
    private TextView pendingBidsCount;
    private TextView totalValueText;
    private Button allFilterBtn;
    private Button activeFilterBtn;
    private Button pendingFilterBtn;
    private List<BidRecord> allBidRecords;
    private String currentFilter = "all";

    public BidMonitorFragment() {
        // Required empty public constructor
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_bid_monitor, container, false);
        
        // Find views
        bidsContainer = view.findViewById(R.id.bids_container);
        activeBidsCount = view.findViewById(R.id.active_bids_count);
        pendingBidsCount = view.findViewById(R.id.pending_bids_count);
        totalValueText = view.findViewById(R.id.total_value);
        
        // Setup filter buttons
        allFilterBtn = view.findViewById(R.id.filter_all);
        activeFilterBtn = view.findViewById(R.id.filter_active);
        pendingFilterBtn = view.findViewById(R.id.filter_pending);
        
        setupFilterListeners();
        
        // Load sample bid data
        loadBidData();
        
        return view;
    }
    
    private void setupFilterListeners() {
        allFilterBtn.setOnClickListener(v -> {
            updateFilter("all");
            updateButtonState(allFilterBtn, true);
            updateButtonState(activeFilterBtn, false);
            updateButtonState(pendingFilterBtn, false);
        });
        
        activeFilterBtn.setOnClickListener(v -> {
            updateFilter("Active");
            updateButtonState(allFilterBtn, false);
            updateButtonState(activeFilterBtn, true);
            updateButtonState(pendingFilterBtn, false);
        });
        
        pendingFilterBtn.setOnClickListener(v -> {
            updateFilter("Pending");
            updateButtonState(allFilterBtn, false);
            updateButtonState(activeFilterBtn, false);
            updateButtonState(pendingFilterBtn, true);
        });
    }
    
    private void updateButtonState(Button button, boolean selected) {
        if (selected) {
            button.setBackgroundResource(R.drawable.button_primary_bg);
            button.setTextColor(getResources().getColor(android.R.color.white));
        } else {
            button.setBackgroundResource(R.drawable.button_outline_bg);
            button.setTextColor(getResources().getColor(R.color.colorPrimary));
        }
    }
    
    private void updateFilter(String filter) {
        if (!filter.equals(currentFilter)) {
            currentFilter = filter;
            refreshBidRecordViews();
        }
    }
    
    private void refreshBidRecordViews() {
        // Clear current views
        bidsContainer.removeAllViews();
        
        // Filter records and add them again
        for (BidRecord record : allBidRecords) {
            if (currentFilter.equals("all") || record.status.equals(currentFilter)) {
                addBidRecordView(record);
            }
        }
    }
    
    private void loadBidData() {
        // Sample data - in a real app, this would come from Firestore
        allBidRecords = getSampleBidRecords();
        
        // Update statistics
        updateBidStatistics();
        
        // Display each bid record
        refreshBidRecordViews();
    }
    
    private void updateBidStatistics() {
        int activeCount = 0;
        int pendingCount = 0;
        int totalValue = 0;
        
        for (BidRecord record : allBidRecords) {
            if ("Active".equals(record.status)) {
                activeCount++;
                // Add to total value - parse the bid amount (remove commas)
                String bidValue = record.highestBid.replace(",", "");
                try {
                    int bidAmount = Integer.parseInt(bidValue);
                    // Calculate total: bid per kg * quantity in kg
                    String quantity = record.quantity.replace(" kg", "");
                    int quantityValue = Integer.parseInt(quantity);
                    totalValue += bidAmount * quantityValue;
                } catch (NumberFormatException e) {
                    // Just in case the parsing fails
                }
            } else if ("Pending".equals(record.status)) {
                pendingCount++;
            }
        }
        
        // Update UI
        activeBidsCount.setText(String.valueOf(activeCount));
        pendingBidsCount.setText(String.valueOf(pendingCount));
        totalValueText.setText("₹" + String.format("%,d", totalValue));
    }
    
    private void addBidRecordView(BidRecord record) {
        // Inflate the bid record item layout
        View recordView = getLayoutInflater().inflate(R.layout.item_bid_record, bidsContainer, false);
        
        // Set bid details
        TextView productNameView = recordView.findViewById(R.id.product_name);
        TextView quantityView = recordView.findViewById(R.id.product_quantity);
        TextView highestBidView = recordView.findViewById(R.id.highest_bid);
        TextView numberOfBidsView = recordView.findViewById(R.id.number_of_bids);
        TextView bidderNameView = recordView.findViewById(R.id.bidder_name);
        TextView bidStatusView = recordView.findViewById(R.id.bid_status);
        TextView expiresInView = recordView.findViewById(R.id.expires_in);
        
        productNameView.setText(record.productName);
        quantityView.setText(record.quantity);
        highestBidView.setText("₹" + record.highestBid + "/kg");
        numberOfBidsView.setText(record.numberOfBids + " bids");
        bidderNameView.setText("by " + record.bidderName);
        bidStatusView.setText(record.status);
        expiresInView.setText("Expires in: " + record.expiresIn);
        
        // Set bid status color based on status
        if ("Active".equals(record.status)) {
            bidStatusView.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
            bidStatusView.setBackgroundResource(R.drawable.green_rounded_background);
        } else if ("Pending".equals(record.status)) {
            bidStatusView.setTextColor(getResources().getColor(android.R.color.holo_orange_dark));
            bidStatusView.setBackgroundResource(R.drawable.orange_rounded_background);
        } else if ("Expired".equals(record.status)) {
            bidStatusView.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
            bidStatusView.setBackgroundResource(R.drawable.red_rounded_background);
        }
        
        // Click listener to view bid details
        CardView cardView = recordView.findViewById(R.id.bid_card);
        cardView.setOnClickListener(v -> {
            // In a real app, this would navigate to a detailed view of the bid
            // For now, we'll just show a toast
            if (getContext() != null) {
                android.widget.Toast.makeText(getContext(), 
                        "Viewing details for " + record.productName, 
                        android.widget.Toast.LENGTH_SHORT).show();
            }
        });
        
        // Add the view to the container
        bidsContainer.addView(recordView);
    }
    
    private List<BidRecord> getSampleBidRecords() {
        List<BidRecord> records = new ArrayList<>();
        
        // Add sample records
        records.add(new BidRecord("Premium Wheat", "500 kg", "2,500", "8", "GrainMaster Corp", "Active", "2 days"));
        records.add(new BidRecord("Organic Rice", "750 kg", "3,200", "5", "OrganicFoods Ltd", "Active", "1 day"));
        records.add(new BidRecord("Fresh Tomatoes", "200 kg", "1,800", "12", "FreshMarket", "Active", "12 hours"));
        records.add(new BidRecord("Yellow Corn", "350 kg", "1,250", "3", "CornHub Co", "Pending", "Awaiting approval"));
        records.add(new BidRecord("Red Onions", "180 kg", "950", "6", "VeggieTraders", "Expired", "Ended yesterday"));
        
        return records;
    }
    
    // Simple data class for bid records
    private static class BidRecord {
        String productName;
        String quantity;
        String highestBid;
        String numberOfBids;
        String bidderName;
        String status;
        String expiresIn;
        
        BidRecord(String productName, String quantity, String highestBid, String numberOfBids, String bidderName, String status, String expiresIn) {
            this.productName = productName;
            this.quantity = quantity;
            this.highestBid = highestBid;
            this.numberOfBids = numberOfBids;
            this.bidderName = bidderName;
            this.status = status;
            this.expiresIn = expiresIn;
        }
    }
} 