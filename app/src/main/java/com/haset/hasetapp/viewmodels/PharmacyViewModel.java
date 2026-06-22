package com.haset.hasetapp.viewmodels;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.haset.hasetapp.models.CartItem;
import com.haset.hasetapp.models.PharmacyProduct;
import com.haset.hasetapp.repositories.PharmacyRepository;

import java.util.ArrayList;
import java.util.List;

public class PharmacyViewModel extends AndroidViewModel {
    private final PharmacyRepository repository;
    private LiveData<List<PharmacyProduct>> allProducts;
    private LiveData<List<CartItem>> cartItems;
    private final MutableLiveData<String> currentUserId = new MutableLiveData<>();
    private final MutableLiveData<String> searchQuery = new MutableLiveData<>("");

    public PharmacyViewModel(@NonNull Application application) {
        super(application);
        repository = new PharmacyRepository();
    }

    public void setUserId(String userId) {
        currentUserId.setValue(userId);
    }

    public void setSearchQuery(String query) {
        searchQuery.setValue(query);
    }

    public LiveData<String> getSearchQuery() {
        return searchQuery;
    }

    public LiveData<List<PharmacyProduct>> getAllProducts() {
        if (allProducts == null) {
            allProducts = repository.getAllProducts();
        }
        return allProducts;
    }

    public LiveData<List<PharmacyProduct>> getProductsByCategory(String category) {
        if (category == null || category.equalsIgnoreCase("all")) {
            return getAllProducts();
        }
        return repository.getProductsByCategory(category.toLowerCase());
    }

    public LiveData<List<PharmacyProduct>> getBestsellers() {
        return Transformations.map(getAllProducts(), products -> {
            // In a real app, bestsellers might be a separate flag or query
            // Here we just return the first 5 products as sample
            if (products == null || products.isEmpty()) return new ArrayList<>();
            return products.subList(0, Math.min(5, products.size()));
        });
    }

    public LiveData<List<CartItem>> getCartItems() {
        return Transformations.switchMap(currentUserId, userId -> {
            if (userId == null) return new MutableLiveData<>(new ArrayList<>());
            return repository.getCartItems(userId);
        });
    }

    public LiveData<Integer> getCartCount() {
        return Transformations.map(getCartItems(), items -> {
            int count = 0;
            if (items != null) {
                for (CartItem item : items) {
                    count += item.getQuantity();
                }
            }
            return count;
        });
    }

    public LiveData<Double> getCartSubtotal() {
        return Transformations.map(getCartItems(), items -> {
            double subtotal = 0.0;
            if (items != null) {
                for (CartItem item : items) {
                    subtotal += item.getTotalPrice();
                }
            }
            return subtotal;
        });
    }

    public void addToCart(PharmacyProduct product, int quantity) {
        String userId = currentUserId.getValue();
        if (userId != null) {
            repository.addToCart(userId, new CartItem(product, quantity));
        }
    }

    public void updateQuantity(String productId, int quantity) {
        String userId = currentUserId.getValue();
        if (userId != null) {
            repository.updateCartItemQuantity(userId, productId, quantity);
        }
    }

    public void removeFromCart(String productId) {
        String userId = currentUserId.getValue();
        if (userId != null) {
            repository.removeFromCart(userId, productId);
        }
    }

    public void clearCart() {
        String userId = currentUserId.getValue();
        if (userId != null) {
            repository.clearCart(userId);
        }
    }
}
