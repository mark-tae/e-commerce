package com.example.maxecommerce.controller;

import com.example.maxecommerce.model.CartItem;
import com.example.maxecommerce.util.DatabaseConnection;
import com.example.maxecommerce.util.Toast;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class CheckoutController {

    @FXML
    private BorderPane rootPane;
    @FXML
    private TableView<CartItem> cartTable;
    @FXML
    private TableColumn<CartItem, String> nameColumn;
    @FXML
    private TableColumn<CartItem, String> quantityColumn;
    @FXML
    private TableColumn<CartItem, String> priceColumn;
    @FXML
    private TableColumn<CartItem, String> subtotalColumn;
    @FXML
    private Text totalAmountText;
    @FXML
    private RadioButton codRadio;
    @FXML
    private RadioButton digitalWalletRadio;
    @FXML
    private ToggleGroup paymentGroup;
    @FXML
    private Button confirmButton;
    @FXML
    private Button cancelButton;

    private List<CartItem> cartItems;
    private int customerId;
    private double totalAmount;

    public void initializeCheckout(List<CartItem> items, int customerId) {
        this.cartItems = items;
        this.customerId = customerId;
        setupTable();
        loadCartItems();
    }

    private void setupTable() {
        nameColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getProductName()));
        quantityColumn.setCellValueFactory(cellData -> new SimpleStringProperty(String.valueOf(cellData.getValue().getQuantity())));
        priceColumn.setCellValueFactory(cellData -> new SimpleStringProperty(String.format("$%.2f", cellData.getValue().getPrice())));
        subtotalColumn.setCellValueFactory(cellData -> new SimpleStringProperty(String.format("$%.2f", cellData.getValue().getPrice() * cellData.getValue().getQuantity())));
        cartTable.getItems().setAll(cartItems);

        totalAmount = cartItems.stream().mapToDouble(item -> item.getPrice() * item.getQuantity()).sum();
        totalAmountText.setText(String.format("Total: $%.2f", totalAmount));
    }

    private void loadCartItems() {
        if (cartItems.isEmpty()) {
            Toast.show("Cart is empty!", Toast.ToastType.ERROR, rootPane.getScene());
            cancelCheckout();
        }
    }

    @FXML
    private void confirmOrder() {
        String paymentMethod = codRadio.isSelected() ? "cod" : "digital_wallet";
        checkout(cartItems, paymentMethod);
    }

    @FXML
    private void cancelCheckout() {
        Stage stage = (Stage) cancelButton.getScene().getWindow();
        stage.close();
    }

    private void checkout(List<CartItem> cartItems, String paymentMethod) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                // Create order
                PreparedStatement orderStmt = conn.prepareStatement(
                        "INSERT INTO Orders (user_id, total_amount, status, payment_method) VALUES (?, ?, 'processing', ?)",
                        PreparedStatement.RETURN_GENERATED_KEYS);
                orderStmt.setInt(1, customerId);
                orderStmt.setDouble(2, totalAmount);
                orderStmt.setString(3, paymentMethod);orderStmt.executeUpdate();
                ResultSet orderKeys = orderStmt.getGeneratedKeys();
                orderKeys.next();
                int orderId = orderKeys.getInt(1);

                // Add order items and update stock
                for (CartItem item : cartItems) {
                    PreparedStatement itemStmt = conn.prepareStatement(
                            "INSERT INTO OrderItem (order_id, product_id, quantity, price_at_purchase) VALUES (?, ?, ?, ?)");
                    itemStmt.setInt(1, orderId);
                    itemStmt.setInt(2, getProductIdByName(item.getProductName()));
                    itemStmt.setInt(3, item.getQuantity());
                    itemStmt.setDouble(4, item.getPrice());
                    itemStmt.executeUpdate();

                    PreparedStatement stockStmt = conn.prepareStatement(
                            "UPDATE Product SET stock = stock - ? WHERE product_id = ? AND stock >= ?");
                    stockStmt.setInt(1, item.getQuantity());
                    stockStmt.setInt(2, getProductIdByName(item.getProductName()));
                    stockStmt.setInt(3, item.getQuantity());
                    if (stockStmt.executeUpdate() == 0) {
                        throw new SQLException("Insufficient stock for product: " + item.getProductName());
                    }
                }

                // Clear cart
                PreparedStatement clearCart = conn.prepareStatement("DELETE FROM CartItem WHERE cart_id = (SELECT cart_id FROM Cart WHERE user_id = ?)");
                clearCart.setInt(1, customerId);
                clearCart.executeUpdate();

                // Add notification
                PreparedStatement notifyStmt = conn.prepareStatement(
                        "INSERT INTO Notification (user_id, message) VALUES (?, ?)");
                notifyStmt.setInt(1, customerId);
                notifyStmt.setString(2, "Your order #" + orderId + " is being processed.");
                notifyStmt.executeUpdate();

                conn.commit();
                Toast.show("Order placed successfully!", Toast.ToastType.SUCCESS, rootPane.getScene());
                ((Stage) confirmButton.getScene().getWindow()).close();
            } catch (SQLException e) {
                conn.rollback();
                Toast.show("Error during checkout: " + e.getMessage(), Toast.ToastType.ERROR, rootPane.getScene());
                e.printStackTrace();
            }
        } catch (SQLException e) {
            Toast.show("Database error: " + e.getMessage(), Toast.ToastType.ERROR, rootPane.getScene());
            e.printStackTrace();
        }
    }

    private int getProductIdByName(String productName) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT product_id FROM Product WHERE name = ?")) {
            stmt.setString(1, productName);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("product_id");
            }
            throw new SQLException("Product not found: " + productName);
        }
    }
}