package com.sadiqstore.model;

public class Invoice {
    private int id;
    private String date;
    private double total;
    private String customerName; // <--- The new field

    // 1. Update the Constructor to accept 'customerName'
    public Invoice(int id, String date, double total, String customerName) {
        this.id = id;
        this.date = date;
        this.total = total;
        this.customerName = customerName; // <--- IMPORTANT: usage of 'this.'
    }

    // 2. Add the Getters
    public int getId() { return id; }
    public String getDate() { return date; }
    public double getTotal() { return total; }
    public String getCustomerName() { return customerName; }
}