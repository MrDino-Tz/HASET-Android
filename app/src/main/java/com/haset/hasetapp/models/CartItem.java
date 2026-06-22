package com.haset.hasetapp.models;

import java.io.Serializable;

public class CartItem implements Serializable {
    private String productId;
    private String productName;
    private double unitPrice;
    private int quantity;
    private String imageUrl;
    private String category;

    public CartItem() {
    }

    public CartItem(PharmacyProduct product, int quantity) {
        this.productId = product.getProductId();
        this.productName = product.getName();
        this.unitPrice = product.getPrice();
        this.quantity = quantity;
        this.imageUrl = product.getImageUrl();
        this.category = product.getCategory();
    }

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public double getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(double unitPrice) {
        this.unitPrice = unitPrice;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public double getTotalPrice() {
        return unitPrice * quantity;
    }
}
