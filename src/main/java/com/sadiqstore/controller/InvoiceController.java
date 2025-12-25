package com.sadiqstore.controller;

import com.sadiqstore.model.CartItem;
import com.sadiqstore.model.Product;
import com.sadiqstore.util.DatabaseHandler;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import java.sql.*;
import javafx.collections.transformation.FilteredList;
 import javafx.collections.transformation.SortedList;

public class InvoiceController {

    // Left Side
    @FXML private TextField searchField;
    @FXML private TableView<Product> productTable;
    @FXML private TableColumn<Product, String> colProdName;
    @FXML private TableColumn<Product, Double> colProdPrice;
    @FXML private TableColumn<Product, Integer> colProdQty;
    @FXML private TextField qtyInput;

    // Right Side
    @FXML private TextField customerNameField; // <--- The new Customer Name Field
    @FXML private TableView<CartItem> cartTable;
    @FXML private TableColumn<CartItem, String> colCartName;
    @FXML private TableColumn<CartItem, Integer> colCartQty;
    @FXML private TableColumn<CartItem, Double> colCartTotal;
    @FXML private Label totalLabel;

    private ObservableList<Product> productList = FXCollections.observableArrayList();
    private ObservableList<CartItem> cartList = FXCollections.observableArrayList();

  @FXML
    public void initialize() {
        setupTables();
        loadProducts();
        
        // --- NEW SEARCH LOGIC ---
        FilteredList<Product> filteredData = new FilteredList<>(productList, p -> true);
        
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredData.setPredicate(product -> {
                if (newValue == null || newValue.isEmpty()) return true;
                
                String lowerCaseFilter = newValue.toLowerCase();
                return product.getName().toLowerCase().contains(lowerCaseFilter);
            });
        });

        SortedList<Product> sortedData = new SortedList<>(filteredData);
        sortedData.comparatorProperty().bind(productTable.comparatorProperty());
        productTable.setItems(sortedData); // Bind the SORTED list, not the raw list
    }

    private void setupTables() {
        colProdName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colProdPrice.setCellValueFactory(new PropertyValueFactory<>("sellingPrice"));
        colProdQty.setCellValueFactory(new PropertyValueFactory<>("quantity"));
      

        colCartName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colCartQty.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        colCartTotal.setCellValueFactory(new PropertyValueFactory<>("lineTotal"));
        cartTable.setItems(cartList);
    }

    private void loadProducts() {
        productList.clear();
        try (Connection conn = DatabaseHandler.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM products")) {

            while (rs.next()) {
                productList.add(new Product(
                        rs.getInt("id"), rs.getString("name"), rs.getString("description"),
                        rs.getDouble("cost_price"), rs.getDouble("selling_price"), rs.getInt("quantity")
                ));
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    @FXML
    private void addToCart() {
        Product selected = productTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        try {
            int qty = Integer.parseInt(qtyInput.getText());
            if (qty > selected.getQuantity()) {
                new Alert(Alert.AlertType.ERROR, "Not enough stock! Available: " + selected.getQuantity()).show();
                return;
            }
            cartList.add(new CartItem(selected, qty));
            updateTotal();
        } catch (NumberFormatException e) { }
    }

    @FXML
    private void removeFromCart() {
        CartItem selected = cartTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            cartList.remove(selected);
            updateTotal();
        }
    }

    private void updateTotal() {
        double total = 0;
        for (CartItem item : cartList) total += item.getLineTotal();
        totalLabel.setText("Rs. " + total);
    }

   @FXML private CheckBox creditCheckBox; // <--- ADD THIS

    @FXML
    private void checkout() {
        if (cartList.isEmpty()) return;

        double totalAmount = 0;
        for (CartItem item : cartList) totalAmount += item.getLineTotal();
        
        String custName = customerNameField.getText().trim();
        boolean isCredit = creditCheckBox.isSelected();

        // VALIDATION: Cannot do credit without a name
        if (isCredit && custName.isEmpty()) {
            new Alert(Alert.AlertType.ERROR, "For Udhaar, Customer Name is Required!").show();
            return;
        }
        if (custName.isEmpty()) custName = "Walk-in";

        Connection conn = null;
        try {
            conn = DatabaseHandler.getConnection();
            conn.setAutoCommit(false);

            // 1. Check if Customer Exists (For Credit linking)
            int customerId = -1;
            if (isCredit) {
                // Find or Create Customer
                PreparedStatement psFind = conn.prepareStatement("SELECT id FROM customers WHERE name = ?");
                psFind.setString(1, custName);
                ResultSet rs = psFind.executeQuery();
                if (rs.next()) {
                    customerId = rs.getInt("id");
                } else {
                    // Create new customer automatically
                    PreparedStatement psCreate = conn.prepareStatement("INSERT INTO customers(name, current_credit) VALUES(?, 0)", Statement.RETURN_GENERATED_KEYS);
                    psCreate.setString(1, custName);
                    psCreate.executeUpdate();
                    ResultSet keys = psCreate.getGeneratedKeys();
                    if(keys.next()) customerId = keys.getInt(1);
                }
            }

            // 2. Insert Invoice
            String insertInv = "INSERT INTO invoices(total_amount, customer_name, customer_id) VALUES(?, ?, ?)";
            int invoiceId = -1;
            try (PreparedStatement pstmt = conn.prepareStatement(insertInv, Statement.RETURN_GENERATED_KEYS)) {
                pstmt.setDouble(1, totalAmount);
                pstmt.setString(2, custName);
                if (customerId != -1) pstmt.setInt(3, customerId); else pstmt.setNull(3, java.sql.Types.INTEGER);
                pstmt.executeUpdate();
                ResultSet keys = pstmt.getGeneratedKeys();
                if(keys.next()) invoiceId = keys.getInt(1);
            }

            // 3. IF CREDIT: Add to Transaction History & Update Balance
            if (isCredit && customerId != -1) {
                String sqlHist = "INSERT INTO credit_transactions(customer_id, amount, description, invoice_id) VALUES(?, ?, ?, ?)";
                String sqlBal = "UPDATE customers SET current_credit = current_credit + ? WHERE id = ?";
                
                try(PreparedStatement psHist = conn.prepareStatement(sqlHist);
                    PreparedStatement psBal = conn.prepareStatement(sqlBal)) {
                    
                    // History Record
                    psHist.setInt(1, customerId);
                    psHist.setDouble(2, totalAmount);
                    psHist.setString(3, "Invoice #" + invoiceId);
                    psHist.setInt(4, invoiceId);
                    psHist.executeUpdate();
                    
                    // Update Balance
                    psBal.setDouble(1, totalAmount);
                    psBal.setInt(2, customerId);
                    psBal.executeUpdate();
                }
            }

            // 4. Insert Items & Update Stock (Same as before)
            String insertItem = "INSERT INTO invoice_items(invoice_id, product_id, quantity, unit_price) VALUES(?,?,?,?)";
            String updateStock = "UPDATE products SET quantity = quantity - ? WHERE id = ?";
            try (PreparedStatement psItem = conn.prepareStatement(insertItem);
                 PreparedStatement psStock = conn.prepareStatement(updateStock)) {
                for (CartItem item : cartList) {
                    psItem.setInt(1, invoiceId); psItem.setInt(2, item.getProduct().getId());
                    psItem.setInt(3, item.getQuantity()); psItem.setDouble(4, item.getPrice()); psItem.addBatch();
                    psStock.setInt(1, item.getQuantity()); psStock.setInt(2, item.getProduct().getId()); psStock.addBatch();
                }
                psItem.executeBatch(); psStock.executeBatch();
            }

            conn.commit();
            new Alert(Alert.AlertType.INFORMATION, "Invoice Saved! " + (isCredit ? "(Added to Udhaar)" : "")).show();
            cartList.clear(); totalLabel.setText("Rs. 0.0"); customerNameField.clear(); creditCheckBox.setSelected(false); loadProducts();

        } catch (SQLException e) {
            e.printStackTrace();
            try { if(conn!=null) conn.rollback(); } catch (SQLException ex){}
        }
    }
}