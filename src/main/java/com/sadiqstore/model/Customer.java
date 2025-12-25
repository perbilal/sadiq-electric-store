package com.sadiqstore.model;

public class Customer {
    private int id;
    private String name;
    private String phone;
    private double credit;

    public Customer(int id, String name, String phone, double credit) {
        this.id = id;
        this.name = name;
        this.phone = phone;
        this.credit = credit;
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public String getPhone() { return phone; }
    public double getCredit() { return credit; }
}