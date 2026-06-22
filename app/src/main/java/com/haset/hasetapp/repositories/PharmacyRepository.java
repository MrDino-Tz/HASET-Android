package com.haset.hasetapp.repositories;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;
import com.haset.hasetapp.firebase.FirebaseHelper;
import com.haset.hasetapp.models.CartItem;
import com.haset.hasetapp.models.PharmacyProduct;
import com.haset.hasetapp.utils.Constants;

import java.util.ArrayList;
import java.util.List;

public class PharmacyRepository {
    private final FirebaseHelper firebaseHelper = FirebaseHelper.getInstance();
    private final DatabaseReference productsRef;
    private final DatabaseReference cartRef;

    public PharmacyRepository() {
        productsRef = firebaseHelper.getDatabaseReference().child("pharmacy_products");
        cartRef = firebaseHelper.getDatabaseReference().child("carts");
    }

    public LiveData<List<PharmacyProduct>> getAllProducts() {
        MutableLiveData<List<PharmacyProduct>> productsLiveData = new MutableLiveData<>();
        
        productsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<PharmacyProduct> products = new ArrayList<>();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    PharmacyProduct product = ds.getValue(PharmacyProduct.class);
                    if (product != null) {
                        product.setProductId(ds.getKey());
                        products.add(product);
                    }
                }
                productsLiveData.postValue(products);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                productsLiveData.postValue(new ArrayList<>());
            }
        });
        
        return productsLiveData;
    }

    public LiveData<List<PharmacyProduct>> getProductsByCategory(String category) {
        MutableLiveData<List<PharmacyProduct>> productsLiveData = new MutableLiveData<>();
        
        productsRef.orderByChild("category").equalTo(category).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<PharmacyProduct> products = new ArrayList<>();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    PharmacyProduct product = ds.getValue(PharmacyProduct.class);
                    if (product != null) {
                        product.setProductId(ds.getKey());
                        products.add(product);
                    }
                }
                productsLiveData.postValue(products);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                productsLiveData.postValue(new ArrayList<>());
            }
        });
        
        return productsLiveData;
    }

    public LiveData<List<CartItem>> getCartItems(String userId) {
        MutableLiveData<List<CartItem>> cartLiveData = new MutableLiveData<>();
        
        cartRef.child(userId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<CartItem> cartItems = new ArrayList<>();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    CartItem item = ds.getValue(CartItem.class);
                    if (item != null) {
                        cartItems.add(item);
                    }
                }
                cartLiveData.postValue(cartItems);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                cartLiveData.postValue(new ArrayList<>());
            }
        });
        
        return cartLiveData;
    }

    public void addToCart(String userId, CartItem item) {
        cartRef.child(userId).child(item.getProductId()).setValue(item);
    }

    public void removeFromCart(String userId, String productId) {
        cartRef.child(userId).child(productId).removeValue();
    }

    public void updateCartItemQuantity(String userId, String productId, int quantity) {
        if (quantity <= 0) {
            removeFromCart(userId, productId);
        } else {
            cartRef.child(userId).child(productId).child("quantity").setValue(quantity);
        }
    }

    public void clearCart(String userId) {
        cartRef.child(userId).removeValue();
    }
}
