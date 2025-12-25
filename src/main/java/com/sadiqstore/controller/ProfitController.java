package com.sadiqstore.controller;

import com.sadiqstore.util.DatabaseHandler;
import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

public class ProfitController {

    @FXML private Label revenueLabel;
    @FXML private Label costLabel;
    @FXML private Label profitLabel;
    @FXML private BarChart<String, Number> salesChart;

    @FXML
    public void initialize() {
        calculateFinances();
        loadChartData();
    }

    private void calculateFinances() {
        // This query calculates total revenue and total cost based on current product cost
        String sql = "SELECT " +
                     "SUM(ii.quantity * ii.unit_price) as total_revenue, " +
                     "SUM(ii.quantity * p.cost_price) as total_cost " +
                     "FROM invoice_items ii " +
                     "JOIN products p ON ii.product_id = p.id";

        try (Connection conn = DatabaseHandler.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            if (rs.next()) {
                double revenue = rs.getDouble("total_revenue");
                double cost = rs.getDouble("total_cost");
                double profit = revenue - cost;

                revenueLabel.setText("Rs. " + String.format("%.2f", revenue));
                costLabel.setText("Rs. " + String.format("%.2f", cost));
                profitLabel.setText("Rs. " + String.format("%.2f", profit));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadChartData() {
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Units Sold");

        // Get Top 5 selling products
        String sql = "SELECT p.name, SUM(ii.quantity) as total_sold " +
                     "FROM invoice_items ii " +
                     "JOIN products p ON ii.product_id = p.id " +
                     "GROUP BY ii.product_id " +
                     "ORDER BY total_sold DESC LIMIT 5";

        try (Connection conn = DatabaseHandler.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                series.getData().add(new XYChart.Data<>(rs.getString("name"), rs.getInt("total_sold")));
            }
            
            salesChart.getData().add(series);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}