package com.example.maxecommerce.controller;

import com.example.maxecommerce.model.Earnings;
import com.example.maxecommerce.model.Order;
import com.example.maxecommerce.model.Product;
import com.example.maxecommerce.util.DatabaseConnection;
import com.example.maxecommerce.util.SessionManager;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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

import static com.example.maxecommerce.util.SessionManager.LOGGER;

public class VendorDashboardController {

    @FXML
    private BorderPane rootPane;
    @FXML
    private Button homeButton;
    @FXML
    private Button backButton;
    @FXML
    private Button logoutButton;
    @FXML
    private Button refreshButton;
    @FXML
    private TabPane tabPane;
    @FXML
    private TextField salesSearchField;
    @FXML
    private Button salesSearchButton;
    @FXML
    private TableView<Order> salesTable;
    @FXML
    private TableColumn<Order, String> orderIdColumn;
    @FXML
    private TableColumn<Order, String> customerColumn;
    @FXML
    private TableColumn<Order, String> totalAmountColumn;
    @FXML
    private TableColumn<Order, String> salesStatusColumn;
    @FXML
    private TableColumn<Order, String> dateColumn;
    @FXML
    private Button addProductButton;
    @FXML
    private TextField inventorySearchField;
    @FXML
    private Button inventorySearchButton;
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
    private TableColumn<Product, String> statusColumn;
    @FXML
    private TableColumn<Product, Void> inventoryActionsColumn;
    @FXML
    private Label totalEarningsText;
    @FXML
    private TableView<Earnings> earningsTable;
    @FXML
    private TableColumn<Earnings, String> monthColumn;
    @FXML
    private TableColumn<Earnings, String> earningsAmountColumn;

    private static final String IMAGE_DIR = "src/main/resources/com/example/maxecommerce/images/";

