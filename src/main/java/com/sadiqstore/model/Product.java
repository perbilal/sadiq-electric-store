package com.sadiqstore.model;

public class Product {
    private int id;
    private String name;
    private String description;
    private double costPrice;
    private double sellingPrice;
    private int quantity;

    public Product(int id, String name, String description, double costPrice, double sellingPrice, int quantity) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.costPrice = costPrice;
        this.sellingPrice = sellingPrice;
        this.quantity = quantity;
    }

    // Getters (Needed for the Table to read data)
    public int getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public double getCostPrice() { return costPrice; }
    public double getSellingPrice() { return sellingPrice; }
    public int getQuantity() { return quantity; }
}