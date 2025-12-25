package com.sadiqstore.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseHandler {

    // This checks if we are inside the jar or running in IDE to save DB in correct place
    private static final String DB_URL = "jdbc:sqlite:sadiq_store.db";

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL);
    }

    public static void initDB() {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {

            // 1. Create Users Table (For Login)
            stmt.execute("CREATE TABLE IF NOT EXISTS users (" +
                         "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                         "username TEXT UNIQUE NOT NULL, " +
                         "password TEXT NOT NULL)");

            // 2. Create Products Table
            stmt.execute("CREATE TABLE IF NOT EXISTS products (" +
                         "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                         "name TEXT NOT NULL, " +
                         "description TEXT, " +
                         "cost_price REAL NOT NULL, " +
                         "selling_price REAL NOT NULL, " +
                         "quantity INTEGER NOT NULL)");

            // 3. Create Customers Table (For Credit)
            stmt.execute("CREATE TABLE IF NOT EXISTS customers (" +
                         "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                         "name TEXT NOT NULL, " +
                         "phone TEXT, " +
                         "current_credit REAL DEFAULT 0.0)");

            // 4. Create Invoices Table (UPDATED WITH CUSTOMER NAME)
            stmt.execute("CREATE TABLE IF NOT EXISTS invoices (" +
                         "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                         "customer_id INTEGER, " +
                         "customer_name TEXT DEFAULT 'Walk-in', " + // <--- ADDED THIS LINE
                         "date TEXT DEFAULT CURRENT_TIMESTAMP, " +
                         "total_amount REAL, " +
                         "FOREIGN KEY(customer_id) REFERENCES customers(id))");

            // 5. Create Invoice Items Table (Linking Products to Invoices)
            stmt.execute("CREATE TABLE IF NOT EXISTS invoice_items (" +
                         "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                         "invoice_id INTEGER, " +
                         "product_id INTEGER, " +
                         "quantity INTEGER, " +
                         "unit_price REAL, " +
                         "FOREIGN KEY(invoice_id) REFERENCES invoices(id), " +
                         "FOREIGN KEY(product_id) REFERENCES products(id))");

            // 6. Create Credit Transactions Table (The History Ledger)
            stmt.execute("CREATE TABLE IF NOT EXISTS credit_transactions (" +
                         "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                         "customer_id INTEGER, " +
                         "amount REAL, " + // Positive = Debt, Negative = Payment
                         "date TEXT DEFAULT CURRENT_TIMESTAMP, " +
                         "description TEXT, " + // e.g. "Invoice #105" or "Took Cables"
                         "invoice_id INTEGER, " + // Optional link to invoice
                         "FOREIGN KEY(customer_id) REFERENCES customers(id))");             

            System.out.println("Database and Tables initialized successfully!");

        } catch (SQLException e) {
            System.err.println("Error initializing database: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
}