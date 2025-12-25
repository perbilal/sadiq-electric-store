package com.sadiqstore.controller;

import com.sadiqstore.model.Customer;
import com.sadiqstore.util.DatabaseHandler;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.util.Pair;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Optional;

public class CustomerController {

    @FXML private TextField nameField;
    @FXML private TextField phoneField;
    @FXML private TextField searchField;
    
    @FXML private TableView<Customer> customerTable;
    @FXML private TableColumn<Customer, Integer> colId;
    @FXML private TableColumn<Customer, String> colName;
    @FXML private TableColumn<Customer, String> colPhone;
    @FXML private TableColumn<Customer, Double> colCredit;

    private ObservableList<Customer> customerList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colPhone.setCellValueFactory(new PropertyValueFactory<>("phone"));
        colCredit.setCellValueFactory(new PropertyValueFactory<>("credit"));

        loadCustomers();

        // Search Filter
        FilteredList<Customer> filteredData = new FilteredList<>(customerList, p -> true);
        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            filteredData.setPredicate(cust -> {
                if (newVal == null || newVal.isEmpty()) return true;
                return cust.getName().toLowerCase().contains(newVal.toLowerCase());
            });
        });
        customerTable.setItems(filteredData);
    }

    private void loadCustomers() {
        customerList.clear();
        try (Connection conn = DatabaseHandler.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM customers")) {

            while (rs.next()) {
                customerList.add(new Customer(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getString("phone"),
                        rs.getDouble("current_credit")
                ));
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    @FXML
    private void addCustomer() {
        String name = nameField.getText();
        String phone = phoneField.getText();

        if (name.isEmpty()) return;

        try (Connection conn = DatabaseHandler.getConnection();
             PreparedStatement pstmt = conn.prepareStatement("INSERT INTO customers(name, phone, current_credit) VALUES(?,?,0)")) {
            
            pstmt.setString(1, name);
            pstmt.setString(2, phone);
            pstmt.executeUpdate();
            
            nameField.clear();
            phoneField.clear();
            loadCustomers(); // Refresh

        } catch (Exception e) { e.printStackTrace(); }
    }

    @FXML
    private void handleUpdateCredit() {
        Customer selected = customerTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            new Alert(Alert.AlertType.WARNING, "Select a customer first!").show();
            return;
        }

        // Custom Dialog to Add/Subtract Money
        Dialog<Pair<String, String>> dialog = new Dialog<>();
        dialog.setTitle("Manage Credit");
        dialog.setHeaderText("Add Debt (Positive) or Payment (Negative)");

        ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10);
        
        TextField amountField = new TextField(); amountField.setPromptText("e.g. 500 or -200");
        TextField descField = new TextField(); descField.setPromptText("e.g. Took Cables");

        grid.add(new Label("Amount:"), 0, 0); grid.add(amountField, 1, 0);
        grid.add(new Label("Description:"), 0, 1); grid.add(descField, 1, 1);

        dialog.getDialogPane().setContent(grid);
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                return new Pair<>(amountField.getText(), descField.getText());
            }
            return null;
        });

        Optional<Pair<String, String>> result = dialog.showAndWait();
        result.ifPresent(pair -> {
            try {
                double amount = Double.parseDouble(pair.getKey());
                String desc = pair.getValue();
                if (desc == null || desc.isEmpty()) desc = "Manual Update";
                
                updateCreditInDB(selected.getId(), amount, desc);
            } catch (NumberFormatException e) {
                new Alert(Alert.AlertType.ERROR, "Invalid Amount").show();
            }
        });
    }

    private void updateCreditInDB(int id, double amount, String desc) {
        try (Connection conn = DatabaseHandler.getConnection()) {
            conn.setAutoCommit(false);
            
            // 1. Update Balance
            PreparedStatement psBal = conn.prepareStatement("UPDATE customers SET current_credit = current_credit + ? WHERE id = ?");
            psBal.setDouble(1, amount);
            psBal.setInt(2, id);
            psBal.executeUpdate();
            
            // 2. Add History
            PreparedStatement psHist = conn.prepareStatement("INSERT INTO credit_transactions(customer_id, amount, description) VALUES(?,?,?)");
            psHist.setInt(1, id);
            psHist.setDouble(2, amount);
            psHist.setString(3, desc);
            psHist.executeUpdate();
            
            conn.commit();
            loadCustomers();
            new Alert(Alert.AlertType.INFORMATION, "Credit Updated!").show();

        } catch (Exception e) { e.printStackTrace(); }
    }

    @FXML
    private void handleDeleteCustomer() {
        Customer selected = customerTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        
        try (Connection conn = DatabaseHandler.getConnection();
             PreparedStatement pstmt = conn.prepareStatement("DELETE FROM customers WHERE id = ?")) {
             
            pstmt.setInt(1, selected.getId());
            pstmt.executeUpdate();
            loadCustomers();
        } catch (Exception e) { e.printStackTrace(); }
    }
    
    @FXML
    private void handleViewHistory() {
        Customer selected = customerTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        StringBuilder history = new StringBuilder("Transaction History for " + selected.getName() + ":\n\n");
        String sql = "SELECT * FROM credit_transactions WHERE customer_id = ? ORDER BY id DESC";

        try (Connection conn = DatabaseHandler.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, selected.getId());
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                String date = rs.getString("date");
                String desc = rs.getString("description");
                double amt = rs.getDouble("amount");
                
                history.append("[").append(date.substring(0, 16)).append("] ")
                       .append(desc).append(" ... ")
                       .append(amt > 0 ? "Debt: " : "Paid: ")
                       .append(Math.abs(amt))
                       .append("\n");
            }

            TextArea area = new TextArea(history.toString());
            area.setEditable(false);
            area.setWrapText(true);
            area.setPrefSize(400, 300);

            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Customer History");
            alert.setHeaderText("Udhaar Details: " + selected.getName());
            alert.getDialogPane().setContent(area);
            alert.showAndWait();

        } catch (Exception e) { e.printStackTrace(); }
    }
}