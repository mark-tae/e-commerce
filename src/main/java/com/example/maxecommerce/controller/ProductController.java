package com.example.maxecommerce.controller;

import com.example.maxecommerce.model.CartItem;
import com.example.maxecommerce.model.Product;
import com.example.maxecommerce.util.DatabaseConnection;
import com.example.maxecommerce.util.SessionManager;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.stripe.Stripe;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import javafx.application.HostServices;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.text.Text;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;

public class ProductController {

    private static final Logger LOGGER = Logger.getLogger(ProductController.class.getName());
    private static final String STRIPE_API_KEY = System.getenv("STRIPE_API_KEY");
    private static final String PAYMENT_BASE_URL = "gcash://pay";

    @FXML
    private BorderPane rootPane;
    @FXML
    private TextField searchField;
    @FXML
    private Button cartButton;
    @FXML
    private Button dashboardButton;
    @FXML
    private Button logoutButton;
    @FXML
    private GridPane productGrid;
    @FXML
    private Button loginButton;

    private final List<Product> productList = new ArrayList<>();
    private final int customerId = SessionManager.getInstance().getCurrentUserId() != null ? SessionManager.getInstance().getCurrentUserId() : -1;
    private HostServices hostServices;

    @FXML
    private void initialize() {
        rootPane.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                Stage stage = (Stage) newScene.getWindow();
                stage.setWidth(1200);
                stage.setHeight(800);
                stage.setResizable(false);
                stage.centerOnScreen();
                LOGGER.info("Product window set to 1200x800, centered");
            }
        });

        if (SessionManager.getInstance().isGuest()) {
            dashboardButton.setVisible(false);
            logoutButton.setVisible(false);
            cartButton.setVisible(false);
            loginButton.setVisible(true);
        } else {
            dashboardButton.setVisible(true);
            logoutButton.setVisible(true);
            cartButton.setVisible(true);
            loginButton.setVisible(false);
        }
        loadProducts("");
        setupSearchListener();
    }

    private void setupSearchListener() {
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            loadProducts(newValue.trim());
        });
    }

    @FXML
    private void handleSearch() {
        loadProducts(searchField.getText().trim());
    }

    public void setHostServices(HostServices hostServices) {
        this.hostServices = hostServices;
        LOGGER.info("HostServices set in ProductController: " + (hostServices != null));
    }

    private void loadProducts(String searchTerm) {
        productList.clear();
        String query = "SELECT p.product_id, p.name, p.description, c.name AS category_name, p.price, p.stock, p.discount, p.image_path " +
                "FROM Product p JOIN Category c ON p.category_id = c.category_id WHERE p.status = 'approved'";
        if (!searchTerm.isEmpty()) {
            query += " AND (LOWER(p.name) LIKE ? OR LOWER(p.description) LIKE ?)";
        }

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            if (!searchTerm.isEmpty()) {
                String likePattern = "%" + searchTerm.toLowerCase() + "%";
                stmt.setString(1, likePattern);
                stmt.setString(2, likePattern);
            }
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
            populateProductGrid();
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to load products: " + e.getMessage());
            LOGGER.severe("Failed to load products: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void populateProductGrid() {
        productGrid.getChildren().clear();
        int column = 0;
        int row = 0;
        for (Product product : productList) {
            VBox card = new VBox(10);
            card.getStyleClass().add("dashboard-card");
            card.setPrefWidth(300);
            card.setPadding(new Insets(10));

            ImageView productImage = new ImageView();
            productImage.setFitWidth(150);
            productImage.setFitHeight(150);
            productImage.setPreserveRatio(true);
            try {
                String imagePath = product.getImagePath();
                if (imagePath != null && !imagePath.isEmpty()) {
                    String resourcePath = "/com/example/maxecommerce/images/" + imagePath;
                    Image image = new Image(Objects.requireNonNull(getClass().getResourceAsStream(resourcePath)));
                    productImage.setImage(image);
                } else {
                    productImage.setImage(new Image(Objects.requireNonNull(getClass().getResourceAsStream("/com/example/maxecommerce/images/default-product.png"))));
                }
            } catch (Exception e) {
                productImage.setImage(new Image(Objects.requireNonNull(getClass().getResourceAsStream("/com/example/maxecommerce/images/default-product.png"))));
                LOGGER.warning("Failed to load product image: " + e.getMessage());
            }

            Text name = new Text(product.getName());
            name.getStyleClass().add("dashboard-title");

            Text description = new Text(product.getDescription() != null ? truncateDescription(product.getDescription(), 50) : "No description");
            description.getStyleClass().add("dashboard-label");

            double discountedPrice = product.getPrice() * (1 - product.getDiscount() / 100);
            Text price = new Text(String.format("₱%.2f", discountedPrice));
            price.getStyleClass().add("dashboard-label");

            Text originalPrice = new Text();
            if (product.getDiscount() > 0) {
                originalPrice.setText(String.format("₱%.2f", product.getPrice()));
                originalPrice.getStyleClass().add("dashboard-label-strikethrough");
            }

            Text stock = new Text(product.getStock() > 0 ? "In Stock: " + product.getStock() : "Out of Stock");
            stock.getStyleClass().add(product.getStock() > 0 && product.getStock() <= 5 ? "dashboard-label-low" : "dashboard-label");

            Button addToCartButton = new Button("Add to Cart");
            addToCartButton.getStyleClass().add("dashboard-btn");
            addToCartButton.setDisable(product.getStock() <= 0 || SessionManager.getInstance().isGuest());
            addToCartButton.setOnAction(e -> showAddToCartDialog(product));

            card.getChildren().addAll(productImage, name, description, price, originalPrice, stock, addToCartButton);
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

    private void showAddToCartDialog(Product product) {
        Dialog<Integer> dialog = new Dialog<>();
        dialog.setTitle("Add to Cart");
        dialog.setHeaderText("Add " + product.getName() + " to Cart");

        VBox content = new VBox(10);
        content.setPadding(new Insets(15));
        content.setAlignment(Pos.CENTER);

        ImageView productImage = new ImageView();
        productImage.setFitWidth(120);
        productImage.setFitHeight(120);
        productImage.setPreserveRatio(true);
        try {
            String imagePath = product.getImagePath();
            if (imagePath != null && !imagePath.isEmpty()) {
                String resourcePath = "/com/example/maxecommerce/images/" + imagePath;
                Image image = new Image(Objects.requireNonNull(getClass().getResourceAsStream(resourcePath)));
                productImage.setImage(image);
            } else {
                productImage.setImage(new Image(Objects.requireNonNull(getClass().getResourceAsStream("/com/example/maxecommerce/images/default-product.png"))));
            }
        } catch (Exception e) {
            productImage.setImage(new Image(Objects.requireNonNull(getClass().getResourceAsStream("/com/example/maxecommerce/images/default-product.png"))));
            LOGGER.warning("Failed to load dialog image: " + e.getMessage());
        }

        Text description = new Text(product.getDescription() != null ? truncateDescription(product.getDescription(), 100) : "No description");
        description.getStyleClass().add("dashboard-label");

        double discountedPrice = product.getPrice() * (1 - product.getDiscount() / 100);
        Text price = new Text(String.format("Price: ₱%.2f", discountedPrice));
        price.getStyleClass().add("dashboard-label");

        Text originalPrice = new Text();
        if (product.getDiscount() > 0) {
            originalPrice.setText(String.format("Original: ₱%.2f", product.getPrice()));
            originalPrice.getStyleClass().add("dashboard-label-strikethrough");
        }

        Text stock = new Text(product.getStock() > 0 ? "In Stock: " + product.getStock() : "Out of Stock");
        stock.getStyleClass().add(product.getStock() > 0 && product.getStock() <= 5 ? "dashboard-label-low" : "dashboard-label");

        Label quantityLabel = new Label("Quantity:");
        quantityLabel.getStyleClass().add("dashboard-label");
        TextField quantityField = new TextField("1");
        quantityField.setEditable(false);
        quantityField.setPrefWidth(50);
        quantityField.getStyleClass().add("dashboard-text-field");

        Button minusButton = new Button("-");
        minusButton.getStyleClass().add("dashboard-btn");
        minusButton.setDisable(true);

        Button plusButton = new Button("+");
        plusButton.getStyleClass().add("dashboard-btn");
        plusButton.setDisable(product.getStock() <= 1);

        HBox quantityBox = new HBox(5, minusButton, quantityField, plusButton);
        quantityBox.setAlignment(Pos.CENTER);

        minusButton.setOnAction(e -> {
            int quantity = Integer.parseInt(quantityField.getText());
            if (quantity > 1) {
                quantity--;
                quantityField.setText(String.valueOf(quantity));
                minusButton.setDisable(quantity == 1);
                plusButton.setDisable(quantity == product.getStock());
            }
        });

        plusButton.setOnAction(e -> {
            int quantity = Integer.parseInt(quantityField.getText());
            if (quantity < product.getStock()) {
                quantity++;
                quantityField.setText(String.valueOf(quantity));
                minusButton.setDisable(false);
                plusButton.setDisable(quantity == product.getStock());
            }
        });

        content.getChildren().addAll(productImage, description, price, originalPrice, stock, quantityLabel, quantityBox);

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getStylesheets().add(getClass().getResource("/com/example/maxecommerce/admin/admin.css").toExternalForm());

        ButtonType confirmButtonType = new ButtonType("Confirm", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(confirmButtonType, ButtonType.CANCEL);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == confirmButtonType) {
                try {
                    return Integer.parseInt(quantityField.getText());
                } catch (NumberFormatException e) {
                    return 0;
                }
            }
            return 0;
        });

        dialog.getDialogPane().setPrefSize(400, 500);
        Stage dialogStage = (Stage) dialog.getDialogPane().getScene().getWindow();
        dialogStage.setResizable(false);
        dialogStage.centerOnScreen();
        Integer quantity = dialog.showAndWait().orElse(0);
        if (quantity > 0) {
            addToCart(product.getId(), quantity);
        }
    }

    private void addToCart(int productId, int quantity) {
        if (SessionManager.getInstance().isGuest()) {
            showAlert(Alert.AlertType.WARNING, "Login Required", "Please log in to add items to cart.");
            return;
        }

        try (Connection conn = DatabaseConnection.getConnection()) {
            PreparedStatement stockStmt = conn.prepareStatement("SELECT stock FROM Product WHERE product_id = ?");
            stockStmt.setInt(1, productId);
            ResultSet stockRs = stockStmt.executeQuery();
            if (!stockRs.next() || stockRs.getInt("stock") < quantity) {
                showAlert(Alert.AlertType.ERROR, "Error", "Insufficient stock!");
                return;
            }

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

            PreparedStatement itemStmt = conn.prepareStatement(
                    "SELECT cart_item_id, quantity FROM CartItem WHERE cart_id = ? AND product_id = ?");
            itemStmt.setInt(1, cartId);
            itemStmt.setInt(2, productId);
            ResultSet itemRs = itemStmt.executeQuery();
            if (itemRs.next()) {
                int newQuantity = itemRs.getInt("quantity") + quantity;
                PreparedStatement updateItem = conn.prepareStatement(
                        "UPDATE CartItem SET quantity = ? WHERE cart_item_id = ?");
                updateItem.setInt(1, newQuantity);
                updateItem.setInt(2, itemRs.getInt("cart_item_id"));
                updateItem.executeUpdate();
            } else {
                PreparedStatement insertItem = conn.prepareStatement(
                        "INSERT INTO CartItem (cart_id, product_id, quantity) VALUES (?, ?, ?)");
                insertItem.setInt(1, cartId);
                insertItem.setInt(2, productId);
                insertItem.setInt(3, quantity);
                insertItem.executeUpdate();
            }

            showAlert(Alert.AlertType.INFORMATION, "Success", "Added " + quantity + " item(s) to cart!");
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to add to cart: " + e.getMessage());
            LOGGER.severe("Failed to add to cart: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void showCart() {
        if (SessionManager.getInstance().isGuest()) {
            showAlert(Alert.AlertType.WARNING, "Login Required", "Please log in to view your cart.");
            return;
        }

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
            stage.setResizable(false);
            stage.centerOnScreen();
            stage.show();
            LOGGER.info("Opened CartView.fxml");
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to open cart: " + e.getMessage());
            LOGGER.severe("Failed to load CartView.fxml: " + e.getMessage());
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
            LOGGER.severe("Failed to remove cart item: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private int getNextOrderId(Connection conn) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement("SELECT AUTO_INCREMENT FROM information_schema.TABLES WHERE TABLE_NAME = 'orders'")) {
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            }
        }
        return 1;
    }

    public void checkout(List<CartItem> cartItems) {
        if (SessionManager.getInstance().isGuest()) {
            showAlert(Alert.AlertType.WARNING, "Login Required", "Please log in to checkout.");
            return;
        }

        if (cartItems.isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Error", "Cart is empty!");
            return;
        }

        double totalAmount = cartItems.stream().mapToDouble(item -> item.getPrice() * item.getQuantity()).sum();

        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Checkout");
        dialog.setHeaderText("Complete Your Purchase");

        VBox content = new VBox(10);
        content.setPadding(new Insets(20));
        content.setAlignment(Pos.CENTER);

        Label totalLabel = new Label(String.format("Total Amount: ₱%.2f", totalAmount));
        totalLabel.getStyleClass().add("dashboard-label");

        Label locationLabel = new Label("Delivery Location:");
        locationLabel.getStyleClass().add("dashboard-label");
        TextField locationField = new TextField();
        locationField.setPromptText("Enter delivery address");
        locationField.getStyleClass().add("dashboard-text-field");

        Label methodLabel = new Label("Select Payment Method:");
        methodLabel.getStyleClass().add("dashboard-label");
        ChoiceBox<String> methodChoice = new ChoiceBox<>();
        methodChoice.getItems().addAll("Credit Card", "GCash");
        methodChoice.setValue("Credit Card");

        content.getChildren().addAll(totalLabel, locationLabel, locationField, methodLabel, methodChoice);

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getStylesheets().add(getClass().getResource("/com/example/maxecommerce/admin/admin.css").toExternalForm());

        ButtonType confirmButtonType = new ButtonType("Confirm", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(confirmButtonType, ButtonType.CANCEL);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == confirmButtonType && !locationField.getText().trim().isEmpty()) {
                return methodChoice.getValue() + "|" + locationField.getText().trim();
            }
            return null;
        });

        dialog.getDialogPane().setPrefSize(400, 500);
        Stage dialogStage = (Stage) dialog.getDialogPane().getScene().getWindow();
        dialogStage.setResizable(false);
        dialogStage.centerOnScreen();

        String result = dialog.showAndWait().orElse(null);
        if (result == null) {
            showAlert(Alert.AlertType.WARNING, "Cancelled", "Checkout cancelled.");
            return;
        }

        String[] parts = result.split("\\|");
        String selectedMethod = parts[0];
        String deliveryLocation = parts[1];

        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                PreparedStatement orderStmt = conn.prepareStatement(
                        "INSERT INTO orders (user_id, delivery_location, total_amount, payment_method, status, payment_link) VALUES (?, ?, ?, ?, 'pending', ?)",
                        PreparedStatement.RETURN_GENERATED_KEYS);
                orderStmt.setInt(1, customerId);
                orderStmt.setString(2, deliveryLocation);
                orderStmt.setDouble(3, totalAmount);
                orderStmt.setString(4, selectedMethod.toLowerCase());
                String paymentLink = selectedMethod.equalsIgnoreCase("GCash") ? PAYMENT_BASE_URL + "?order_id=" + getNextOrderId(conn) + "&amount=" + totalAmount : null;
                orderStmt.setString(5, paymentLink);
                orderStmt.executeUpdate();

                ResultSet orderKeys = orderStmt.getGeneratedKeys();
                if (!orderKeys.next()) {
                    throw new SQLException("Failed to retrieve order ID.");
                }
                int orderId = orderKeys.getInt(1);

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

                if (selectedMethod.equalsIgnoreCase("Credit Card")) {
                    Stripe.apiKey = STRIPE_API_KEY;
                    SessionCreateParams params = SessionCreateParams.builder()
                            .addPaymentMethodType(SessionCreateParams.PaymentMethodType.CARD)
                            .setMode(SessionCreateParams.Mode.PAYMENT)
                            .addLineItem(
                                    SessionCreateParams.LineItem.builder()
                                            .setPriceData(
                                                    SessionCreateParams.LineItem.PriceData.builder()
                                                            .setCurrency("php")
                                                            .setUnitAmount((long) (totalAmount * 100))
                                                            .setProductData(
                                                                    SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                                                            .setName("Order #" + orderId)
                                                                            .build()
                                                            )
                                                            .build()
                                            )
                                            .setQuantity(1L)
                                            .build()
                            )
                            .setSuccessUrl("http://localhost:8080/success?order_id=" + orderId)
                            .setCancelUrl("http://localhost:8080/cancel")
                            .build();

                    Session session = Session.create(params);
                    openBrowser(session.getUrl());
                    showAlert(Alert.AlertType.INFORMATION, "Payment", "Redirecting to Stripe Checkout...");
                } else if (selectedMethod.equalsIgnoreCase("GCash")) {
                    String paymentUrl = PAYMENT_BASE_URL + "?order_id=" + orderId + "&amount=" + totalAmount;
                    boolean paymentSuccessful = displayQRCode(paymentUrl, orderId, totalAmount, cartItems);
                    if (!paymentSuccessful) {
                        throw new SQLException("GCash payment was not completed.");
                    }
                    PreparedStatement updateOrderStmt = conn.prepareStatement(
                            "UPDATE orders SET status = 'completed', payment_method = ? WHERE order_id = ?");
                    updateOrderStmt.setString(1, selectedMethod.toLowerCase());
                    updateOrderStmt.setInt(2, orderId);
                    updateOrderStmt.executeUpdate();
                }

                PreparedStatement clearCart = conn.prepareStatement(
                        "DELETE FROM CartItem WHERE cart_id = (SELECT cart_id FROM Cart WHERE user_id = ?)");
                clearCart.setInt(1, customerId);
                clearCart.executeUpdate();

                PreparedStatement notifyStmt = conn.prepareStatement(
                        "INSERT INTO Notification (user_id, message) VALUES (?, ?)");
                notifyStmt.setInt(1, customerId);
                notifyStmt.setString(2, "Your order #" + orderId + " has been completed.");
                notifyStmt.executeUpdate();

                conn.commit();
                showAlert(Alert.AlertType.INFORMATION, "Success", String.format("Order placed successfully! Total: ₱%.2f", totalAmount));
            } catch (Exception e) {
                conn.rollback();
                showAlert(Alert.AlertType.ERROR, "Error", "Checkout failed: " + e.getMessage());
                LOGGER.severe("Checkout failed: " + e.getMessage());
                e.printStackTrace();
            }
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Error", "Database connection failed: " + e.getMessage());
            LOGGER.severe("Database connection failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private boolean displayQRCode(String url, int orderId, double totalAmount, List<CartItem> cartItems) {
        try {
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix bitMatrix = qrCodeWriter.encode(url, BarcodeFormat.QR_CODE, 250, 250);
            ByteArrayOutputStream pngOutputStream = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(bitMatrix, "PNG", pngOutputStream);
            ByteArrayInputStream inputStream = new ByteArrayInputStream(pngOutputStream.toByteArray());
            Image qrImage = new Image(inputStream);
            ImageView qrView = new ImageView(qrImage);

            VBox qrBox = new VBox(10);
            qrBox.setAlignment(Pos.CENTER);
            qrBox.setPadding(new Insets(20));

            Label titleLabel = new Label("GCash Payment");
            titleLabel.getStyleClass().add("dashboard-title");

            Label instructionLabel = new Label("Scan with GCash to pay");
            instructionLabel.getStyleClass().add("dashboard-label");

            Label orderLabel = new Label("Order #" + orderId);
            orderLabel.getStyleClass().add("dashboard-label");

            Label amountLabel = new Label(String.format("Total Amount: ₱%.2f", totalAmount));
            amountLabel.getStyleClass().add("dashboard-label");

            Label itemsLabel = new Label("Items:");
            itemsLabel.getStyleClass().add("dashboard-label");
            VBox itemsBox = new VBox(5);
            for (CartItem item : cartItems) {
                Label itemLabel = new Label(String.format("%s (x%d) - ₱%.2f", item.getProductName(), item.getQuantity(), item.getPrice() * item.getQuantity()));
                itemLabel.getStyleClass().add("dashboard-label");
                itemsBox.getChildren().add(itemLabel);
            }

            Label scanLabel = new Label("Simulate Scan (Enter QR Code):");
            scanLabel.getStyleClass().add("dashboard-label");
            TextField scanField = new TextField();
            scanField.setPromptText("Enter code (include '5' to proceed)");
            scanField.getStyleClass().add("dashboard-text-field");
            scanField.setPrefWidth(300);

            Button proceedButton = new Button("Proceed");
            proceedButton.getStyleClass().add("dashboard-btn");
            proceedButton.setDisable(true);

            scanField.textProperty().addListener((obs, oldValue, newValue) -> {
                proceedButton.setDisable(!newValue.contains("5"));
            });

            boolean[] paymentSuccess = {false};
            proceedButton.setOnAction(e -> {
                paymentSuccess[0] = true;
                Stage stage = (Stage) proceedButton.getScene().getWindow();
                stage.close();
            });

            qrBox.getChildren().addAll(titleLabel, instructionLabel, qrView, orderLabel, amountLabel, itemsLabel, itemsBox, scanLabel, scanField, proceedButton);

            Scene qrScene = new Scene(qrBox, 1200, 800);
            qrScene.getStylesheets().add(getClass().getResource("/com/example/maxecommerce/admin/admin.css").toExternalForm());
            Stage qrStage = new Stage();
            qrStage.setScene(qrScene);
            qrStage.setTitle("GCash Payment");
            qrStage.setResizable(false);
            qrStage.centerOnScreen();
            qrStage.showAndWait();

            return paymentSuccess[0];
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to generate QR code: " + e.getMessage());
            LOGGER.severe("QR Code error: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    private void openBrowser(String url) {
        try {
            if (hostServices != null) {
                hostServices.showDocument(url);
            } else {
                java.awt.Desktop.getDesktop().browse(new java.net.URI(url));
            }
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to open browser: " + e.getMessage());
            LOGGER.severe("Browser error: " + e.getMessage());
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
            LOGGER.severe("Failed to load cart: " + e.getMessage());
            e.printStackTrace();
        }
        return cartItems;
    }

    @FXML
    private void goToDashboard() {
        try {
            String fxmlPath;
            String title;
            fxmlPath = "/com/example/maxecommerce/home/HomeView.fxml";
            title = "Home";

            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();
            Stage stage = (Stage) dashboardButton.getScene().getWindow();
            Scene scene = new Scene(root, 1200, 800);
            stage.setScene(scene);
            stage.setTitle(title);
            stage.setWidth(1200);
            stage.setHeight(800);
            stage.setResizable(false);
            stage.centerOnScreen();
            showAlert(Alert.AlertType.INFORMATION, "Success", "Navigated to dashboard!");
            LOGGER.info("Navigated to " + fxmlPath);
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to navigate to dashboard: " + e.getMessage());
            LOGGER.severe("Failed to load dashboard: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void handleLogout() {
        try {
            SessionManager.getInstance().logout();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/maxecommerce/auth/LoginView.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) logoutButton.getScene().getWindow();
            Scene scene = new Scene(root, 1200, 800);
            stage.setScene(scene);
            stage.setTitle("E-Commerce Platform - Login");
            stage.setWidth(1200);
            stage.setHeight(800);
            stage.setResizable(false);
            stage.centerOnScreen();
            showAlert(Alert.AlertType.INFORMATION, "Success", "Logged out successfully!");
            LOGGER.info("Logged out and navigated to LoginView.fxml");
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to log out: " + e.getMessage());
            LOGGER.severe("Failed to load LoginView.fxml: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void goToLogin() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/maxecommerce/auth/LoginView.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) rootPane.getScene().getWindow();
            Scene scene = new Scene(root, 1200, 800);
            stage.setScene(scene);
            stage.setTitle("E-Commerce Platform - Login");
            stage.setWidth(1200);
            stage.setHeight(800);
            stage.setResizable(false);
            stage.centerOnScreen();
            LOGGER.info("Navigated to LoginView.fxml");
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to navigate to login: " + e.getMessage());
            LOGGER.severe("Failed to load LoginView.fxml: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.getDialogPane().getStylesheets().add(getClass().getResource("/com/example/maxecommerce/admin/admin.css").toExternalForm());
        alert.getDialogPane().setMinWidth(400);
        alert.getDialogPane().setMinHeight(200);
        Stage alertStage = (Stage) alert.getDialogPane().getScene().getWindow();
        alertStage.setResizable(false);
        alertStage.centerOnScreen();
        alert.showAndWait();
    }
}