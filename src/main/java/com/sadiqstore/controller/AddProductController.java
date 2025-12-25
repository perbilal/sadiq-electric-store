package com.sadiqstore.controller;

import com.sadiqstore.util.DatabaseHandler;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class AddProductController {

    @FXML private TextField nameField;
    @FXML private TextArea descField;
    @FXML private TextField qtyField;
    @FXML private TextField costField;
    @FXML private TextField priceField;
    @FXML private Label statusLabel;

    @FXML
    private void saveProduct() {
        String name = nameField.getText();
        String desc = descField.getText();
        
        try {
            // Convert text to numbers
            int qty = Integer.parseInt(qtyField.getText());
            double cost = Double.parseDouble(costField.getText());
            double price = Double.parseDouble(priceField.getText());

            if (name.isEmpty()) {
                statusLabel.setText("Name is required!");
                return;
            }

            // SAVE TO DATABASE
            saveToDatabase(name, desc, qty, cost, price);

        } catch (NumberFormatException e) {
            statusLabel.setText("Please enter valid numbers for price/qty");
        }
    }

    private void saveToDatabase(String name, String desc, int qty, double cost, double price) {
        String sql = "INSERT INTO products(name, description, quantity, cost_price, selling_price) VALUES(?,?,?,?,?)";

        try (Connection conn = DatabaseHandler.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, name);
            pstmt.setString(2, desc);
            pstmt.setInt(3, qty);
            pstmt.setDouble(4, cost);
            pstmt.setDouble(5, price);
            
            pstmt.executeUpdate();
            
            System.out.println("Product Saved!");
            
            // Close the window
            Stage stage = (Stage) nameField.getScene().getWindow();
            stage.close();

        } catch (SQLException e) {
            statusLabel.setText("Database Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
    @FXML
    private void closeWindow() {
        Stage stage = (Stage) nameField.getScene().getWindow();
        stage.close();
    }
}