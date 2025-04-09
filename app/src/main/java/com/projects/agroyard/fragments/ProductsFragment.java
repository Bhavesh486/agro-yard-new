package com.projects.agroyard.fragments;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.projects.agroyard.R;
import com.projects.agroyard.adapters.ProductAdapter;
import com.projects.agroyard.models.Product;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class ProductsFragment extends Fragment {
    private static final String TAG = "ProductsFragment";
    private RecyclerView recyclerView;
    private ProductAdapter productAdapter;
    private SwipeRefreshLayout swipeRefreshLayout;
    private List<Product> productList = new ArrayList<>();
    private static final String API_URL = "http://10.0.2.2/agroyard/api/get_products.php";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_products, container, false);

        try {
            // Initialize views
            recyclerView = view.findViewById(R.id.products_recycler_view);
            swipeRefreshLayout = view.findViewById(R.id.swipe_refresh_layout);
            
            // Setup RecyclerView
            recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
            productAdapter = new ProductAdapter(getContext(), productList, this::onProductClick);
            recyclerView.setAdapter(productAdapter);

            // Setup SwipeRefreshLayout
            swipeRefreshLayout.setOnRefreshListener(this::fetchProducts);

            // Initial fetch
            fetchProducts();
        } catch (Exception e) {
            Log.e(TAG, "Error initializing Products: " + e.getMessage(), e);
            Toast.makeText(getContext(), "Error initializing Products: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }

        return view;
    }

    private void fetchProducts() {
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
                requireActivity().runOnUiThread(() -> {
                    swipeRefreshLayout.setRefreshing(false);
                    Toast.makeText(getContext(), "Error fetching products: " + e.getMessage(), 
                        Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    Log.e(TAG, "HTTP error: " + response.code());
                    requireActivity().runOnUiThread(() -> {
                        swipeRefreshLayout.setRefreshing(false);
                        Toast.makeText(getContext(), "Error: " + response.code(), 
                            Toast.LENGTH_SHORT).show();
                    });
                    return;
                }

                try {
                    if (response.body() == null) {
                        Log.e(TAG, "Empty response body");
                        requireActivity().runOnUiThread(() -> {
                            swipeRefreshLayout.setRefreshing(false);
                            Toast.makeText(getContext(), "Error: Empty response from server", 
                                Toast.LENGTH_SHORT).show();
                        });
                        return;
                    }

                    String responseData = response.body().string();
                    Log.d(TAG, "API Response: " + responseData);
                    
                    if (responseData.isEmpty()) {
                        Log.e(TAG, "Empty response data");
                        requireActivity().runOnUiThread(() -> {
                            swipeRefreshLayout.setRefreshing(false);
                            Toast.makeText(getContext(), "Error: Empty response data", 
                                Toast.LENGTH_SHORT).show();
                        });
                        return;
                    }

                    JSONObject jsonResponse = new JSONObject(responseData);
                    
                    if (jsonResponse.getBoolean("success")) {
                        JSONArray productsArray = jsonResponse.getJSONArray("products");
                        List<Product> newProducts = new ArrayList<>();
                        
                        Log.d(TAG, "Found " + productsArray.length() + " products");
                        
                        for (int i = 0; i < productsArray.length(); i++) {
                            JSONObject productJson = productsArray.getJSONObject(i);
                            
                            // Enhanced logging of product data including images
                            StringBuilder productInfo = new StringBuilder();
                            productInfo.append("Product #").append(i + 1).append(": ");
                            
                            if (productJson.has("id")) {
                                productInfo.append("ID=").append(productJson.getInt("id")).append(", ");
                            }
                            
                            if (productJson.has("product_name")) {
                                productInfo.append("Name=").append(productJson.getString("product_name")).append(", ");
                            }
                            
                            // Check for image_path (from your database)
                            if (productJson.has("image_path")) {
                                String imagePath = productJson.getString("image_path");
                                productInfo.append("image_path=").append(imagePath);
                                Log.d(TAG, "Product #" + (i + 1) + " has image_path: " + imagePath);
                            } else {
                                productInfo.append("NO image_path");
                                Log.w(TAG, "Product #" + (i + 1) + " is missing image_path field");
                            }
                            
                            // Also check for older image fields for backwards compatibility
                            if (productJson.has("image_url")) {
                                Log.d(TAG, "Product #" + (i + 1) + " also has image_url: " + productJson.getString("image_url"));
                            }
                            
                            if (productJson.has("image_filename")) {
                                Log.d(TAG, "Product #" + (i + 1) + " also has image_filename: " + productJson.getString("image_filename"));
                            }
                            
                            Log.d(TAG, productInfo.toString());
                            
                            // Create the product object which will handle all the image fields correctly
                            newProducts.add(new Product(productJson));
                        }
                        
                        requireActivity().runOnUiThread(() -> {
                            productList.clear();
                            productList.addAll(newProducts);
                            productAdapter.notifyDataSetChanged();
                            swipeRefreshLayout.setRefreshing(false);
                            
                            if (newProducts.isEmpty()) {
                                Toast.makeText(getContext(), "No products available", 
                                    Toast.LENGTH_SHORT).show();
                            } else {
                                Log.d(TAG, "Updated UI with " + newProducts.size() + " products");
                            }
                        });
                    } else {
                        final String errorMessage = jsonResponse.has("message") ? 
                            jsonResponse.optString("message", "Unknown error") : "Unknown error";
                        
                        Log.e(TAG, "API error: " + errorMessage);
                        requireActivity().runOnUiThread(() -> {
                            swipeRefreshLayout.setRefreshing(false);
                            Toast.makeText(getContext(), 
                                "Error: " + errorMessage, 
                                Toast.LENGTH_SHORT).show();
                        });
                    }
                } catch (JSONException e) {
                    Log.e(TAG, "JSON parsing error: " + e.getMessage(), e);
                    requireActivity().runOnUiThread(() -> {
                        swipeRefreshLayout.setRefreshing(false);
                        Toast.makeText(getContext(), 
                            "Error parsing response: " + e.getMessage(), 
                            Toast.LENGTH_SHORT).show();
                    });
                }
            }
        });
    }

    private void onProductClick(Product product) {
        // Navigate to product details fragment
        ProductDetailFragment detailFragment = ProductDetailFragment.newInstance(product);
        requireActivity().getSupportFragmentManager()
            .beginTransaction()
            .replace(R.id.fragment_container, detailFragment)
            .addToBackStack(null)
            .commit();
    }
}