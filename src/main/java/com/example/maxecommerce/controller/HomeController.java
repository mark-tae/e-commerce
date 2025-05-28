package com.example.maxecommerce.controller;

import com.example.maxecommerce.model.CartItem;
import com.example.maxecommerce.model.Product;
import com.example.maxecommerce.util.DatabaseConnection;
import com.example.maxecommerce.util.SessionManager;
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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;

public class HomeController {

    private static final Logger LOGGER = Logger.getLogger(HomeController.class.getName());

    @FXML
    private BorderPane rootPane;
    @FXML
    private TextField searchField;
    @FXML
    private Button shopButton;
    @FXML
    private Button vendorButton;
    @FXML
    private Button becomeVendorButton;
    @FXML
    private Button logoutButton;
    @FXML
    private GridPane productGrid;

    private HostServices hostServices;

    public void setHostServices(HostServices hostServices) {
        this.hostServices = hostServices;
        LOGGER.info("HostServices set in HomeController: " + (hostServices != null));
    }

    @FXML
    private void initialize() {
        // Set window properties
        rootPane.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                Stage stage = (Stage) newScene.getWindow();
                stage.setWidth(1200);
                stage.setHeight(800);
                stage.setResizable(true);
                stage.centerOnScreen();
                LOGGER.info("Home window set to 1200x800, centered");
            }
        });

        if (!SessionManager.getInstance().isLoggedIn() && !SessionManager.getInstance().isGuest()) {
            showAlert(Alert.AlertType.ERROR, "Error", "No user logged in.");
            handleLogout();
            return;
        }
        // Show vendor button if user has vendor_status = 'approved'
        vendorButton.setVisible(SessionManager.getInstance().getVendorStatus() != null &&
                SessionManager.getInstance().getVendorStatus().equals("approved"));
        // Show become vendor button if user is not guest and vendor_status is 'none' or 'rejected'
        becomeVendorButton.setVisible(!SessionManager.getInstance().isGuest() &&
                (SessionManager.getInstance().getVendorStatus() == null ||
                        SessionManager.getInstance().getVendorStatus().equals("none") ||
                        SessionManager.getInstance().getVendorStatus().equals("rejected")));
        loadFeaturedProducts("");
        setupSearchListener();
    }

    private void setupSearchListener() {
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            loadFeaturedProducts(newValue.trim());
        });
    }

    @FXML
    private void handleSearch() {
        loadFeaturedProducts(searchField.getText().trim());
    }

    @FXML
    private void handleBecomeVendor() {
        if (SessionManager.getInstance().getVendorStatus() != null &&
                (SessionManager.getInstance().getVendorStatus().equals("approved") ||
                        SessionManager.getInstance().getVendorStatus().equals("pending"))) {
            showAlert(Alert.AlertType.ERROR, "Error", "You already have vendor status or a pending request.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Become a Vendor");
        confirm.setHeaderText("Confirm Vendor Request");
        confirm.setContentText("Are you sure you want to submit a vendor request? This will require admin approval.");
        confirm.getDialogPane().getStylesheets().add(getClass().getResource("/com/example/maxecommerce/admin/admin.css").toExternalForm());
        confirm.getDialogPane().setMinWidth(400);
        confirm.getDialogPane().setMinHeight(200);
        if (!confirm.showAndWait().filter(ButtonType.OK::equals).isPresent()) {
            return;
        }

        try (Connection conn = DatabaseConnection.getConnection()) {
            PreparedStatement checkStmt = conn.prepareStatement(
                    "SELECT request_id FROM VendorRequest WHERE user_id = ? AND status = 'pending'");
            checkStmt.setInt(1, SessionManager.getInstance().getCurrentUserId());
            ResultSet rs = checkStmt.executeQuery();
            if (rs.next()) {
                showAlert(Alert.AlertType.WARNING, "Warning", "You already have a pending vendor request.");
                return;
            }

            PreparedStatement insertStmt = conn.prepareStatement(
                    "INSERT INTO VendorRequest (user_id, status, created_at) VALUES (?, 'pending', NOW())",
                    PreparedStatement.RETURN_GENERATED_KEYS);
            insertStmt.setInt(1, SessionManager.getInstance().getCurrentUserId());
            insertStmt.executeUpdate();

            ResultSet keys = insertStmt.getGeneratedKeys();
            keys.next();
            int requestId = keys.getInt(1);

            PreparedStatement updateUserStmt = conn.prepareStatement(
                    "UPDATE user SET vendor_status = 'pending' WHERE user_id = ?");
            updateUserStmt.setInt(1, SessionManager.getInstance().getCurrentUserId());
            updateUserStmt.executeUpdate();

            PreparedStatement notifyStmt = conn.prepareStatement(
                    "INSERT INTO Notification (user_id, message) VALUES (1, ?)");
            notifyStmt.setString(1, "New vendor request #" + requestId + " from user ID " + SessionManager.getInstance().getCurrentUserId());
            notifyStmt.executeUpdate();

            showAlert(Alert.AlertType.INFORMATION, "Success", "Vendor request submitted. Awaiting admin approval.");
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Error", "Error submitting vendor request: " + e.getMessage());
            LOGGER.severe("Error submitting vendor request: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void loadFeaturedProducts(String searchTerm) {
        productGrid.getChildren().clear();
        String query = "SELECT p.product_id, p.name, p.description, p.price, p.discount, p.stock, p.image_path " +
                "FROM Product p WHERE p.status = 'approved'";
        if (!searchTerm.isEmpty()) {
            query += " AND (LOWER(p.name) LIKE ? OR LOWER(p.description) LIKE ?)";
        }
        query += " LIMIT 4";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            if (!searchTerm.isEmpty()) {
                String likePattern = "%" + searchTerm.toLowerCase() + "%";
                stmt.setString(1, likePattern);
                stmt.setString(2, likePattern);
            }
            ResultSet rs = stmt.executeQuery();
            int row = 0;
            int col = 0;
            while (rs.next()) {
                Product product = new Product(
                        rs.getInt("product_id"),
                        rs.getString("name"),
                        rs.getString("description"),
                        null,
                        rs.getDouble("price"),
                        rs.getInt("stock"),
                        rs.getDouble("discount"),
                        rs.getString("image_path")
                );

                VBox productCard = new VBox(8);
                productCard.getStyleClass().add("dashboard-card");
                productCard.setPrefWidth(300);
                productCard.setPadding(new Insets(10));

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

                Text description = new Text(product.getDescription() != null ?
                        truncateDescription(product.getDescription(), 50) : "No description");
                description.getStyleClass().add("dashboard-label");

                double discountedPrice = product.getPrice() * (1 - product.getDiscount() / 100);
                Text price = new Text(String.format("₱%.2f", discountedPrice));
                price.getStyleClass().add("dashboard-label");

                Text discount = new Text(product.getDiscount() > 0 ?
                        String.format("%.0f%% Off", product.getDiscount()) : "");
                discount.getStyleClass().add("dashboard-label");

                Text stock = new Text(product.getStock() > 0 ?
                        "In Stock: " + product.getStock() : "Out of Stock");
                stock.getStyleClass().add(product.getStock() > 0 && product.getStock() <= 5 ?
                        "dashboard-label-low" : "dashboard-label");

                Button addToCartButton = new Button("Add to Cart");
                addToCartButton.getStyleClass().add("dashboard-btn");
                addToCartButton.setDisable(product.getStock() <= 0 || SessionManager.getInstance().isGuest());
                addToCartButton.setOnAction(e -> addToCart(product.getId()));

                productCard.getChildren().addAll(productImage, name, description, price, discount, stock, addToCartButton);
                productGrid.add(productCard, col, row);
                col++;
                if (col > 2) {
                    col = 0;
                    row++;
                }
            }
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to load products: " + e.getMessage());
            LOGGER.severe("Failed to load products: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private String truncateDescription(String description, int maxLength) {
        if (description.length() <= maxLength) return description;
        return description.substring(0, maxLength - 3) + "...";
    }

    private void addToCart(int productId) {
        if (SessionManager.getInstance().isGuest()) {
            showAlert(Alert.AlertType.ERROR, "Error", "Guests cannot add items to cart. Please log in.");
            return;
        }
        try (Connection conn = DatabaseConnection.getConnection()) {
            PreparedStatement stockStmt = conn.prepareStatement("SELECT stock FROM Product WHERE product_id = ?");
            stockStmt.setInt(1, productId);
            ResultSet stockRs = stockStmt.executeQuery();
            if (!stockRs.next() || stockRs.getInt("stock") <= 0) {
                showAlert(Alert.AlertType.ERROR, "Error", "Product out of stock!");
                return;
            }

            PreparedStatement cartStmt = conn.prepareStatement("SELECT cart_id FROM Cart WHERE user_id = ?");
            cartStmt.setInt(1, SessionManager.getInstance().getCurrentUserId());
            ResultSet cartRs = cartStmt.executeQuery();
            int cartId;
            if (cartRs.next()) {
                cartId = cartRs.getInt("cart_id");
            } else {
                PreparedStatement insertCart = conn.prepareStatement("INSERT INTO Cart (user_id) VALUES (?)",
                        PreparedStatement.RETURN_GENERATED_KEYS);
                insertCart.setInt(1, SessionManager.getInstance().getCurrentUserId());
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
            LOGGER.severe("Failed to add to cart: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void goToShop() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/maxecommerce/product/ProductView.fxml"));
            Parent root = loader.load();
            ProductController controller = loader.getController();
            if (hostServices != null) {
                controller.setHostServices(hostServices);
                LOGGER.info("HostServices injected into ProductController");
            }
            Stage stage = (Stage) shopButton.getScene().getWindow();
            Scene scene = new Scene(root, 1200, 800);
            stage.setScene(scene);
            stage.setTitle("Shop Products");
            stage.setWidth(1400);
            stage.setHeight(800);
            stage.setResizable(true);
            stage.centerOnScreen();
            LOGGER.info("Navigated to ProductView.fxml");
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to load shop: " + e.getMessage());
            LOGGER.severe("Failed to load ProductView.fxml: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void goToVendorDashboard() {
        if (!SessionManager.getInstance().getVendorStatus().equals("approved")) {
            showAlert(Alert.AlertType.ERROR, "Error", "You do not have vendor privileges.");
            return;
        }
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/maxecommerce/vendor/VendorView.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) vendorButton.getScene().getWindow();
            Scene scene = new Scene(root, 1400, 800);
            stage.setScene(scene);
            stage.setTitle("Vendor Dashboard");
            stage.setWidth(1400);
            stage.setHeight(800);
            stage.setResizable(true);
            stage.centerOnScreen();
            LOGGER.info("Navigated to VendorDashboard.fxml");
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to load vendor dashboard: " + e.getMessage());
            LOGGER.severe("Failed to load VendorDashboard.fxml: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void handleLogout() {
        SessionManager.getInstance().logout();
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/maxecommerce/auth/LoginView.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) logoutButton.getScene().getWindow();
            Scene scene = new Scene(root, 1200, 800);
            stage.setScene(scene);
            stage.setTitle("E-Commerce Platform - Login");
            stage.setWidth(500);
            stage.setHeight(800);
            stage.setResizable(true);
            stage.centerOnScreen();
            showAlert(Alert.AlertType.INFORMATION, "Success", "Logged out successfully!");
            LOGGER.info("Logged out and navigated to LoginView.fxml");
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to log out: " + e.getMessage());
            LOGGER.severe("Failed to load LoginView.fxml: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.getDialogPane().getStylesheets().add(getClass().getResource("/com/example/maxecommerce/admin/admin.css").toExternalForm());
        alert.getDialogPane().setMinWidth(400);
        alert.getDialogPane().setMinHeight(200);
        Stage alertStage = (Stage) alert.getDialogPane().getScene().getWindow();
        alertStage.setResizable(false);
        alertStage.centerOnScreen();
        alert.showAndWait();
    }
}