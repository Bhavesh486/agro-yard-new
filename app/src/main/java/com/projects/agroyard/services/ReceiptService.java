package com.projects.agroyard.services;

import android.util.Log;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.projects.agroyard.models.Receipt;

public class ReceiptService {
    private static final String COLLECTION_RECEIPTS = "receipts";
    private static final String TAG = "ReceiptService";
    private final FirebaseFirestore db;
    private final FirebaseAuth auth;

    public ReceiptService() {
        this.db = FirebaseFirestore.getInstance();
        this.auth = FirebaseAuth.getInstance();
    }

    // Create a new receipt
    public Task<DocumentReference> createReceipt(Receipt receipt) {
        Log.d("RECEIPT_DEBUG", "Creating new receipt for product: " + receipt.getProductName());
        return db.collection(COLLECTION_RECEIPTS).add(receipt)
            .addOnSuccessListener(documentReference -> {
                Log.d("RECEIPT_DEBUG", "Receipt created with ID: " + documentReference.getId());
            })
            .addOnFailureListener(e -> {
                Log.e("RECEIPT_DEBUG", "Error creating receipt: " + e.getMessage());
            });
    }

    // Get all receipts for the current farmer
    public Task<QuerySnapshot> getReceiptsForFarmer() {
        String currentUserId = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : "";
        Log.d("RECEIPT_DEBUG", "Getting receipts for farmer ID: " + currentUserId);
        
        if (currentUserId.isEmpty()) {
            Log.e("RECEIPT_DEBUG", "Cannot get receipts: Current user ID is empty");
        }
        
        // Query without ordering by timestamp to avoid issues
        return db.collection(COLLECTION_RECEIPTS)
                .whereEqualTo("farmerId", currentUserId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d("RECEIPT_DEBUG", "Farmer receipts query completed successfully");
                    } else {
                        Log.e("RECEIPT_DEBUG", "Farmer receipts query failed: " + 
                              (task.getException() != null ? task.getException().getMessage() : "Unknown error"));
                    }
                });
    }

    // Get all receipts for the current member
    public Task<QuerySnapshot> getReceiptsForMember() {
        String currentUserId = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : "";
        Log.d("RECEIPT_DEBUG", "Getting receipts for member ID: " + currentUserId);
        
        if (currentUserId.isEmpty()) {
            Log.e("RECEIPT_DEBUG", "Cannot get receipts: Current user ID is empty");
        }
        
        // Query without ordering by timestamp to avoid issues
        return db.collection(COLLECTION_RECEIPTS)
                .whereEqualTo("memberId", currentUserId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d("RECEIPT_DEBUG", "Member receipts query completed successfully");
                    } else {
                        Log.e("RECEIPT_DEBUG", "Member receipts query failed: " + 
                              (task.getException() != null ? task.getException().getMessage() : "Unknown error"));
                    }
                });
    }

    // Get a receipt by ID
    public Task<QuerySnapshot> getReceiptById(String receiptId) {
        return db.collection(COLLECTION_RECEIPTS)
                .whereEqualTo("id", receiptId)
                .get();
    }

    // Update a receipt's status
    public Task<Void> updateReceiptStatus(String receiptId, String status) {
        return db.collection(COLLECTION_RECEIPTS)
                .document(receiptId)
                .update("status", status);
    }
} 