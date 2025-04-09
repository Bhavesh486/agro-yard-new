package com.projects.agroyard.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.projects.agroyard.R;

public class NotificationsFragment extends Fragment {

    private ImageView backButton;
    private TextView markAllReadButton;
    private CardView biddingSection;

    public NotificationsFragment() {
        // Required empty public constructor
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_notifications, container, false);
        
        // Initialize views
        backButton = view.findViewById(R.id.back_button);
        markAllReadButton = view.findViewById(R.id.mark_all_read);
        
        // Set up click listeners
        backButton.setOnClickListener(v -> {
            // Go back to previous screen
            requireActivity().getSupportFragmentManager().popBackStack();
        });
        
        markAllReadButton.setOnClickListener(v -> {
            // Mark all notifications as read (in a real app, this would update a database)
            Toast.makeText(requireContext(), "All notifications marked as read", Toast.LENGTH_SHORT).show();
        });
        
        // Set up bidding section click listeners
        setupBiddingNotifications(view);
        
        return view;
    }
    
    /**
     * Set up click listeners for the bidding notifications
     */
    private void setupBiddingNotifications(View view) {
        // Find all the bidding notification items - in this example we're using the parent LinearLayouts
        // This assumes the layout structure from fragment_notifications.xml
        
        // For a real app, you would likely have a RecyclerView with adapters instead of hard-coded views
        
        // Find each notification item by its index in the CardView's LinearLayout
        // This is a simplification - in a real app with dynamic content, you would use IDs or a RecyclerView
        try {
            CardView biddingCard = (CardView) ((ViewGroup) view).findViewWithTag("bidding_section");
            if (biddingCard != null) {
                ViewGroup biddingLayout = (ViewGroup) biddingCard.getChildAt(0);
                
                // Tomato bidding notification (first child)
                biddingLayout.getChildAt(0).setOnClickListener(v -> {
                    navigateToBidding("Fresh Tomatoes");
                });
                
                // Wheat bidding notification (third child, after the divider)
                biddingLayout.getChildAt(2).setOnClickListener(v -> {
                    navigateToBidding("Organic Wheat");
                });
                
                // Rice bidding notification (fifth child, after the divider)
                biddingLayout.getChildAt(4).setOnClickListener(v -> {
                    navigateToBidding("Basmati Rice");
                });
            }
        } catch (Exception e) {
            // Fallback - when the view structure doesn't match expectations
            // Setup a click listener on the whole bidding card
            View fallbackBiddingSection = view.findViewById(R.id.bidding_section);
            if (fallbackBiddingSection != null) {
                fallbackBiddingSection.setOnClickListener(v -> {
                    navigateToBidding(null);
                });
            }
        }
    }
    
    /**
     * Navigate to the BettingFragment with the selected product
     */
    private void navigateToBidding(String productName) {
        // In a real app, you would pass the product name to the BettingFragment
        BettingFragment bettingFragment = new BettingFragment();
        
        if (productName != null) {
            Bundle args = new Bundle();
            args.putString("product_name", productName);
            bettingFragment.setArguments(args);
            
            Toast.makeText(requireContext(), "Viewing " + productName + " bidding", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(requireContext(), "Viewing all bidding activities", Toast.LENGTH_SHORT).show();
        }
        
        // Navigate to the betting fragment
        FragmentTransaction transaction = requireActivity().getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container, bettingFragment);
        transaction.addToBackStack(null);
        transaction.commit();
    }
} 