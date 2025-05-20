package com.example.maxecommerce.controller;

import com.example.maxecommerce.model.Earnings;
import com.example.maxecommerce.model.Order;
import com.example.maxecommerce.model.Product;
import com.example.maxecommerce.model.ApprovalRequest;
import com.example.maxecommerce.util.DatabaseConnection;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
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
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class VendorDashboardController {

    @FXML
    private BorderPane rootPane;
    @FXML
    private Button backButton;
    @FXML
    private Button logoutButton;
    @FXML
    private TabPane tabPane;
    @FXML
    private TableView<Order> salesTable;
    @FXML
    private TableColumn<Order, String> orderIdColumn;
    @FXML
    private TableColumn<Order, String> customerColumn;
    @FXML
    private TableColumn<Order, String> totalAmountColumn;
    @FXML
    private TableColumn<Order, String> salesStatusColumn; // Renamed for Sales tab
    @FXML
    private TableColumn<Order, String> dateColumn;
    @FXML
    private Button addProductButton;
    @FXML
    private TableView<Product> inventoryTable;
    @FXML
    private TableColumn<Product, String> productNameColumn;
    @FXML
    private TableColumn<Product, String> categoryColumn;
    @FXML
    private TableColumn<Product, String> priceColumn;
    @FXML
    private TableColumn<Product, String> stockColumn;
    @FXML
    private TableColumn<Product, String> discountColumn;
    @FXML
    private TableColumn<Product, String> statusColumn; // For Inventory tab
    @FXML
    private TableColumn<Product, Void> inventoryActionsColumn;
    @FXML
    private Text totalEarningsText;
    @FXML
    private TableView<Earnings> earningsTable;
    @FXML
    private TableColumn<Earnings, String> monthColumn;
    @FXML
    private TableColumn<Earnings, String> earningsAmountColumn;

    private final int vendorId = 1; // Replace with actual vendor ID from authentication

    @FXML
    private void initialize() {
        setupSalesTable();
        setupInventoryTable();
        setupEarningsTable();
        loadSales();
        loadInventory();
        loadEarnings();
    }

    private void setupSalesTable() {
        orderIdColumn.setCellValueFactory(cellData -> new SimpleStringProperty(String.valueOf(cellData.getValue().getOrderId())));
        customerColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getCustomerName()));
        totalAmountColumn.setCellValueFactory(cellData -> new SimpleStringProperty(String.format("₱%.2f", cellData.getValue().getTotalAmount())));
        salesStatusColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getStatus()));
        dateColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))));
    }

    private void setupInventoryTable() {
        productNameColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getName()));
        categoryColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getCategoryName()));
        priceColumn.setCellValueFactory(cellData -> new SimpleStringProperty(String.format("₱%.2f", cellData.getValue().getPrice())));
        stockColumn.setCellValueFactory(cellData -> new SimpleStringProperty(String.valueOf(cellData.getValue().getStock())));
        discountColumn.setCellValueFactory(cellData -> new SimpleStringProperty(String.format("%.2f%%", cellData.getValue().getDiscount())));
        statusColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getStatus()));
        statusColumn.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(status);
                    switch (status.toLowerCase()) {
                        case "pending":
                            setStyle("-fx-text-fill: #ffc107; -fx-font-weight: bold;");
                            break;
                        case "approved":
                            setStyle("-fx-text-fill: #28a745; -fx-font-weight: bold;");
                            break;
                        case "rejected":
                            setStyle("-fx-text-fill: #dc3545; -fx-font-weight: bold;");
                            break;
                        default:
                            setStyle("");
                    }
                }
            }
        });

        inventoryActionsColumn.setCellFactory(param -> new TableCell<>() {
            private final Button editButton = new Button("Edit");
            private final Button deleteButton = new Button("Delete");
            private final HBox pane = new HBox(5, editButton, deleteButton);

            {
                editButton.getStyleClass().add("dashboard-btn-primary");
                deleteButton.getStyleClass().add("dashboard-btn-danger");
                editButton.setOnAction(event -> {
                    Product product = getTableView().getItems().get(getIndex());
                    openEditProductForm(product);
                });
                deleteButton.setOnAction(event -> {
                    Product product = getTableView().getItems().get(getIndex());
                    deleteProduct(product);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : pane);
            }
        });
    }

    private void setupEarningsTable() {
        monthColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getMonth()));
        earningsAmountColumn.setCellValueFactory(cellData -> new SimpleStringProperty(String.format("₱%.2f", cellData.getValue().getAmount())));
    }

    private void loadSales() {
        List<Order> orders = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT o.order_id, o.total_amount, o.status, o.created_at, c.first_name, c.last_name " +
                             "FROM Orders o " +
                             "JOIN user c ON o.user_id = c.user_id " +
                             "JOIN OrderItem oi ON o.order_id = oi.order_id " +
                             "JOIN Product p ON oi.product_id = p.product_id " +
                             "WHERE p.vendor_id = ?")) {
            stmt.setInt(1, vendorId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                orders.add(new Order(
                        rs.getInt("order_id"),
                        rs.getString("first_name") + " " + rs.getString("last_name"),
                        rs.getDouble("total_amount"),
                        rs.getString("status"),
                        rs.getTimestamp("created_at").toLocalDateTime()
                ));
            }
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Error", "Error loading sales: " + e.getMessage());
            e.printStackTrace();
        }
        salesTable.getItems().setAll(orders);
    }

    private void loadInventory() {
        List<Product> products = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT p.product_id, p.name, p.description, c.name AS category_name, p.vendor_id, p.price, p.stock, p.discount, p.image_path, p.status, p.created_at " +
                             "FROM Product p JOIN Category c ON p.category_id = c.category_id " +
                             "WHERE p.vendor_id = ?")) {
            stmt.setInt(1, vendorId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                products.add(new Product(
                        rs.getInt("product_id"),
                        rs.getString("name"),
                        rs.getString("description"),
                        rs.getString("category_name"),
                        rs.getInt("vendor_id"),
                        rs.getDouble("price"),
                        rs.getInt("stock"),
                        rs.getDouble("discount"),
                        rs.getString("image_path"),
                        rs.getString("status"),
                        rs.getTimestamp("created_at").toLocalDateTime()
                ));
            }
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Error", "Error loading inventory: " + e.getMessage());
            e.printStackTrace();
        }
        inventoryTable.getItems().setAll(products);
    }

    private void loadEarnings() {
        double totalEarnings = 0;
        Map<String, Double> monthlyEarnings = new HashMap<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT oi.price_at_purchase, oi.quantity, o.created_at " +
                             "FROM OrderItem oi " +
                             "JOIN Orders o ON oi.order_id = o.order_id " +
                             "JOIN Product p ON oi.product_id = p.product_id " +
                             "WHERE p.vendor_id = ? AND o.status = 'completed'")) {
            stmt.setInt(1, vendorId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                double amount = rs.getDouble("price_at_purchase") * rs.getInt("quantity");
                LocalDateTime date = rs.getTimestamp("created_at").toLocalDateTime();
                String monthKey = date.format(DateTimeFormatter.ofPattern("yyyy-MM"));
                totalEarnings += amount;
                monthlyEarnings.merge(monthKey, amount, Double::sum);
            }
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Error", "Error loading earnings: " + e.getMessage());
            e.printStackTrace();
        }

        totalEarningsText.setText(String.format("Total Earnings: ₱%.2f", totalEarnings));
        List<Earnings> earningsList = new ArrayList<>();
        monthlyEarnings.forEach((month, amount) -> earningsList.add(new Earnings(month, amount)));
        earningsTable.getItems().setAll(earningsList);
    }

    @FXML
    private void openAddProductForm() {
        showProductForm(null, "create");
    }

    private void openEditProductForm(Product product) {
        showProductForm(product, "update");
    }

    private void deleteProduct(Product product) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete Product");
        confirm.setHeaderText("Are you sure you want to delete " + product.getName() + "?");
        confirm.setContentText("This action requires admin approval.");
        if (confirm.showAndWait().filter(ButtonType.OK::equals).isPresent()) {
            try (Connection conn = DatabaseConnection.getConnection()) {
                conn.setAutoCommit(false);
                try {
                    // Insert approval request
                    PreparedStatement approvalStmt = conn.prepareStatement(
                            "INSERT INTO ProductApproval (product_id, vendor_id, request_type, status) VALUES (?, ?, 'delete', 'pending')",
                            PreparedStatement.RETURN_GENERATED_KEYS);
                    approvalStmt.setInt(1, product.getId());
                    approvalStmt.setInt(2, vendorId);
                    approvalStmt.executeUpdate();

                    ResultSet keys = approvalStmt.getGeneratedKeys();
                    keys.next();
                    int requestId = keys.getInt(1);

                    // Notify admin
                    PreparedStatement notifyStmt = conn.prepareStatement(
                            "INSERT INTO Notification (user_id, message) VALUES (?, ?)");
                    notifyStmt.setInt(1, 1); // Admin ID 1
                    notifyStmt.setString(2, "Vendor requested deletion of product #" + product.getId() + " (Request #" + requestId + ")");
                    notifyStmt.executeUpdate();

                    conn.commit();
                    showAlert(Alert.AlertType.INFORMATION, "Success", "Deletion request submitted for approval.");
                } catch (SQLException e) {
                    conn.rollback();
                    showAlert(Alert.AlertType.ERROR, "Error", "Failed to submit deletion request: " + e.getMessage());
                    e.printStackTrace();
                }
            } catch (SQLException e) {
                showAlert(Alert.AlertType.ERROR, "Error", "Database connection failed: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    private void showProductForm(Product product, String requestType) {
        try {
            VBox form = new VBox(10);
            form.getStyleClass().add("dashboard-form");
            form.setPadding(new Insets(20));

            TextField nameField = new TextField(product != null ? product.getName() : "");
            nameField.getStyleClass().add("dashboard-text-field");
            TextField descriptionField = new TextField(product != null ? product.getDescription() : "");
            descriptionField.getStyleClass().add("dashboard-text-field");
            TextField categoryField = new TextField(product != null ? product.getCategoryName() : "");
            categoryField.getStyleClass().add("dashboard-text-field");
            TextField priceField = new TextField(product != null ? String.valueOf(product.getPrice()) : "");
            priceField.getStyleClass().add("dashboard-text-field");
            TextField stockField = new TextField(product != null ? String.valueOf(product.getStock()) : "");
            stockField.getStyleClass().add("dashboard-text-field");
            TextField discountField = new TextField(product != null ? String.valueOf(product.getDiscount()) : "");
            discountField.getStyleClass().add("dashboard-text-field");
            Label statusLabel = new Label("Status: " + (product != null ? product.getStatus() : "Pending"));
            statusLabel.getStyleClass().add("dashboard-label");

            Button saveButton = new Button(product != null ? "Submit Update" : "Add Product");
            saveButton.getStyleClass().add("dashboard-btn-primary");

            // Create and style labels
            Label nameLabel = new Label("Name");
            nameLabel.getStyleClass().add("dashboard-label");
            Label descriptionLabel = new Label("Description");
            descriptionLabel.getStyleClass().add("dashboard-label");
            Label categoryLabel = new Label("Category");
            categoryLabel.getStyleClass().add("dashboard-label");
            Label priceLabel = new Label("Price (₱)");
            priceLabel.getStyleClass().add("dashboard-label");
            Label stockLabel = new Label("Stock");
            stockLabel.getStyleClass().add("dashboard-label");
            Label discountLabel = new Label("Discount (%)");
            discountLabel.getStyleClass().add("dashboard-label");

            saveButton.setOnAction(e -> {
                try {
                    // Validate inputs
                    if (nameField.getText().trim().isEmpty() || categoryField.getText().trim().isEmpty()) {
                        showAlert(Alert.AlertType.ERROR, "Error", "Name and Category are required.");
                        return;
                    }
                    double price = Double.parseDouble(priceField.getText());
                    int stock = Integer.parseInt(stockField.getText());
                    double discount = discountField.getText().isEmpty() ? 0.0 : Double.parseDouble(discountField.getText());
                    if (price <= 0 || stock < 0 || discount < 0 || discount > 100) {
                        showAlert(Alert.AlertType.ERROR, "Error", "Invalid price, stock, or discount.");
                        return;
                    }

                    saveProduct(
                            product != null ? product.getId() : -1,
                            nameField.getText(),
                            descriptionField.getText(),
                            categoryField.getText(),
                            price,
                            stock,
                            discount,
                            requestType
                    );
                    loadInventory();
                    ((Stage) saveButton.getScene().getWindow()).close();
                } catch (NumberFormatException ex) {
                    showAlert(Alert.AlertType.ERROR, "Error", "Invalid number format: " + ex.getMessage());
                } catch (SQLException ex) {
                    showAlert(Alert.AlertType.ERROR, "Error", "Database error: " + ex.getMessage());
                    ex.printStackTrace();
                }
            });

            form.getChildren().addAll(
                    nameLabel, nameField,
                    descriptionLabel, descriptionField,
                    categoryLabel, categoryField,
                    priceLabel, priceField,
                    stockLabel, stockField,
                    discountLabel, discountField,
                    statusLabel,
                    saveButton
            );

            Scene scene = new Scene(form, 400, 600);
            scene.getStylesheets().add(getClass().getResource("/com/example/maxecommerce/vendor/vendor.css").toExternalForm());
            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle(product != null ? "Edit Product" : "Add Product");
            stage.setScene(scene);
            stage.show();
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Error", "Error opening form: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void saveProduct(int productId, String name, String description, String categoryName, double price, int stock, double discount, String requestType) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                // Get or create category
                int categoryId;
                PreparedStatement catStmt = conn.prepareStatement("SELECT category_id FROM Category WHERE name = ?");
                catStmt.setString(1, categoryName);
                ResultSet catRs = catStmt.executeQuery();
                if (catRs.next()) {
                    categoryId = catRs.getInt("category_id");
                } else {
                    PreparedStatement insertCat = conn.prepareStatement("INSERT INTO Category (name) VALUES (?)", PreparedStatement.RETURN_GENERATED_KEYS);
                    insertCat.setString(1, categoryName);
                    insertCat.executeUpdate();
                    ResultSet keys = insertCat.getGeneratedKeys();
                    keys.next();
                    categoryId = keys.getInt(1);
                }

                int newProductId = productId;
                if (requestType.equals("create")) {
                    // Insert product with pending status
                    PreparedStatement stmt = conn.prepareStatement(
                            "INSERT INTO Product (name, description, category_id, vendor_id, price, stock, discount, status) VALUES (?, ?, ?, ?, ?, ?, ?, 'pending')",
                            PreparedStatement.RETURN_GENERATED_KEYS);
                    stmt.setString(1, name);
                    stmt.setString(2, description);
                    stmt.setInt(3, categoryId);
                    stmt.setInt(4, vendorId);
                    stmt.setDouble(5, price);
                    stmt.setInt(6, stock);
                    stmt.setDouble(7, discount);
                    stmt.executeUpdate();

                    ResultSet keys = stmt.getGeneratedKeys();
                    keys.next();
                    newProductId = keys.getInt(1);
                } else if (requestType.equals("update")) {
                    // Store update as pending approval
                    PreparedStatement stmt = conn.prepareStatement(
                            "UPDATE Product SET name = ?, description = ?, category_id = ?, price = ?, stock = ?, discount = ?, status = 'pending' WHERE product_id = ?");
                    stmt.setString(1, name);
                    stmt.setString(2, description);
                    stmt.setInt(3, categoryId);
                    stmt.setDouble(4, price);
                    stmt.setInt(5, stock);
                    stmt.setDouble(6, discount);
                    stmt.setInt(7, productId);
                    stmt.executeUpdate();
                }

                // Insert approval request
                PreparedStatement approvalStmt = conn.prepareStatement(
                        "INSERT INTO ProductApproval (product_id, vendor_id, request_type, status) VALUES (?, ?, ?, 'pending')",
                        PreparedStatement.RETURN_GENERATED_KEYS);
                approvalStmt.setInt(1, newProductId);
                approvalStmt.setInt(2, vendorId);
                approvalStmt.setString(3, requestType);
                approvalStmt.executeUpdate();

                ResultSet keys = approvalStmt.getGeneratedKeys();
                keys.next();
                int requestId = keys.getInt(1);

                // Notify admin
                PreparedStatement notifyStmt = conn.prepareStatement(
                        "INSERT INTO Notification (user_id, message) VALUES (?, ?)");
                notifyStmt.setInt(1, 1); // Admin ID 1
                notifyStmt.setString(2, "Vendor submitted " + requestType + " request for product #" + newProductId + " (Request #" + requestId + ")");
                notifyStmt.executeUpdate();

                conn.commit();
                showAlert(Alert.AlertType.INFORMATION, "Success", requestType.equals("create") ? "Product added, pending approval." : "Update submitted for approval.");
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        }
    }

    @FXML
    private void handleBack() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/maxecommerce/view/HomeView.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) backButton.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Customer Dashboard");
            showAlert(Alert.AlertType.INFORMATION, "Success", "Navigated to dashboard!");
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Error", "Error navigating to dashboard: " + e.getMessage());
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
            showAlert(Alert.AlertType.ERROR, "Error", "Error logging out: " + e.getMessage());
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