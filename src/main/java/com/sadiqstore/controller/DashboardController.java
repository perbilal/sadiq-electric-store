package com.sadiqstore.controller;

import com.sadiqstore.App;
import com.sadiqstore.util.DatabaseHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.BorderPane;
import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

public class DashboardController {

    @FXML private BorderPane mainPane;
    
    // Stats Labels
    @FXML private Label totalProductsLabel;
    @FXML private Label lowStockLabel;
    @FXML private Label todaySalesLabel;

    @FXML
    public void initialize() {
        refreshStats();
    }

    @FXML
    private void refreshStats() {
        // Only run this if the labels are loaded (e.g. on the main screen)
        if (totalProductsLabel == null) return; 

        try (Connection conn = DatabaseHandler.getConnection();
             Statement stmt = conn.createStatement()) {

            // 1. Total Products
            ResultSet rs1 = stmt.executeQuery("SELECT COUNT(*) FROM products");
            if(rs1.next()) totalProductsLabel.setText(String.valueOf(rs1.getInt(1)));

            // 2. Low Stock (Items less than 5)
            ResultSet rs2 = stmt.executeQuery("SELECT COUNT(*) FROM products WHERE quantity < 5");
            if(rs2.next()) lowStockLabel.setText(String.valueOf(rs2.getInt(1)));

            // 3. Today's Invoices
            ResultSet rs3 = stmt.executeQuery("SELECT COUNT(*) FROM invoices WHERE date(date) = date('now')");
            if(rs3.next()) todaySalesLabel.setText(String.valueOf(rs3.getInt(1)));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void showInventory() {
        // 1. Ask for Password
        TextInputDialog dialog = new TextInputDialog("");
        dialog.setTitle("Security Check");
        dialog.setHeaderText("Restricted Access");
        dialog.setContentText("Please enter Admin PIN to view Inventory:");

        java.util.Optional<String> result = dialog.showAndWait();
        
        if (result.isPresent()) {
            // CHANGE "1234" TO YOUR PREFERRED PASSWORD
            if (result.get().equals("admin")) { 
                loadPage("Inventory");
            } else {
                new Alert(Alert.AlertType.ERROR, "Wrong PIN!").show();
            }
        }
    }

    @FXML
    private void showProfit() {
        loadPage("ProfitDashboard");
    }

    @FXML
    private void showCustomers() { 
        loadPage("Customers");
    }

    @FXML
    private void showInvoice() {
        loadPage("Invoice");
    }

    @FXML
    private void showHistory() {
        loadPage("InvoiceHistory");
    }

    @FXML
    private void showHome() {
        // Reloads the Dashboard so the Stats Cards reappear
        try {
            App.setRoot("Dashboard");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Helper method to switch the center screen
    private void loadPage(String pageName) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/" + pageName + ".fxml"));
            Parent root = loader.load();
            mainPane.setCenter(root);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}