package com.sadiqstore.controller;

import com.sadiqstore.App;
import com.sadiqstore.model.CartItem;
import com.sadiqstore.model.Invoice;
import com.sadiqstore.model.Product; // Ensure this is imported
import com.sadiqstore.util.DatabaseHandler;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Optional;

public class InvoiceHistoryController {

    @FXML private TextField searchField;
    @FXML private TableView<Invoice> invoiceTable;
    @FXML private TableColumn<Invoice, Integer> colId;
    @FXML private TableColumn<Invoice, String> colDate;
    @FXML private TableColumn<Invoice, String> colCustomer;
    @FXML private TableColumn<Invoice, Double> colTotal;

    private ObservableList<Invoice> invoiceList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colDate.setCellValueFactory(new PropertyValueFactory<>("date"));
        colCustomer.setCellValueFactory(new PropertyValueFactory<>("customerName"));
        colTotal.setCellValueFactory(new PropertyValueFactory<>("total"));

        loadInvoices();

        // --- SEARCH FUNCTIONALITY ---
        FilteredList<Invoice> filteredData = new FilteredList<>(invoiceList, p -> true);
        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            filteredData.setPredicate(inv -> {
                if (newVal == null || newVal.isEmpty()) return true;
                return inv.getCustomerName().toLowerCase().contains(newVal.toLowerCase());
            });
        });
        invoiceTable.setItems(filteredData);
    }

    private void loadInvoices() {
        invoiceList.clear();
        // Updated SQL to fetch customer_name
        String sql = "SELECT * FROM invoices ORDER BY id DESC"; 

        try (Connection conn = DatabaseHandler.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                invoiceList.add(new Invoice(
                        rs.getInt("id"),
                        rs.getString("date"),
                        rs.getDouble("total_amount"),
                        rs.getString("customer_name") // Fetch new column
                ));
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    // --- REQUIREMENT 1: OPTIONAL RESTORE ---
    @FXML
    private void handleDeleteInvoice() {
        Invoice selected = invoiceTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        // Custom Dialog
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Delete Invoice");
        alert.setHeaderText("Delete Invoice #" + selected.getId());
        alert.setContentText("Do you want to restore the stock items to inventory?");

        ButtonType btnRestore = new ButtonType("Yes, Delete & Restore Stock");
        ButtonType btnDeleteOnly = new ButtonType("No, Delete Only");
        ButtonType btnCancel = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);

        alert.getButtonTypes().setAll(btnRestore, btnDeleteOnly, btnCancel);

        Optional<ButtonType> result = alert.showAndWait();
        if (result.get() == btnRestore) {
            deleteInvoice(selected.getId(), true); // Restore = true
        } else if (result.get() == btnDeleteOnly) {
            deleteInvoice(selected.getId(), false); // Restore = false
        }
    }

    private void deleteInvoice(int invoiceId, boolean restoreStock) {
        String getItemsSQL = "SELECT product_id, quantity FROM invoice_items WHERE invoice_id = ?";
        String restoreStockSQL = "UPDATE products SET quantity = quantity + ? WHERE id = ?";
        String deleteItemsSQL = "DELETE FROM invoice_items WHERE invoice_id = ?";
        String deleteInvoiceSQL = "DELETE FROM invoices WHERE id = ?";

        try (Connection conn = DatabaseHandler.getConnection()) {
            conn.setAutoCommit(false);

            // 1. Restore Stock (Only if requested)
            if (restoreStock) {
                try (PreparedStatement getStmt = conn.prepareStatement(getItemsSQL);
                     PreparedStatement updateStmt = conn.prepareStatement(restoreStockSQL)) {
                    getStmt.setInt(1, invoiceId);
                    ResultSet rs = getStmt.executeQuery();
                    while (rs.next()) {
                        updateStmt.setInt(1, rs.getInt("quantity"));
                        updateStmt.setInt(2, rs.getInt("product_id"));
                        updateStmt.addBatch();
                    }
                    updateStmt.executeBatch();
                }
            }

            // 2. Delete Record
            try (PreparedStatement delItems = conn.prepareStatement(deleteItemsSQL);
                 PreparedStatement delInv = conn.prepareStatement(deleteInvoiceSQL)) {
                delItems.setInt(1, invoiceId);
                delItems.executeUpdate();
                delInv.setInt(1, invoiceId);
                delInv.executeUpdate();
            }

            conn.commit();
            loadInvoices(); // Refresh UI
            System.out.println("Invoice Deleted. Restored: " + restoreStock);

        } catch (Exception e) { e.printStackTrace(); }
    }

    // --- REQUIREMENT 2: VIEW DETAILS ---
    @FXML
    private void handleViewDetails() {
        Invoice selected = invoiceTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        StringBuilder details = new StringBuilder("Items in Invoice #" + selected.getId() + ":\n\n");
        String sql = "SELECT p.name, i.quantity, i.unit_price FROM invoice_items i JOIN products p ON i.product_id = p.id WHERE i.invoice_id = " + selected.getId();

        try (Connection conn = DatabaseHandler.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                details.append("• ").append(rs.getString("name"))
                       .append(" (x").append(rs.getInt("quantity")).append(")")
                       .append(" - Rs. ").append(rs.getDouble("unit_price") * rs.getInt("quantity"))
                       .append("\n");
            }
            
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Invoice Details");
            alert.setHeaderText("Customer: " + selected.getCustomerName());
            alert.setContentText(details.toString());
            alert.showAndWait();

        } catch (Exception e) { e.printStackTrace(); }
    }

    // --- REQUIREMENT 5: EDIT / UPDATE INVOICE ---
    @FXML
    private void handleEditInvoice() {
        Invoice selected = invoiceTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        // Logic: 
        // 1. Delete the invoice (AND Restore Stock automatically) so it's like it never happened.
        // 2. Redirect user to "Invoice" page.
        // 3. (Optional) We could preload the cart, but for simplicity, we just cancel the old one 
        //    and let the user type the new one.
        
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Update Invoice");
        alert.setHeaderText("Edit Mode");
        alert.setContentText("To edit, we must cancel this invoice and restore items to stock.\nYou can then create a new correct invoice.\n\nProceed?");
        
        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            deleteInvoice(selected.getId(), true); // Delete & Restore
            try {
                App.setRoot("Invoice"); // Send user to New Invoice page
            } catch (IOException e) { e.printStackTrace(); }
        }
    }
}