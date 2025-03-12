package com.projects.agroyard.fragments;

import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;
import com.projects.agroyard.R;
import com.projects.agroyard.adapters.ImageCarouselAdapter;
import com.projects.agroyard.adapters.MarketInsightAdapter;
import com.projects.agroyard.adapters.TransactionAdapter;

import com.projects.agroyard.model.Slide;
import java.util.ArrayList;
import java.util.List;



public class HomeFragment extends Fragment {
    private ViewPager2 imageCarousel;
    private RecyclerView marketInsightsRecyclerView;
    private RecyclerView transactionsRecyclerView;
    private List<Slide> slideList;
    private List<MarketInsight> insightsList;
    private List<Transaction> transactionsList;
    private Handler sliderHandler = new Handler();
    private int currentPage = 0;

    public HomeFragment() {
        // Required empty public constructor
    }

    public static HomeFragment newInstance(String param1, String param2) {
        HomeFragment fragment = new HomeFragment();
        Bundle args = new Bundle();
        args.putString("param1", param1);
        args.putString("param2", param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        imageCarousel = view.findViewById(R.id.image_carousel);
        setupCarousel();

        marketInsightsRecyclerView = view.findViewById(R.id.market_insights_recycler);
        setupMarketInsights();

        transactionsRecyclerView = view.findViewById(R.id.transactions_recycler);
        setupTransactions();

        return view;
    }

    private void setupCarousel() {
        slideList = new ArrayList<>();
        slideList.add(new Slide(1, "https://encrypted-tbn2.gstatic.com/images?q=tbn:ANd9GcRr0houfdpryn28NOIRl14_NgVxHLTWdpnfNnrAoa1nVsZ50bjd",
                "Fresh Harvest Season", "Connect with local farmers for best deals"));
        slideList.add(new Slide(2, "https://as1.ftcdn.net/jpg/02/72/67/96/1000_F_272679686_2VEUdZ69f0ZtXXyGBbPJcoxi8BAd4ikw.webp",
                "Market Analytics", "Get real-time crop pricing and forecasts"));
        slideList.add(new Slide(3, "https://images.moneycontrol.com/static-mcnews/2018/02/crops-1-770x433.jpg?impolicy=website&width=770&height=431",
                "Fast Delivery", "Farm to market in under 24 hours"));

        ImageCarouselAdapter carouselAdapter = new ImageCarouselAdapter(slideList, getContext());
        imageCarousel.setAdapter(carouselAdapter);

        startAutoScroll();
    }

    private void setupMarketInsights() {
        insightsList = new ArrayList<>();
        insightsList.add(new MarketInsight("Wheat", "₹240.50/ton", "2.5",
                "https://images.unsplash.com/photo-1574323347407-f5e1c5a1ec1a?w=200&q=80",
                "Expected to rise due to seasonal demand"));
        insightsList.add(new MarketInsight("Rice", "₹350.75/ton", "-1.2",
                "https://images.unsplash.com/photo-1586201375761-83865001e8dd?w=200&q=80",
                "Slight decrease due to oversupply"));

        MarketInsightAdapter insightAdapter = new MarketInsightAdapter(insightsList, getContext());
        marketInsightsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        marketInsightsRecyclerView.setAdapter(insightAdapter);
    }

    private void setupTransactions() {
        transactionsList = new ArrayList<>();
        transactionsList.add(new Transaction("Wheat Sale", "₹1,200.00", "5 tons", "credit"));
        transactionsList.add(new Transaction("Fertilizer Purchase", "₹450.00", "10 bags", "debit"));

        TransactionAdapter transactionAdapter = new TransactionAdapter(transactionsList, getContext());
        transactionsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        transactionsRecyclerView.setAdapter(transactionAdapter);
    }

    private void startAutoScroll() {
        final Runnable runnable = new Runnable() {
            @Override
            public void run() {
                if (currentPage == slideList.size()) {
                    currentPage = 0;
                }
                imageCarousel.setCurrentItem(currentPage++, true);
                sliderHandler.postDelayed(this, 5000);
            }
        };
        sliderHandler.postDelayed(runnable, 5000);
    }

    @Override
    public void onPause() {
        super.onPause();
        sliderHandler.removeCallbacksAndMessages(null);
    }

    @Override
    public void onResume() {
        super.onResume();
        startAutoScroll();
    }

    // Inner class for MarketInsight with getters
    public class MarketInsight {
        private String cropName;        // Made private
        private String price;
        private String changePercentage;
        private String imageUrl;
        private String description;

        public MarketInsight(String cropName, String price, String changePercentage, String imageUrl, String description) {
            this.cropName = cropName;
            this.price = price;
            this.changePercentage = changePercentage;
            this.imageUrl = imageUrl;
            this.description = description;
        }

        // Public getters
        public String getCropName() { return cropName; }
        public String getPrice() { return price; }
        public String getChangePercentage() { return changePercentage; }
        public String getImageUrl() { return imageUrl; }
        public String getDescription() { return description; }
    }

    // Inner class for Transaction with getters
    public class Transaction {
        private String transactionName;  // Made private
        private String amount;
        private String quantity;
        private String type;

        public Transaction(String transactionName, String amount, String quantity, String type) {
            this.transactionName = transactionName;
            this.amount = amount;
            this.quantity = quantity;
            this.type = type;
        }

        // Public getters
        public String getTransactionName() { return transactionName; }
        public String getAmount() { return amount; }
        public String getQuantity() { return quantity; }
        public String getType() { return type; }
    }
}