    @FXML
    private void initialize() {
        SessionManager session = SessionManager.getInstance();
        if (!session.isLoggedIn()) {
            showAlert(Alert.AlertType.ERROR, "Error", "No user logged in.");
            handleLogout();
            return;
        }
        if (session.isSuspended()) {
            showAlert(Alert.AlertType.ERROR, "Error", "Your account is suspended.");
            handleLogout();
            return;
        }
        if (!session.isVendor()) {
            showAlert(Alert.AlertType.ERROR, "Error", "You do not have vendor privileges.");
            handleBack();
            return;
        }

        // Listen for scene attachment to set window properties
        rootPane.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                Stage stage = (Stage) newScene.getWindow();
                stage.setWidth(1200);
                stage.setHeight(800);
                stage.setResizable(false);
                stage.centerOnScreen();
            }
        });

        setupSalesTable();
        setupInventoryTable();
        setupEarningsTable();
        setupSearchControls();
        loadSales();
        loadInventory();
        loadEarnings();
    }

    private void setupSearchControls() {
        salesSearchButton.setOnAction(event -> loadSales());
        inventorySearchButton.setOnAction(event -> loadInventory());
        refreshButton.setOnAction(event -> {
            salesSearchField.clear();
            inventorySearchField.clear();
            loadSales();
            loadInventory();
            loadEarnings();
            showAlert(Alert.AlertType.INFORMATION, "Success", "Dashboard refreshed.");
        });
    }

    private void setupSalesTable() {
        orderIdColumn.setCellValueFactory(cellData -> new SimpleStringProperty(String.valueOf(cellData.getValue().getOrderId())));
        customerColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getCustomerName()));
        totalAmountColumn.setCellValueFactory(cellData -> new SimpleStringProperty(String.format("₱%.2f", cellData.getValue().getTotalAmount())));
        salesStatusColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getStatus()));
        salesStatusColumn.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(status);
                    switch (status.toLowerCase()) {
                        case "completed":
                            setStyle("-fx-text-fill: #2e7d32; -fx-font-weight: bold;");
                            break;
                        case "pending":
                            setStyle("-fx-text-fill: #f57c00; -fx-font-weight: bold;");
                            break;
                        case "cancelled":
                            setStyle("-fx-text-fill: #d32f2f; -fx-font-weight: bold;");
                            break;
                        default:
                            setStyle("");
                    }
                }
            }
        });
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
                            setStyle("-fx-text-fill: #f57c00; -fx-font-weight: bold;");
                            break;
                        case "approved":
                            setStyle("-fx-text-fill: #2e7d32; -fx-font-weight: bold;");
                            break;
                        case "rejected":
                        case "pending_delete":
                            setStyle("-fx-text-fill: #d32f2f; -fx-font-weight: bold;");
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
            private final HBox pane = new HBox(8, editButton, deleteButton);

            {
                editButton.getStyleClass().add("dashboard-btn");
                deleteButton.getStyleClass().add("dashboard-btn");
                editButton.setOnAction(event -> {
                    Product product = getTableView().getItems().get(getIndex());
                    openEditProductForm(product);
                    loadInventory();
                });
                deleteButton.setOnAction(event -> {
                    Product product = getTableView().getItems().get(getIndex());
                    deleteProduct(product);
                    loadInventory();
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
        String search = salesSearchField.getText().trim();
        String query = "SELECT o.order_id, o.total_amount, o.status, o.created_at, c.first_name, c.last_name " +
                "FROM Orders o " +
                "JOIN user c ON o.user_id = c.user_id " +
                "JOIN OrderItem oi ON o.order_id = oi.order_id " +
                "JOIN Product p ON oi.product_id = p.product_id " +
                "WHERE p.user_id = ?";
        List<String> conditions = new ArrayList<>();
        List<Object> params = new ArrayList<>();
        params.add(SessionManager.getInstance().getCurrentUserId());

        if (!search.isEmpty()) {
            conditions.add("(CONCAT(c.first_name, ' ', c.last_name) LIKE ? OR o.order_id LIKE ?)");
            params.add("%" + search + "%");
            params.add("%" + search + "%");
        }

        if (!conditions.isEmpty()) {
            query += " AND " + String.join(" AND ", conditions);
        }

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            for (int i = 0; i < params.size(); i++) {
                stmt.setObject(i + 1, params.get(i));
            }
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
        if (orders.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "No Data", "No sales found.");
        }
    }

    private void loadInventory() {
        List<Product> products = new ArrayList<>();
        String search = inventorySearchField.getText().trim();
        String query = "SELECT p.product_id, p.name, p.description, c.name AS category_name, p.user_id, p.price, p.stock, p.discount, p.image_path, p.status, p.created_at " +
                "FROM Product p JOIN Category c ON p.category_id = c.category_id " +
                "WHERE p.user_id = ?";
        List<String> conditions = new ArrayList<>();
        List<Object> params = new ArrayList<>();
        params.add(SessionManager.getInstance().getCurrentUserId());

        if (!search.isEmpty()) {
            conditions.add("(p.name LIKE ? OR c.name LIKE ?)");
            params.add("%" + search + "%");
            params.add("%" + search + "%");
        }

        if (!conditions.isEmpty()) {
            query += " AND " + String.join(" AND ", conditions);
        }

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            for (int i = 0; i < params.size(); i++) {
                stmt.setObject(i + 1, params.get(i));
            }
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                products.add(new Product(
                        rs.getInt("product_id"),
                        rs.getString("name"),
                        rs.getString("description"),
                        rs.getString("category_name"),
                        rs.getInt("user_id"),
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
        if (products.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "No Data", "No products found.");
        }
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
                             "WHERE p.user_id = ? AND o.status = 'completed'")) {
            stmt.setInt(1, SessionManager.getInstance().getCurrentUserId());
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
        if (earningsList.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "No Data", "No earnings found.");
        }
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
        confirm.setHeaderText("Delete " + product.getName() + "?");
        confirm.setContentText("This will permanently delete the product. Are you sure?");
        String cssPath = getClass().getResource("/com/example/maxecommerce/vendor/vendor.css") != null
                ? getClass().getResource("/com/example/maxecommerce/vendor/vendor.css").toExternalForm()
                : "";
        if (!cssPath.isEmpty()) {
            confirm.getDialogPane().getStylesheets().add(cssPath);
        } else {
            LOGGER.warning("CSS file not found at /com/example/maxecommerce/vendor/vendor.css");
        }
        confirm.getDialogPane().setMinWidth(400);
        confirm.getDialogPane().setMinHeight(200);

        if (confirm.showAndWait().filter(ButtonType.OK::equals).isPresent()) {
            try (Connection conn = DatabaseConnection.getConnection()) {
                conn.setAutoCommit(false);
                try {

                    PreparedStatement deleteStmt = conn.prepareStatement(
                            "DELETE FROM Product WHERE product_id = ?");
                    deleteStmt.setInt(1, product.getId());



                    
                    int rowsAffected = deleteStmt.executeUpdate();






                    if (rowsAffected > 0) {
                        conn.commit();
                        showAlert(Alert.AlertType.INFORMATION, "Success", "Product '" + product.getName() + "' deleted successfully.");
                        LOGGER.info("Product deleted: product_id=" + product.getId());
                    } else {
                        conn.rollback();
                        showAlert(Alert.AlertType.ERROR, "Error", "Product not found or could not be deleted.");
                        LOGGER.warning("No rows affected for product_id=" + product.getId());
                    }
                } catch (SQLException e) {
                    conn.rollback();
                    showAlert(Alert.AlertType.ERROR, "Error", "Failed to delete product: " + e.getMessage());
                    LOGGER.severe("Error deleting product_id=" + product.getId() + ": " + e.getMessage());
                    e.printStackTrace();
                }
            } catch (SQLException e) {
                showAlert(Alert.AlertType.ERROR, "Error", "Database connection failed: " + e.getMessage());
                LOGGER.severe("Database connection failed: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            LOGGER.info("Product deletion cancelled for product_id=" + product.getId());
        }
    }

    private void showProductForm(Product product, String requestType) {
        try {
            VBox form = new VBox(10);
            form.getStyleClass().add("dashboard-card");
            form.setPadding(new Insets(20));

            Label nameLabel = new Label("Name");
            nameLabel.getStyleClass().add("dashboard-label");
            TextField nameField = new TextField(product != null ? product.getName() : "");
            nameField.getStyleClass().add("dashboard-text-field");

            Label descriptionLabel = new Label("Description");
            descriptionLabel.getStyleClass().add("dashboard-label");
            TextField descriptionField = new TextField(product != null ? product.getDescription() : "");
            descriptionField.getStyleClass().add("dashboard-text-field");

            Label categoryLabel = new Label("Category");
            categoryLabel.getStyleClass().add("dashboard-label");
            TextField categoryField = new TextField(product != null ? product.getCategoryName() : "");
            categoryField.getStyleClass().add("dashboard-text-field");

            Label priceLabel = new Label("Price (₱)");
            priceLabel.getStyleClass().add("dashboard-label");
            TextField priceField = new TextField(product != null ? String.valueOf(product.getPrice()) : "");
            priceField.getStyleClass().add("dashboard-text-field");

            Label stockLabel = new Label("Stock");
            stockLabel.getStyleClass().add("dashboard-label");
            TextField stockField = new TextField(product != null ? String.valueOf(product.getStock()) : "");
            stockField.getStyleClass().add("dashboard-text-field");

            Label discountLabel = new Label("Discount (%)");
            discountLabel.getStyleClass().add("dashboard-label");
            TextField discountField = new TextField(product != null ? String.valueOf(product.getDiscount()) : "");
            discountField.getStyleClass().add("dashboard-text-field");

            Label imageLabel = new Label("Product Image");
            imageLabel.getStyleClass().add("dashboard-label");
            ImageView imageView = new ImageView();
            imageView.setFitWidth(100);
            imageView.setFitHeight(100);
            imageView.setPreserveRatio(true);
            final File[] selectedImage = {null};
            if (product != null && product.getImagePath() != null) {
                try {
                    String imagePath = "/com/example/maxecommerce/images/" + product.getImagePath();
                    Image image = new Image(getClass().getResourceAsStream(imagePath));
                    imageView.setImage(image);
                } catch (Exception e) {
                    imageView.setImage(null);
                }
            }
            Button uploadButton = new Button("Upload Image");
            uploadButton.getStyleClass().add("dashboard-btn");
            uploadButton.setOnAction(e -> {
                FileChooser fileChooser = new FileChooser();
                fileChooser.setTitle("Select Product Image");
                fileChooser.getExtensionFilters().addAll(
                        new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg")
                );
                File file = fileChooser.showOpenDialog(form.getScene().getWindow());
                if (file != null) {
                    selectedImage[0] = file;
                    try {
                        Image image = new Image(file.toURI().toString());
                        imageView.setImage(image);
                    } catch (Exception ex) {
                        showAlert(Alert.AlertType.ERROR, "Error", "Failed to load image: " + ex.getMessage());
                    }
                }
            });

            Label statusLabel = new Label("Status: " + (product != null ? product.getStatus() : "Pending"));
            statusLabel.getStyleClass().add("dashboard-label");

            Button saveButton = new Button(product != null ? "Submit Update" : "Add Product");
            saveButton.getStyleClass().add("dashboard-btn");

            saveButton.setOnAction(e -> {
                try {
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

                    String imagePath = null;
                    if (selectedImage[0] != null) {
                        Path targetDir = Paths.get(IMAGE_DIR);
                        Files.createDirectories(targetDir);
                        String fileName = System.currentTimeMillis() + "_" + selectedImage[0].getName();
                        Path targetPath = targetDir.resolve(fileName);
                        Files.copy(selectedImage[0].toPath(), targetPath);
                        imagePath = fileName;
                    } else if (product != null && product.getImagePath() != null) {
                        imagePath = product.getImagePath();
                    }

                    saveProduct(
                            product != null ? product.getId() : -1,
                            nameField.getText(),
                            descriptionField.getText(),
                            categoryField.getText(),
                            price,
                            stock,
                            discount,
                            imagePath,
                            requestType
                    );
                    loadInventory();
                    ((Stage) saveButton.getScene().getWindow()).close();
                } catch (NumberFormatException ex) {
                    showAlert(Alert.AlertType.ERROR, "Error", "Invalid number format: " + ex.getMessage());
                } catch (IOException ex) {
                    showAlert(Alert.AlertType.ERROR, "Error", "Failed to save image: " + ex.getMessage());
                    ex.printStackTrace();
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
                    imageLabel, imageView, uploadButton,
                    statusLabel, saveButton
            );

            Scene scene = new Scene(form, 400, 700);
            scene.getStylesheets().add(getClass().getResource("/com/example/maxecommerce/admin/admin.css").toExternalForm());
            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle(product != null ? "Edit Product" : "Add Product");
            stage.setScene(scene);
            stage.setWidth(400);
            stage.setHeight(800);
            stage.setResizable(false);
            stage.centerOnScreen();
            stage.show();
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Error", "Error opening form: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void saveProduct(int productId, String name, String description, String categoryName, double price, int stock, double discount, String imagePath, String requestType) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
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
                    PreparedStatement stmt = conn.prepareStatement(
                            "INSERT INTO Product (name, description, category_id, user_id, price, stock, discount, image_path, status) VALUES (?, ?, ?, ?, ?, ?, ?, ?, 'pending')",
                            PreparedStatement.RETURN_GENERATED_KEYS);
                    stmt.setString(1, name);
                    stmt.setString(2, description);
                    stmt.setInt(3, categoryId);
                    stmt.setInt(4, SessionManager.getInstance().getCurrentUserId());
                    stmt.setDouble(5, price);
                    stmt.setInt(6, stock);
                    stmt.setDouble(7, discount);
                    stmt.setString(8, imagePath);
                    stmt.executeUpdate();

                    ResultSet keys = stmt.getGeneratedKeys();
                    keys.next();
                    newProductId = keys.getInt(1);
                } else if (requestType.equals("update")) {
                    PreparedStatement stmt = conn.prepareStatement(
                            "UPDATE Product SET name = ?, description = ?, category_id = ?, price = ?, stock = ?, discount = ?, image_path = ?, status = 'pending' WHERE product_id = ?");
                    stmt.setString(1, name);
                    stmt.setString(2, description);
                    stmt.setInt(3, categoryId);
                    stmt.setDouble(4, price);
                    stmt.setInt(5, stock);
                    stmt.setDouble(6, discount);
                    stmt.setString(7, imagePath);
                    stmt.setInt(8, productId);
                    stmt.executeUpdate();
                }

                PreparedStatement approvalStmt = conn.prepareStatement(
                        "INSERT INTO ProductApproval (product_id, user_id, request_type, status) VALUES (?, ?, ?, 'pending')",
                        PreparedStatement.RETURN_GENERATED_KEYS);
                approvalStmt.setInt(1, newProductId);
                approvalStmt.setInt(2, SessionManager.getInstance().getCurrentUserId());
                approvalStmt.setString(3, requestType);
                approvalStmt.executeUpdate();

                ResultSet keys = approvalStmt.getGeneratedKeys();
                keys.next();
                int requestId = keys.getInt(1);

                PreparedStatement notifyStmt = conn.prepareStatement(
                        "INSERT INTO Notification (user_id, message) VALUES (1, ?)");
                notifyStmt.setString(1, "Vendor submitted " + requestType + " request for product #" + newProductId + " (Request #" + requestId + ")");
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
    private void handleHome() {
        loadScene("/com/example/maxecommerce/home/HomeView.fxml", "Home");
    }

    @FXML
    private void handleBack() {
        handleHome();
    }

    @FXML
    private void handleLogout() {
        SessionManager.getInstance().logout();
        loadScene("/com/example/maxecommerce/auth/LoginView.fxml", "E-Commerce Platform - Login");
    }

    private void loadScene(String fxmlPath, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();
            Stage stage = (Stage) rootPane.getScene().getWindow();
            Scene scene = new Scene(root, 1200, 800);
            stage.setScene(scene);
            stage.setTitle(title);
            stage.setWidth(1400);
            stage.setHeight(800);
            stage.setResizable(true);
            stage.centerOnScreen();
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Error", "Error navigating: " + e.getMessage());
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