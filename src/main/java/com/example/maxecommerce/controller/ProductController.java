package com.example.maxecommerce.controller;

import com.example.maxecommerce.model.CartItem;
import com.example.maxecommerce.model.Product;
import com.example.maxecommerce.util.DatabaseConnection;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Text;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ProductController {

    @FXML
    private BorderPane rootPane;
    @FXML
    private Button cartButton;
    @FXML
    private Button dashboardButton;
    @FXML
    private Button logoutButton;
    @FXML
    private GridPane productGrid;

    private final List<Product> productList = new ArrayList<>();
    private final int customerId = 1; // Replace with actual customer ID from login

    @FXML
    private void initialize() {
        loadProducts();
        populateProductGrid();
    }

    private void loadProducts() {
        productList.clear();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT p.product_id, p.name, p.description, c.name AS category_name, p.price, p.stock, p.discount, p.image_path " +
                             "FROM Product p JOIN Category c ON p.category_id = c.category_id")) {
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                productList.add(new Product(
                        rs.getInt("product_id"),
                        rs.getString("name"),
                        rs.getString("description"),
                        rs.getString("category_name"),
                        rs.getDouble("price"),
                        rs.getInt("stock"),
                        rs.getDouble("discount"),
                        rs.getString("image_path")
                ));
            }
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to load products: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void populateProductGrid() {
        productGrid.getChildren().clear();
        int column = 0;
        int row = 0;
        for (Product product : productList) {
            VBox card = new VBox(8);
            card.getStyleClass().add("product-card");
            card.setPrefWidth(200);
            card.setPrefHeight(300);

            Text name = new Text(product.getName());
            name.getStyleClass().add("product-title");

            Text description = new Text(product.getDescription() != null ? truncateDescription(product.getDescription(), 50) : "No description");
            description.getStyleClass().add("product-description");

            double discountedPrice = product.getPrice() * (1 - product.getDiscount() / 100);
            Text price = new Text(String.format("₱%.2f", discountedPrice));
            price.getStyleClass().add("product-price");

            Text originalPrice = new Text();
            if (product.getDiscount() > 0) {
                originalPrice.setText(String.format("₱%.2f", product.getPrice()));
                originalPrice.getStyleClass().add("product-price-discounted");
            }

            Text stock = new Text(product.getStock() > 0 ? "In Stock: " + product.getStock() : "Out of Stock");
            stock.getStyleClass().add(product.getStock() > 0 && product.getStock() <= 5 ? "product-stock-low" : "product-stock");

            Region imagePlaceholder = new Region();
            imagePlaceholder.getStyleClass().add("product-image-placeholder");
            imagePlaceholder.setPrefSize(100, 100);
            Text imageText = new Text(product.getImagePath() != null ? product.getImagePath() : "No Image");
            imageText.getStyleClass().add("product-image-placeholder");
            StackPane imageContainer = new StackPane(imagePlaceholder, imageText);

            Button addToCartButton = new Button("Add to Cart");
            addToCartButton.getStyleClass().add("product-btn-primary");
            addToCartButton.setDisable(product.getStock() <= 0);
            addToCartButton.setOnAction(e -> addToCart(product.getId()));

            card.getChildren().addAll(imageContainer, name, description, price, originalPrice, stock, addToCartButton);
            productGrid.add(card, column, row);

            column++;
            if (column >= 3) {
                column = 0;
                row++;
            }
        }
    }

    private String truncateDescription(String description, int maxLength) {
        if (description.length() <= maxLength) return description;
        return description.substring(0, maxLength - 3) + "...";
    }

    private void addToCart(int productId) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            // Check stock
            PreparedStatement stockStmt = conn.prepareStatement("SELECT stock FROM Product WHERE product_id = ?");
            stockStmt.setInt(1, productId);
            ResultSet stockRs = stockStmt.executeQuery();
            if (!stockRs.next() || stockRs.getInt("stock") <= 0) {
                showAlert(Alert.AlertType.ERROR, "Error", "Product out of stock!");
                return;
            }

            // Get or create cart
            PreparedStatement cartStmt = conn.prepareStatement("SELECT cart_id FROM Cart WHERE user_id = ?");
            cartStmt.setInt(1, customerId);
            ResultSet cartRs = cartStmt.executeQuery();
            int cartId;
            if (cartRs.next()) {
                cartId = cartRs.getInt("cart_id");
            } else {
                PreparedStatement insertCart = conn.prepareStatement("INSERT INTO Cart (user_id) VALUES (?)",
                        PreparedStatement.RETURN_GENERATED_KEYS);
                insertCart.setInt(1, customerId);
                insertCart.executeUpdate();
                ResultSet keys = insertCart.getGeneratedKeys();
                keys.next();
                cartId = keys.getInt(1);
            }

            // Add or update cart item
            PreparedStatement itemStmt = conn.prepareStatement(
                    "SELECT cart_item_id, quantity FROM CartItem WHERE cart_id = ? AND product_id = ?");
            itemStmt.setInt(1, cartId);
            itemStmt.setInt(2, productId);
            ResultSet itemRs = itemStmt.executeQuery();
            if (itemRs.next()) {
                int quantity = itemRs.getInt("quantity") + 1;
                PreparedStatement updateItem = conn.prepareStatement(
                        "UPDATE CartItem SET quantity = ? WHERE cart_item_id = ?");
                updateItem.setInt(1, quantity);
                updateItem.setInt(2, itemRs.getInt("cart_item_id"));
                updateItem.executeUpdate();
            } else {
                PreparedStatement insertItem = conn.prepareStatement(
                        "INSERT INTO CartItem (cart_id, product_id, quantity) VALUES (?, ?, 1)");
                insertItem.setInt(1, cartId);
                insertItem.setInt(2, productId);
                insertItem.executeUpdate();
            }

            showAlert(Alert.AlertType.INFORMATION, "Success", "Added to cart!");
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to add to cart: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void showCart() {
        try {
            List<CartItem> cartItems = loadCartItems();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/maxecommerce/cart/CartView.fxml"));
            Parent root = loader.load();
            CartController controller = loader.getController();
            controller.initializeCart(this, cartItems);

            Scene scene = new Scene(root, 600, 400);
            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(scene);
            stage.setTitle("Cart");
            stage.show();
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to open cart: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void removeFromCart(int cartItemId) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement("DELETE FROM CartItem WHERE cart_item_id = ?")) {
            stmt.setInt(1, cartItemId);
            stmt.executeUpdate();
            showAlert(Alert.AlertType.INFORMATION, "Success", "Item removed from cart!");
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to remove item: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void checkout(List<CartItem> cartItems) {
        if (cartItems.isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Error", "Cart is empty!");
            return;
        }

        // Calculate total amount
        double totalAmount = cartItems.stream().mapToDouble(item -> item.getPrice() * item.getQuantity()).sum();

        // Custom payment method dialog
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Checkout");
        dialog.setHeaderText("Complete Your Purchase");

        // Dialog content
        VBox content = new VBox(10);
        content.setPadding(new Insets(20));
        content.setAlignment(Pos.CENTER);

        Label totalLabel = new Label(String.format("Total Amount: ₱%.2f", totalAmount));
        totalLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        Label methodLabel = new Label("Select Payment Method:");
        ChoiceBox<String> methodChoice = new ChoiceBox<>();
        methodChoice.getItems().addAll("COD", "GCash", "Credit Card");
        methodChoice.setValue("COD");

        content.getChildren().addAll(totalLabel, methodLabel, methodChoice);

        dialog.getDialogPane().setContent(content);

        // Add buttons
        ButtonType confirmButtonType = new ButtonType("Confirm", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(confirmButtonType, ButtonType.CANCEL);

        // Handle result
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == confirmButtonType) {
                return methodChoice.getValue();
            }
            return null;
        });

        String selectedMethod = dialog.showAndWait().orElse(null);
        if (selectedMethod == null) {
            showAlert(Alert.AlertType.WARNING, "Cancelled", "Checkout cancelled.");
            return;
        }

        // Simulate payment for GCash/Credit Card
        if (!selectedMethod.equalsIgnoreCase("COD")) {
            boolean paymentSuccessful = showMockPaymentGateway(selectedMethod);
            if (!paymentSuccessful) {
                showAlert(Alert.AlertType.ERROR, "Error", "Payment failed or cancelled.");
                return;
            }
        }

        // Process order
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                // Create order with processing status
                PreparedStatement orderStmt = conn.prepareStatement(
                        "INSERT INTO Orders (user_id, total_amount, status, payment_method) VALUES (?, ?, 'processing', ?)",
                        PreparedStatement.RETURN_GENERATED_KEYS);
                orderStmt.setInt(1, customerId);
                orderStmt.setDouble(2, totalAmount);
                orderStmt.setString(3, selectedMethod);
                int rowsAffected = orderStmt.executeUpdate();
                System.out.println("Order created: rows affected = " + rowsAffected);

                ResultSet orderKeys = orderStmt.getGeneratedKeys();
                if (!orderKeys.next()) {
                    throw new SQLException("Failed to retrieve order ID.");
                }
                int orderId = orderKeys.getInt(1);
                System.out.println("Order ID: " + orderId + ", Status: processing, Payment Method: " + selectedMethod);

                // Insert order items and update stock
                for (CartItem item : cartItems) {
                    int productId = getProductIdByName(item.getProductName());
                    PreparedStatement itemStmt = conn.prepareStatement(
                            "INSERT INTO OrderItem (order_id, product_id, quantity, price_at_purchase) VALUES (?, ?, ?, ?)");
                    itemStmt.setInt(1, orderId);
                    itemStmt.setInt(2, productId);
                    itemStmt.setInt(3, item.getQuantity());
                    itemStmt.setDouble(4, item.getPrice());
                    itemStmt.executeUpdate();

                    PreparedStatement stockStmt = conn.prepareStatement(
                            "UPDATE Product SET stock = stock - ? WHERE product_id = ? AND stock >= ?");
                    stockStmt.setInt(1, item.getQuantity());
                    stockStmt.setInt(2, productId);
                    stockStmt.setInt(3, item.getQuantity());
                    if (stockStmt.executeUpdate() == 0) {
                        throw new SQLException("Insufficient stock for product: " + item.getProductName());
                    }
                }

                // Update order status to completed and confirm payment method
                PreparedStatement updateOrderStmt = conn.prepareStatement(
                        "UPDATE Orders SET status = 'completed', payment_method = ? WHERE order_id = ?");
                updateOrderStmt.setString(1, selectedMethod);
                updateOrderStmt.setInt(2, orderId);
                rowsAffected = updateOrderStmt.executeUpdate();
                System.out.println("Order updated: ID = " + orderId + ", Status: completed, Payment Method: " + selectedMethod + ", Rows affected = " + rowsAffected);

                // Clear cart
                PreparedStatement clearCart = conn.prepareStatement(
                        "DELETE FROM CartItem WHERE cart_id = (SELECT cart_id FROM Cart WHERE user_id = ?)");
                clearCart.setInt(1, customerId);
                clearCart.executeUpdate();

                // Add notification
                PreparedStatement notifyStmt = conn.prepareStatement(
                        "INSERT INTO Notification (user_id, message) VALUES (?, ?)");
                notifyStmt.setInt(1, customerId);
                notifyStmt.setString(2, "Your order #" + orderId + " has been completed.");
                notifyStmt.executeUpdate();

                conn.commit();
                System.out.println("Transaction committed for order ID: " + orderId);
                showAlert(Alert.AlertType.INFORMATION, "Success", String.format("Order placed successfully! Total: ₱%.2f", totalAmount));
            } catch (SQLException e) {
                conn.rollback();
                System.err.println("SQL Error during checkout: " + e.getMessage());
                showAlert(Alert.AlertType.ERROR, "Error", "Checkout failed: " + e.getMessage());
                e.printStackTrace();
                return;
            }
        } catch (SQLException e) {
            System.err.println("Database connection error: " + e.getMessage());
            showAlert(Alert.AlertType.ERROR, "Error", "Database connection failed: " + e.getMessage());
            e.printStackTrace();
            return;
        }
    }

    private boolean showMockPaymentGateway(String method) {
        Stage paymentStage = new Stage();
        paymentStage.setTitle(method + " Payment");

        Label label = new Label("Simulate " + method + " Payment:\nClick 'Pay' to continue or close to cancel.");
        Button payButton = new Button("Pay");
        Button cancelButton = new Button("Cancel");

        final boolean[] result = {false};

        payButton.setOnAction(e -> {
            result[0] = true;
            paymentStage.close();
        });

        cancelButton.setOnAction(e -> {
            result[0] = false;
            paymentStage.close();
        });

        HBox buttonBox = new HBox(10, payButton, cancelButton);
        buttonBox.setAlignment(Pos.CENTER);

        VBox layout = new VBox(15, label, buttonBox);
        layout.setAlignment(Pos.CENTER);
        layout.setPadding(new Insets(20));

        Scene scene = new Scene(layout, 350, 180);
        paymentStage.setScene(scene);
        paymentStage.initModality(Modality.APPLICATION_MODAL);
        paymentStage.showAndWait();

        return result[0];
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

    public List<CartItem> loadCartItems() {
        List<CartItem> cartItems = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT ci.cart_item_id, ci.quantity, p.name, p.price * (1 - p.discount / 100) AS price " +
                             "FROM CartItem ci JOIN Cart c ON ci.cart_id = c.cart_id " +
                             "JOIN Product p ON ci.product_id = p.product_id WHERE c.user_id = ?")) {
            stmt.setInt(1, customerId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                cartItems.add(new CartItem(
                        rs.getInt("cart_item_id"),
                        rs.getString("name"),
                        rs.getInt("quantity"),
                        rs.getDouble("price")
                ));
            }
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to load cart: " + e.getMessage());
            e.printStackTrace();
        }
        return cartItems;
    }

    @FXML
    private void goToDashboard() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/maxecommerce/home/HomeView.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) dashboardButton.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Customer Dashboard");
            showAlert(Alert.AlertType.INFORMATION, "Success", "Navigated to dashboard!");
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to navigate to dashboard: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void handleLogout() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/maxecommerce/auth/LoginView.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) logoutButton.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("E-Commerce Platform - Login");
            showAlert(Alert.AlertType.INFORMATION, "Success", "Logged out successfully!");
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to log out: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}