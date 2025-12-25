package com.sadiqstore.controller;

import javafx.collections.transformation.FilteredList;// <--- Import for filtering
import javafx.collections.transformation.SortedList;

import java.sql.PreparedStatement;// <--- Import for SQL
import java.sql.SQLException;
import javafx.scene.control.Alert;// <--- Import for Alerts
import javafx.scene.control.Alert.AlertType;

import com.sadiqstore.model.Product;// <--- Import Product Model
import com.sadiqstore.util.DatabaseHandler;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;

import java.io.IOException;// <--- Import for IOExceptions
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

public class InventoryController {

    @FXML private TextField searchField;
    @FXML private TableView<Product> productTable;
    
    @FXML private TableColumn<Product, Integer> colId;
    @FXML private TableColumn<Product, String> colName;
    @FXML private TableColumn<Product, Integer> colQty;
    @FXML private TableColumn<Product, Double> colPrice;
    @FXML private TableColumn<Product, Double> colCost;

    private ObservableList<Product> productList = FXCollections.observableArrayList();

   @FXML
    public void initialize() {
        // 1. Link columns to Product class data
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colQty.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        colPrice.setCellValueFactory(new PropertyValueFactory<>("sellingPrice"));
        colCost.setCellValueFactory(new PropertyValueFactory<>("costPrice"));

        // 2. Load Data initially
        loadData();

        // 3. SEARCH LOGIC
        // Wrap the list in a FilteredList (initially display all data)
        FilteredList<Product> filteredData = new FilteredList<>(productList, p -> true);

        // Set the filter Predicate whenever the filter changes.
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredData.setPredicate(product -> {
                // If filter text is empty, display all products.
                if (newValue == null || newValue.isEmpty()) {
                    return true;
                }

                String lowerCaseFilter = newValue.toLowerCase();

                // Compare product name with filter text.
                if (product.getName().toLowerCase().contains(lowerCaseFilter)) {
                    return true; // Filter matches name
                } else if (String.valueOf(product.getId()).contains(lowerCaseFilter)) {
                    return true; // Filter matches ID
                }
                return false; // Does not match.
            });
        });

        // 4. Wrap the FilteredList in a SortedList. 
        // This binds the comparator, so sorting the table (clicking headers) still works
        SortedList<Product> sortedData = new SortedList<>(filteredData);
        sortedData.comparatorProperty().bind(productTable.comparatorProperty());

        // 5. Add sorted (and filtered) data to the table.
        productTable.setItems(sortedData);
    }

    private void loadData() {
        productList.clear();
        String sql = "SELECT * FROM products";

        try (Connection conn = DatabaseHandler.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                productList.add(new Product(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getString("description"),
                        rs.getDouble("cost_price"),
                        rs.getDouble("selling_price"),
                        rs.getInt("quantity")
                ));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleAddProduct() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/AddProduct.fxml"));
            Parent root = loader.load();
            
            Stage stage = new Stage();
            stage.setTitle("Add Product");
            stage.setScene(new Scene(root));
            
            // Wait for the window to close, then refresh the table!
            stage.showAndWait(); 
            loadData(); // <--- This refreshes the table immediately after closing!

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

  @FXML
    private void handleDeleteProduct() {
        // 1. Get the selected product
        Product selectedProduct = productTable.getSelectionModel().getSelectedItem();

        if (selectedProduct == null) {
            // Show Error if nothing selected
            Alert alert = new Alert(AlertType.WARNING);
            alert.setTitle("No Selection");
            alert.setHeaderText(null);
            alert.setContentText("Please select a product in the table first.");
            alert.showAndWait();
            return;
        }

        // 2. Delete from Database
        String sql = "DELETE FROM products WHERE id = ?";

        try (Connection conn = DatabaseHandler.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, selectedProduct.getId());
            pstmt.executeUpdate();

            // 3. Refresh the Table
            loadData();
            
            System.out.println("Product deleted successfully.");

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}