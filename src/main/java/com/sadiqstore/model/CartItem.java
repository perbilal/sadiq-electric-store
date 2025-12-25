package com.sadiqstore.model;

public class CartItem {
    private Product product;
    private int quantity;
    private double lineTotal;

    public CartItem(Product product, int quantity) {
        this.product = product;
        this.quantity = quantity;
        this.lineTotal = product.getSellingPrice() * quantity;
    }

    // Getters for the Table
    public String getName() { return product.getName(); }
    public double getPrice() { return product.getSellingPrice(); }
    public int getQuantity() { return quantity; }
    public double getLineTotal() { return lineTotal; }
    
    public Product getProduct() { return product; }
}