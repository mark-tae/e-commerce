package com.example.maxecommerce.model;

// CartItem model class
public class CartItem {
    private final int cartItemId;
    private final String productName;
    private final int quantity;
    private final double price;

    public CartItem(int cartItemId, String productName, int quantity, double price) {
        this.cartItemId = cartItemId;
        this.productName = productName;
        this.quantity = quantity;
        this.price = price;
    }

    public int getCartItemId() {
        return cartItemId;
    }

    public String getProductName() {
        return productName;
    }

    public int getQuantity() {
        return quantity;
    }

    public double getPrice() {
        return price;
    }
}
