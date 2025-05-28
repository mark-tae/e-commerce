package com.example.maxecommerce.controller;

import com.example.maxecommerce.model.*;
import com.example.maxecommerce.util.DatabaseConnection;
import com.example.maxecommerce.util.SessionManager;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;

import java.io.File;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;

public class AdminDashboardController {

    private static final Logger LOGGER = Logger.getLogger(AdminDashboardController.class.getName());

    @FXML
    private BorderPane rootPane;
    @FXML
    private Button logoutButton;
    @FXML
    private ComboBox<String> statusFilter;
    @FXML
    private ComboBox<String> typeFilter;
    @FXML
    private Button refreshButton;
    @FXML
    private TextField searchField;
    @FXML
    private Button searchButton;
    @FXML
    private Label totalProductsLabel;
    @FXML
    private Label totalUsersLabel;
    @FXML
    private TableView<ApprovalRequest> approvalTable;
    @FXML
    private TableColumn<ApprovalRequest, String> requestIdColumn;
    @FXML
    private TableColumn<ApprovalRequest, String> productIdColumn;
    @FXML
    private TableColumn<ApprovalRequest, String> productNameColumn;
    @FXML
    private TableColumn<ApprovalRequest, String> approvalUserIdColumn;
    @FXML
    private TableColumn<ApprovalRequest, String> vendorColumn;
    @FXML
    private TableColumn<ApprovalRequest, String> requestTypeColumn;
    @FXML
    private TableColumn<ApprovalRequest, String> statusColumn;
    @FXML
    private TableColumn<ApprovalRequest, String> adminCommentsColumn;
    @FXML
    private TableColumn<ApprovalRequest, String> createdAtColumn;
    @FXML
    private TableColumn<ApprovalRequest, Void> actionsColumn;
    @FXML
    private TableView<VendorRequest> vendorRequestsTable;
    @FXML
    private TableColumn<VendorRequest, String> vendorRequestIdColumn;
    @FXML
    private TableColumn<VendorRequest, String> vendorUserIdColumn;
    @FXML
    private TableColumn<VendorRequest, String> vendorUserEmailColumn;
    @FXML
    private TableColumn<VendorRequest, String> vendorStatusColumn;
    @FXML
    private TableColumn<VendorRequest, String> vendorCreatedAtColumn;
    @FXML
    private TableColumn<VendorRequest, Void> vendorActionsColumn;
    @FXML
    private TextField vendorSearchField;
    @FXML
    private Button vendorSearchButton;
    @FXML
    private TableView<User> userTable;
    @FXML
    private TableColumn<User, String> userIdColumn;
    @FXML
    private TableColumn<User, String> userNameColumn;
    @FXML
    private TableColumn<User, String> userRoleColumn;
    @FXML
    private TableColumn<User, String> userEmailColumn;
    @FXML
    private TextField userSearchField;
    @FXML
    private Button userSearchButton;
    @FXML
    private TableView<AdminOrder> transactionTable;
    @FXML
    private TableColumn<AdminOrder, String> orderIdColumn;
    @FXML
    private TableColumn<AdminOrder, String> transactionUserIdColumn;
    @FXML
    private TableColumn<AdminOrder, String> customerNameColumn;
    @FXML
    private TableColumn<AdminOrder, String> totalAmountColumn;
    @FXML
    private TableColumn<AdminOrder, String> paymentMethodColumn;
    @FXML
    private TableColumn<AdminOrder, String> orderStatusColumn;
    @FXML
    private TableColumn<AdminOrder, String> orderedAtColumn;
    @FXML
    private TextField transactionSearchField;
    @FXML
    private ComboBox<String> transactionStatusFilter;
    @FXML
    private Button transactionSearchButton;

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
        if (!session.isAdmin()) {
            showAlert(Alert.AlertType.ERROR, "Error", "Admin access required.");
            handleBack();
            return;
        }
        try {
            rootPane.sceneProperty().addListener((obs, oldScene, newScene) -> {
                if (newScene != null && newScene.getWindow() != null) {
                    Stage stage = (Stage) newScene.getWindow();
                    stage.setWidth(1200);
                    stage.setHeight(800);
                    stage.setResizable(true);
                    stage.centerOnScreen();
                    LOGGER.info("Admin dashboard window set to 1200x800, centered");
                }
            });

            setupFilters();
            setupApprovalTable();
            setupVendorRequestsTable();
            setupUserTable();
            setupTransactionTable();
            loadStatistics();
            loadApprovalRequests();
            loadVendorRequests();
            loadUsers();
            loadTransactions();
        } catch (Exception e) {
            LOGGER.severe("Error initializing AdminDashboard: " + e.getMessage());
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Error", "Error initializing dashboard: " + e.getMessage());
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

    private void setupFilters() {
        statusFilter.setItems(FXCollections.observableArrayList("All", "Pending", "Approved", "Rejected"));
        statusFilter.getSelectionModel().select("All");
        statusFilter.setOnAction(event -> loadApprovalRequests());

        typeFilter.setItems(FXCollections.observableArrayList("All", "Create", "Update", "Delete"));
        typeFilter.getSelectionModel().select("All");
        typeFilter.setOnAction(event -> loadApprovalRequests());

        transactionStatusFilter.setItems(FXCollections.observableArrayList("All", "Pending", "Completed"));
        transactionStatusFilter.getSelectionModel().select("All");
        transactionStatusFilter.setOnAction(event -> loadTransactions());

        refreshButton.setOnAction(event -> {
            searchField.clear();
            vendorSearchField.clear();
            userSearchField.clear();
            transactionSearchField.clear();
            statusFilter.getSelectionModel().select("All");
            typeFilter.getSelectionModel().select("All");
            transactionStatusFilter.getSelectionModel().select("All");
            loadStatistics();
            loadApprovalRequests();
            loadVendorRequests();
            loadUsers();
            loadTransactions();
            showAlert(Alert.AlertType.INFORMATION, "Success", "Dashboard refreshed.");
        });

        searchButton.setOnAction(event -> loadApprovalRequests());
        vendorSearchButton.setOnAction(event -> loadVendorRequests());
        userSearchButton.setOnAction(event -> loadUsers());
        transactionSearchButton.setOnAction(event -> loadTransactions());
    }

    private void setupApprovalTable() {
        requestIdColumn.setCellValueFactory(cellData -> new SimpleStringProperty(String.valueOf(cellData.getValue().getRequestId())));
        productIdColumn.setCellValueFactory(cellData -> new SimpleStringProperty(String.valueOf(cellData.getValue().getProductId())));
        productNameColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getProductName()));
        approvalUserIdColumn.setCellValueFactory(cellData -> new SimpleStringProperty(String.valueOf(cellData.getValue().getUserId())));
        vendorColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getVendorName()));
        requestTypeColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getRequestType()));
        requestTypeColumn.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String type, boolean empty) {
                super.updateItem(type, empty);
                if (empty || type == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(type);
                    switch (type.toLowerCase()) {
                        case "create":
                            setStyle("-fx-text-fill: #0288d1; -fx-font-weight: bold;");
                            break;
                        case "update":
                            setStyle("-fx-text-fill: #388e3c; -fx-font-weight: bold;");
                            break;
                        case "delete":
                            setStyle("-fx-text-fill: #d32f2f; -fx-font-weight: bold;");
                            break;
                        default:
                            setStyle("");
                    }
                }
            }
        });
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
                            setStyle("-fx-text-fill: #d32f2f; -fx-font-weight: bold;");
                            break;
                        default:
                            setStyle("");
                    }
                }
            }
        });
        adminCommentsColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getAdminComments() != null ? cellData.getValue().getAdminComments() : ""));
        createdAtColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))));

        actionsColumn.setCellFactory(param -> new TableCell<>() {
            private final Button viewButton = new Button("View");
            private final Button approveButton = new Button("Approve");
            private final Button rejectButton = new Button("Reject");
            private final HBox pane = new HBox(8, viewButton, approveButton, rejectButton);

            {
                viewButton.getStyleClass().add("dashboard-btn");
                approveButton.getStyleClass().add("dashboard-btn");
                rejectButton.getStyleClass().add("dashboard-btn");
                viewButton.setOnAction(event -> {
                    ApprovalRequest request = getTableView().getItems().get(getIndex());
                    showProductDetails(request.getProductId());
                });
                approveButton.setOnAction(event -> {
                    ApprovalRequest request = getTableView().getItems().get(getIndex());
                    if (request.getStatus().equalsIgnoreCase("pending")) {
                        processProductRequest(request, "approved");
                    } else {
                        showAlert(Alert.AlertType.WARNING, "Warning", "Request already processed.");
                    }
                });
                rejectButton.setOnAction(event -> {
                    ApprovalRequest request = getTableView().getItems().get(getIndex());
                    if (request.getStatus().equalsIgnoreCase("pending")) {
                        processProductRequest(request, "rejected");
                    } else {
                        showAlert(Alert.AlertType.WARNING, "Warning", "Request already processed.");
                    }
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : pane); // Temporary: Show buttons for all rows
                if (!empty) {
                    ApprovalRequest request = getTableView().getItems().get(getIndex());
                    System.out.println("Approval Request Status: " + request.getStatus());
                }
            }
        });
    }

    private void setupVendorRequestsTable() {
        vendorRequestIdColumn.setCellValueFactory(cellData -> new SimpleStringProperty(String.valueOf(cellData.getValue().getRequestId())));
        vendorUserIdColumn.setCellValueFactory(cellData -> new SimpleStringProperty(String.valueOf(cellData.getValue().getUserId())));
        vendorUserEmailColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getUserEmail()));
        vendorStatusColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getStatus()));
        vendorStatusColumn.setCellFactory(column -> new TableCell<>() {
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
                            setStyle("-fx-text-fill: #d32f2f; -fx-font-weight: bold;");
                            break;
                        default:
                            setStyle("");
                    }
                }
            }
        });
        vendorCreatedAtColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))));
        vendorActionsColumn.setCellFactory(param -> new TableCell<>() {
            private final Button approveButton = new Button("Approve");
            private final Button rejectButton = new Button("Reject");
            private final HBox pane = new HBox(8, approveButton, rejectButton);

            {
                approveButton.getStyleClass().add("dashboard-btn");
                rejectButton.getStyleClass().add("dashboard-btn");
                approveButton.setOnAction(event -> {
                    VendorRequest request = getTableView().getItems().get(getIndex());
                    if (request.getStatus().equalsIgnoreCase("pending")) {
                        processVendorRequest(request, "approved");
                    } else {
                        showAlert(Alert.AlertType.WARNING, "Warning", "Request already processed.");
                    }
                });
                rejectButton.setOnAction(event -> {
                    VendorRequest request = getTableView().getItems().get(getIndex());
                    if (request.getStatus().equalsIgnoreCase("pending")) {
                        processVendorRequest(request, "rejected");
                    } else {
                        showAlert(Alert.AlertType.WARNING, "Warning", "Request already processed.");
                    }
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : pane); // Temporary: Show buttons for all rows
                if (!empty) {
                    VendorRequest request = getTableView().getItems().get(getIndex());
                    System.out.println("Vendor Request Status: " + request.getStatus());
                }
            }
        });
    }

    private void setupUserTable() {
        userIdColumn.setCellValueFactory(cellData -> new SimpleStringProperty(String.valueOf(cellData.getValue().getUserId())));
        userNameColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getName()));
        userRoleColumn.setCellValueFactory(cellData -> {
            User user = cellData.getValue();
            String role = user.getUserId() == 1 ? "admin" :
                    "approved".equals(user.getVendorStatus()) ? "vendor" : "customer";
            return new SimpleStringProperty(role);
        });
        userRoleColumn.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String role, boolean empty) {
                super.updateItem(role, empty);
                if (empty || role == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(role);
                    switch (role.toLowerCase()) {
                        case "admin":
                            setStyle("-fx-text-fill: #8B5CF6; -fx-font-weight: bold;");
                            break;
                        case "vendor":
                            setStyle("-fx-text-fill: #0288d1; -fx-font-weight: bold;");
                            break;
                        case "customer":
                            setStyle("-fx-text-fill: #388e3c; -fx-font-weight: bold;");
                            break;
                        default:
                            setStyle("");
                    }
                }
            }
        });
        userEmailColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getEmail()));
    }

    private void setupTransactionTable() {
        orderIdColumn.setCellValueFactory(cellData -> new SimpleStringProperty(String.valueOf(cellData.getValue().getOrderId())));
        transactionUserIdColumn.setCellValueFactory(cellData -> new SimpleStringProperty(String.valueOf(cellData.getValue().getUserId())));
        customerNameColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getCustomerName()));
        totalAmountColumn.setCellValueFactory(cellData -> new SimpleStringProperty(String.format("₱%.2f", cellData.getValue().getTotalAmount())));
        paymentMethodColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getPaymentMethod()));
        orderStatusColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getStatus()));
        orderStatusColumn.setCellFactory(column -> new TableCell<>() {
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
                        case "completed":
                            setStyle("-fx-text-fill: #2e7d32; -fx-font-weight: bold;");
                            break;
                        default:
                            setStyle("");
                    }
                }
            }
        });
        orderedAtColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))));
    }

    private void loadStatistics() {
        try (Connection conn = DatabaseConnection.getConnection()) {
            try (PreparedStatement productStmt = conn.prepareStatement("SELECT COUNT(*) AS count FROM Product");
                 ResultSet productRs = productStmt.executeQuery()) {
                if (productRs.next()) {
                    totalProductsLabel.setText("Total Products: " + productRs.getInt("count"));
                }
            }
            try (PreparedStatement userStmt = conn.prepareStatement("SELECT COUNT(*) AS count FROM user");
                 ResultSet userRs = userStmt.executeQuery()) {
                if (userRs.next()) {
                    totalUsersLabel.setText("Total Users: " + userRs.getInt("count"));
                }
            }
        } catch (SQLException e) {
            LOGGER.severe("Error loading statistics: " + e.getMessage());
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Error", "Error loading statistics: " + e.getMessage());
        }
    }

    private void loadApprovalRequests() {
        List<ApprovalRequest> requests = new ArrayList<>();
        String status = statusFilter.getSelectionModel().getSelectedItem();
        String type = typeFilter.getSelectionModel().getSelectedItem();
        String search = searchField.getText().trim();

        String query = "SELECT pa.request_id, pa.product_id, pa.user_id, pa.request_type, pa.status, pa.admin_comments, pa.created_at, p.name AS product_name, CONCAT(u.first_name, ' ', u.last_name) AS vendor_name " +
                "FROM ProductApproval pa " +
                "JOIN Product p ON pa.product_id = p.product_id " +
                "JOIN user u ON pa.user_id = u.user_id";
        List<String> conditions = new ArrayList<>();
        List<Object> params = new ArrayList<>();

        if (!status.equals("All")) {
            conditions.add("pa.status = ?");
            params.add(status.toLowerCase());
        }
        if (!type.equals("All")) {
            conditions.add("pa.request_type = ?");
            params.add(type.toLowerCase());
        }
        if (!search.isEmpty()) {
            conditions.add("(p.name LIKE ? OR CONCAT(u.first_name, ' ', u.last_name) LIKE ?)");
            params.add("%" + search + "%");
            params.add("%" + search + "%");
        }

        if (!conditions.isEmpty()) {
            query += " WHERE " + String.join(" AND ", conditions);
        }
        query += " ORDER BY CASE WHEN pa.request_type = 'create' THEN 1 WHEN pa.request_type = 'delete' THEN 2 ELSE 3 END, pa.created_at DESC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            for (int i = 0; i < params.size(); i++) {
                stmt.setString(i + 1, params.get(i).toString());
            }
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                requests.add(new ApprovalRequest(
                        rs.getInt("request_id"),
                        rs.getInt("product_id"),
                        rs.getString("product_name"),
                        rs.getInt("user_id"),
                        rs.getString("vendor_name"),
                        rs.getString("request_type"),
                        rs.getString("status"),
                        rs.getString("admin_comments"),
                        rs.getTimestamp("created_at").toLocalDateTime()
                ));
            }
        } catch (SQLException e) {
            LOGGER.severe("Error loading approval requests: " + e.getMessage());
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Error", "Error loading approval requests: " + e.getMessage());
        }
        System.out.println("Loaded " + requests.size() + " approval requests");
        for (ApprovalRequest req : requests) {
            System.out.println("Request ID: " + req.getRequestId() + ", Status: " + req.getStatus());
        }
        approvalTable.getItems().setAll(requests);
        if (requests.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "No Data", "No approval requests found.");
        }
    }

    private void loadVendorRequests() {
        List<VendorRequest> requests = new ArrayList<>();
        String search = vendorSearchField.getText().trim();

        String query = "SELECT vr.request_id, vr.user_id, u.email, vr.status, vr.created_at " +
                "FROM VendorRequest vr JOIN user u ON vr.user_id = u.user_id";
        List<String> conditions = new ArrayList<>();
        List<Object> params = new ArrayList<>();

        if (!search.isEmpty()) {
            conditions.add("u.email LIKE ?");
            params.add("%" + search + "%");
        }

        if (!conditions.isEmpty()) {
            query += " WHERE " + String.join(" AND ", conditions);
        }
        query += " ORDER BY vr.created_at DESC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            for (int i = 0; i < params.size(); i++) {
                stmt.setString(i + 1, params.get(i).toString());
            }
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                requests.add(new VendorRequest(
                        rs.getInt("request_id"),
                        rs.getInt("user_id"),
                        rs.getString("email"),
                        rs.getString("status"),
                        rs.getTimestamp("created_at").toLocalDateTime()
                ));
            }
        } catch (SQLException e) {
            LOGGER.severe("Error loading vendor requests: " + e.getMessage());
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Error", "Error loading vendor requests: " + e.getMessage());
        }
        System.out.println("Loaded " + requests.size() + " vendor requests");
        for (VendorRequest req : requests) {
            System.out.println("Request ID: " + req.getRequestId() + ", Status: " + req.getStatus());
        }
        vendorRequestsTable.getItems().setAll(requests);
        if (requests.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "No Data", "No vendor requests found.");
        }
    }

    private void loadUsers() {
        List<User> users = new ArrayList<>();
        String search = userSearchField.getText().trim();

        String query = "SELECT user_id, CONCAT(first_name, ' ', last_name) AS name, email, vendor_status FROM user";
        List<String> conditions = new ArrayList<>();
        List<Object> params = new ArrayList<>();

        if (!search.isEmpty()) {
            conditions.add("(CONCAT(first_name, ' ', last_name) LIKE ? OR email LIKE ?)");
            params.add("%" + search + "%");
            params.add("%" + search + "%");
        }

        if (!conditions.isEmpty()) {
            query += " WHERE " + String.join(" AND ", conditions);
        }
        query += " ORDER BY user_id";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            for (int i = 0; i < params.size(); i++) {
                stmt.setString(i + 1, params.get(i).toString());
            }
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                String vendorStatus = rs.getString("vendor_status") != null ? rs.getString("vendor_status") : "none";
                users.add(new User(
                        rs.getInt("user_id"),
                        rs.getString("name"),
                        vendorStatus,
                        rs.getString("email")
                ));
            }
        } catch (SQLException e) {
            LOGGER.severe("Error loading users: " + e.getMessage());
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Error", "Error loading users: " + e.getMessage());
        }
        userTable.getItems().setAll(users);
        if (users.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "No Data", "No users found.");
        }
    }

    private void loadTransactions() {
        List<AdminOrder> orders = new ArrayList<>();
        String status = transactionStatusFilter.getSelectionModel().getSelectedItem();
        String search = transactionSearchField.getText().trim();

        String query = "SELECT o.order_id, o.user_id, o.total_amount, o.payment_method, o.status, o.created_at, " +
                "CONCAT(u.first_name, ' ', u.last_name) AS customer_name " +
                "FROM orders o JOIN user u ON o.user_id = u.user_id";
        List<String> conditions = new ArrayList<>();
        List<Object> params = new ArrayList<>();

        if (!status.equals("All")) {
            conditions.add("o.status = ?");
            params.add(status.toLowerCase());
        }
        if (!search.isEmpty()) {
            conditions.add("(CONCAT(u.first_name, ' ', u.last_name) LIKE ? OR o.order_id = ?)");
            params.add("%" + search + "%");
            try {
                params.add(Integer.parseInt(search));
            } catch (NumberFormatException e) {
                params.add(0); // Invalid order_id
            }
        }

        if (!conditions.isEmpty()) {
            query += " WHERE " + String.join(" AND ", conditions);
        }
        query += " ORDER BY o.created_at DESC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            for (int i = 0; i < params.size(); i++) {
                stmt.setObject(i + 1, params.get(i));
            }
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                orders.add(new AdminOrder(
                        rs.getInt("order_id"),
                        rs.getInt("user_id"),
                        rs.getString("customer_name"),
                        rs.getDouble("total_amount"),
                        rs.getString("payment_method"),
                        rs.getString("status"),
                        rs.getTimestamp("created_at").toLocalDateTime()
                ));
            }
        } catch (SQLException e) {
            LOGGER.severe("Error loading transactions: " + e.getMessage());
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Error", "Error loading transactions: " + e.getMessage());
        }
        transactionTable.getItems().setAll(orders);
        if (orders.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "No Data", "No transactions found.");
        }
    }

    private void processProductRequest(ApprovalRequest request, String action) {
        TextArea commentsArea = new TextArea();
        commentsArea.setPromptText("Enter comments (optional)");
        commentsArea.setPrefRowCount(4);
        commentsArea.setPrefColumnCount(30);
        commentsArea.getStyleClass().add("dashboard-text-area");

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(action.equals("approved") ? "Approve Request" : "Reject Request");
        dialog.setHeaderText("Confirm " + action + " for " + request.getRequestType() + " request of " + request.getProductName());
        dialog.getDialogPane().setContent(new VBox(10, new Label("Comments:"), commentsArea));
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dialog.getDialogPane().getStylesheets().add(getClass().getResource("/com/example/maxecommerce/admin/admin.css").toExternalForm());

        dialog.showAndWait().ifPresent(buttonType -> {
            if (buttonType == ButtonType.OK) {
                try (Connection conn = DatabaseConnection.getConnection()) {
                    conn.setAutoCommit(false);
                    try {
                        PreparedStatement approvalStmt = conn.prepareStatement(
                                "UPDATE ProductApproval SET status = ?, admin_comments = ? WHERE request_id = ?");
                        approvalStmt.setString(1, action);
                        approvalStmt.setString(2, commentsArea.getText());
                        approvalStmt.setInt(3, request.getRequestId());
                        approvalStmt.executeUpdate();

                        if (action.equals("approved")) {
                            if (request.getRequestType().equals("delete")) {
                                PreparedStatement deleteStmt = conn.prepareStatement(
                                        "DELETE FROM Product WHERE product_id = ?");
                                deleteStmt.setInt(1, request.getProductId());
                                deleteStmt.executeUpdate();
                            } else {
                                PreparedStatement productStmt = conn.prepareStatement(
                                        "UPDATE Product SET status = 'approved' WHERE product_id = ?");
                                productStmt.setInt(1, request.getProductId());
                                productStmt.executeUpdate();
                            }
                        } else if (action.equals("rejected")) {
                            if (!request.getRequestType().equals("delete")) {
                                PreparedStatement productStmt = conn.prepareStatement(
                                        "UPDATE Product SET status = 'rejected' WHERE product_id = ?");
                                productStmt.setInt(1, request.getProductId());
                                productStmt.executeUpdate();
                            }
                        }

                        PreparedStatement notifyStmt = conn.prepareStatement(
                                "INSERT INTO Notification (user_id, message) VALUES (?, ?)");
                        notifyStmt.setInt(1, request.getUserId());
                        notifyStmt.setString(2, "Your " + request.getRequestType() + " request for product '" +
                                request.getProductName() + "' has been " + action + ". Comments: " +
                                commentsArea.getText());
                        notifyStmt.executeUpdate();

                        conn.commit();
                        showAlert(Alert.AlertType.INFORMATION, "Success", "Product request " + action + " successfully.");
                        loadApprovalRequests();
                        loadStatistics();
                    } catch (SQLException e) {
                        conn.rollback();
                        LOGGER.severe("Error processing product request: " + e.getMessage());
                        e.printStackTrace();
                        showAlert(Alert.AlertType.ERROR, "Error", "Failed to process product request: " + e.getMessage());
                    }
                } catch (SQLException e) {
                    LOGGER.severe("Database connection failed: " + e.getMessage());
                    e.printStackTrace();
                    showAlert(Alert.AlertType.ERROR, "Error", "Database connection failed: " + e.getMessage());
                }
            }
        });
    }

    private void processVendorRequest(VendorRequest request, String action) {
        TextArea commentsArea = new TextArea();
        commentsArea.setPromptText("Enter comments (optional)");
        commentsArea.setPrefRowCount(4);
        commentsArea.setPrefColumnCount(30);
        commentsArea.getStyleClass().add("dashboard-text-area");

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(action.equals("approved") ? "Approve Vendor Request" : "Reject Vendor Request");
        dialog.setHeaderText("Confirm " + action + " for vendor request from " + request.getUserEmail());
        dialog.getDialogPane().setContent(new VBox(10, new Label("Comments:"), commentsArea));
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dialog.getDialogPane().getStylesheets().add(getClass().getResource("/com/example/maxecommerce/admin/admin.css").toExternalForm());

        dialog.showAndWait().ifPresent(buttonType -> {
            if (buttonType == ButtonType.OK) {
                try (Connection conn = DatabaseConnection.getConnection()) {
                    conn.setAutoCommit(false);
                    try {
                        PreparedStatement approvalStmt = conn.prepareStatement(
                                "UPDATE VendorRequest SET status = ? WHERE request_id = ?");
                        approvalStmt.setString(1, action);
                        approvalStmt.setInt(2, request.getRequestId());
                        approvalStmt.executeUpdate();

                        PreparedStatement userStmt = conn.prepareStatement(
                                "UPDATE user SET vendor_status = ? WHERE user_id = ?");
                        userStmt.setString(1, action);
                        userStmt.setInt(2, request.getUserId());
                        userStmt.executeUpdate();

                        PreparedStatement notifyStmt = conn.prepareStatement(
                                "INSERT INTO Notification (user_id, message) VALUES (?, ?)");
                        notifyStmt.setInt(1, request.getUserId());
                        notifyStmt.setString(2, "Your vendor request has been " + action + ". Comments: " +
                                commentsArea.getText());
                        notifyStmt.executeUpdate();

                        conn.commit();
                        showAlert(Alert.AlertType.INFORMATION, "Success", "Vendor request " + action + " successfully.");
                        loadVendorRequests();
                        loadUsers();
                        loadStatistics();
                    } catch (SQLException e) {
                        conn.rollback();
                        LOGGER.severe("Error processing vendor request: " + e.getMessage());
                        e.printStackTrace();
                        showAlert(Alert.AlertType.ERROR, "Error", "Failed to process vendor request: " + e.getMessage());
                    }
                } catch (SQLException e) {
                    LOGGER.severe("Database connection failed: " + e.getMessage());
                    e.printStackTrace();
                    showAlert(Alert.AlertType.ERROR, "Error", "Database connection failed: " + e.getMessage());
                }
            }
        });
    }

    private void showProductDetails(int productId) {
        LOGGER.info("Attempting to show details for productId: " + productId);
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT p.name, p.description, p.price, p.stock, p.discount, p.image_path, p.status, c.name AS category_name, pa.admin_comments " +
                             "FROM Product p " +
                             "LEFT JOIN Category c ON p.category_id = c.category_id " +
                             "LEFT JOIN ProductApproval pa ON p.product_id = pa.product_id " +
                             "WHERE p.product_id = ?")) {
            stmt.setInt(1, productId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                LOGGER.info("Product found for productId: " + productId);

                // Main container
                VBox mainContainer = new VBox(15);
                mainContainer.setPadding(new Insets(20));
                mainContainer.getStyleClass().add("product-details-card");

                // Header
                Label headerLabel = new Label(rs.getString("name") != null ? rs.getString("name") : "Product Details");
                headerLabel.getStyleClass().add("product-details-header");

                // Image
                ImageView imageView = new ImageView();
                String imagePath = rs.getString("image_path");
                if (imagePath != null && !imagePath.isEmpty()) {
                    try {
                        // Try loading as resource
                        String resourcePath = "/com/example/maxecommerce" + imagePath;
                        InputStream imageStream = getClass().getResourceAsStream(resourcePath);
                        if (imageStream != null) {
                            Image image = new Image(imageStream);
                            imageView.setImage(image);
                            LOGGER.info("Loaded image from resource: " + resourcePath);
                        } else {
                            // Try as file path
                            File imageFile = new File(imagePath);
                            if (imageFile.exists()) {
                                Image image = new Image(imageFile.toURI().toString());
                                imageView.setImage(image);
                                LOGGER.info("Loaded image from file: " + imagePath);
                            } else {
                                LOGGER.warning("Image file not found: " + imagePath);
                                imageView.setImage(new Image(Objects.requireNonNull(getClass().getResourceAsStream("/com/example/maxecommerce/images/logo.png"))));
                            }
                        }
                    } catch (Exception e) {
                        LOGGER.warning("Failed to load image: " + imagePath + ", using placeholder. Error: " + e.getMessage());
                        imageView.setImage(new Image(Objects.requireNonNull(getClass().getResourceAsStream("/com/example/maxecommerce/images/logo.png"))));
                    }
                } else {
                    LOGGER.info("No image path provided for productId: " + productId);
                    imageView.setImage(new Image(Objects.requireNonNull(getClass().getResourceAsStream("/com/example/maxecommerce/images/logo.png"))));
                }
                imageView.setFitWidth(150);
                imageView.setFitHeight(150);
                imageView.setPreserveRatio(true);
                imageView.setStyle("-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 5, 0, 1, 1);");

                // Details Grid
                GridPane detailsGrid = new GridPane();
                detailsGrid.setHgap(10);
                detailsGrid.setVgap(8);
                detailsGrid.getStyleClass().add("product-details-grid");

                // Labels
                int row = 0;
                addDetailRow(detailsGrid, row++, "Category:", rs.getString("category_name") != null ? rs.getString("category_name") : "N/A");
                addDetailRow(detailsGrid, row++, "Description:", rs.getString("description") != null ? rs.getString("description") : "N/A");
                addDetailRow(detailsGrid, row++, "Price:", rs.wasNull() ? "₱0.00" : String.format("₱%.2f", rs.getDouble("price")));
                addDetailRow(detailsGrid, row++, "Stock:", rs.wasNull() ? "0" : String.valueOf(rs.getInt("stock")));
                addDetailRow(detailsGrid, row++, "Discount:", rs.wasNull() ? "0.00%" : String.format("%.2f%%", rs.getDouble("discount")));
                addDetailRow(detailsGrid, row++, "Image Path:", imagePath != null ? imagePath : "N/A");
                addDetailRow(detailsGrid, row++, "Status:", rs.getString("status") != null ? rs.getString("status") : "N/A");
                addDetailRow(detailsGrid, row++, "Admin Comments:", rs.getString("admin_comments") != null ? rs.getString("admin_comments") : "N/A");

                // Close Button
                Button closeButton = new Button("Close");
                closeButton.getStyleClass().add("product-details-button");
                closeButton.setOnAction(e -> ((Stage) closeButton.getScene().getWindow()).close());
                HBox buttonBox = new HBox(closeButton);
                buttonBox.setAlignment(Pos.CENTER);
                buttonBox.setPadding(new Insets(10, 0, 0, 0));

                // Assemble layout
                mainContainer.getChildren().addAll(headerLabel, imageView, detailsGrid, buttonBox);

                // Scene and Stage
                Scene scene = new Scene(mainContainer, 400, 550);
                String cssPath = getClass().getResource("/com/example/maxecommerce/admin/admin.css") != null
                        ? getClass().getResource("/com/example/maxecommerce/admin/admin.css").toExternalForm()
                        : "";
                if (!cssPath.isEmpty()) {
                    scene.getStylesheets().add(cssPath);
                    LOGGER.info("Applied CSS: " + cssPath);
                } else {
                    LOGGER.warning("CSS file not found at /com/example/maxecommerce/admin/admin.css");
                }

                Stage stage = new Stage();
                stage.initModality(Modality.APPLICATION_MODAL);
                stage.setTitle("Product Details - ID: " + productId);
                stage.setScene(scene);
                stage.setResizable(false);
                stage.centerOnScreen();
                Platform.runLater(stage::show);
                LOGGER.info("Product details window displayed for productId: " + productId);
            } else {
                LOGGER.warning("No product found for productId: " + productId);
                showAlert(Alert.AlertType.ERROR, "Error", "Product not found for ID: " + productId);
            }
        } catch (SQLException e) {
            LOGGER.severe("Error loading product details for productId " + productId + ": " + e.getMessage());
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Error", "Error loading product details: " + e.getMessage());
        }
    }

    private void addDetailRow(GridPane grid, int row, String labelText, String value) {
        Label label = new Label(labelText);
        label.getStyleClass().add("product-details-label");
        Label valueLabel = new Label(value);
        valueLabel.getStyleClass().add("product-details-value");
        valueLabel.setWrapText(true);
        valueLabel.setMaxWidth(250);
        grid.add(label, 0, row);
        grid.add(valueLabel, 1, row);
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
        try {
            SessionManager.getInstance().logout();
            loadScene("/com/example/maxecommerce/auth/LoginView.fxml", "E-Commerce Platform - Login");
            showAlert(Alert.AlertType.INFORMATION, "Success", "Logged out successfully!");
        } catch (Exception e) {
            LOGGER.severe("Error logging out: " + e.getMessage());
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Error", "Error logging out: " + e.getMessage());
        }
    }

    private void loadScene(String fxmlPath, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();
            Stage stage = (Stage) rootPane.getScene().getWindow();
            stage.setScene(new Scene(root, 1200, 800));
            stage.setTitle(title);
            stage.setResizable(false);
            stage.centerOnScreen();
            stage.show();
        } catch (Exception e) {
            LOGGER.severe("Error navigating: " + e.getMessage());
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Error", "Error navigating: " + e.getMessage());
        }
    }
}