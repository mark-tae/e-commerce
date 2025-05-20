package com.example.maxecommerce.controller;


import com.example.maxecommerce.model.ApprovalRequest;
import com.example.maxecommerce.util.DatabaseConnection;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class AdminDashboardController {

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
    private TableView<ApprovalRequest> approvalTable;
    @FXML
    private TableColumn<ApprovalRequest, String> requestIdColumn;
    @FXML
    private TableColumn<ApprovalRequest, String> productIdColumn;
    @FXML
    private TableColumn<ApprovalRequest, String> productNameColumn;
    @FXML
    private TableColumn<ApprovalRequest, String> vendorIdColumn;
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
    private void initialize() {
        setupFilters();
        setupApprovalTable();
        loadApprovalRequests();
    }

    private void setupFilters() {
        statusFilter.setItems(FXCollections.observableArrayList("All", "Pending", "Approved", "Rejected"));
        statusFilter.getSelectionModel().select("All");
        statusFilter.setOnAction(event -> loadApprovalRequests());

        typeFilter.setItems(FXCollections.observableArrayList("All", "Create", "Update", "Delete"));
        typeFilter.getSelectionModel().select("Create");
        typeFilter.setOnAction(event -> loadApprovalRequests());
    }

    private void setupApprovalTable() {
        requestIdColumn.setCellValueFactory(cellData -> new SimpleStringProperty(String.valueOf(cellData.getValue().getRequestId())));
        productIdColumn.setCellValueFactory(cellData -> new SimpleStringProperty(String.valueOf(cellData.getValue().getProductId())));
        productNameColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getProductName()));
        vendorIdColumn.setCellValueFactory(cellData -> new SimpleStringProperty(String.valueOf(cellData.getValue().getVendorId())));
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
                    if (type.equalsIgnoreCase("create")) {
                        setStyle("-fx-text-fill: #1976d2; -fx-font-weight: bold;");
                    } else {
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
        adminCommentsColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getAdminComments() != null ? cellData.getValue().getAdminComments() : ""));
        createdAtColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))));

        actionsColumn.setCellFactory(param -> new TableCell<>() {
            private final Button viewButton = new Button("View");
            private final Button approveButton = new Button("Approve");
            private final Button rejectButton = new Button("Reject");
            private final HBox pane = new HBox(5, viewButton, approveButton, rejectButton);

            {
                viewButton.getStyleClass().add("dashboard-btn-info");
                approveButton.getStyleClass().add("dashboard-btn-primary");
                rejectButton.getStyleClass().add("dashboard-btn-danger");
                viewButton.setOnAction(event -> {
                    ApprovalRequest request = getTableView().getItems().get(getIndex());
                    showProductDetails(request.getProductId());
                });
                approveButton.setOnAction(event -> {
                    ApprovalRequest request = getTableView().getItems().get(getIndex());
                    if (request.getStatus().equals("pending")) {
                        processRequest(request, "approved");
                    }
                });
                rejectButton.setOnAction(event -> {
                    ApprovalRequest request = getTableView().getItems().get(getIndex());
                    if (request.getStatus().equals("pending")) {
                        processRequest(request, "rejected");
                    }
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty || !getTableView().getItems().get(getIndex()).getStatus().equals("pending") ? null : pane);
            }
        });
    }

    @FXML
    private void loadApprovalRequests() {
        List<ApprovalRequest> requests = new ArrayList<>();
        String status = statusFilter.getSelectionModel().getSelectedItem();
        String type = typeFilter.getSelectionModel().getSelectedItem();

        String query = "SELECT pa.request_id, pa.product_id, pa.vendor_id, pa.request_type, pa.status, pa.admin_comments, pa.created_at, p.name AS product_name, u.first_name, u.last_name " +
                "FROM ProductApproval pa " +
                "JOIN Product p ON pa.product_id = p.product_id " +
                "JOIN User u ON pa.vendor_id = u.user_id "; //+
                //"WHERE u.role = 'vendor'";
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

        if (!conditions.isEmpty()) {
            query += " AND " + String.join(" AND ", conditions);
        }
        query += " ORDER BY CASE WHEN pa.request_type = 'create' THEN 1 ELSE 2 END, pa.created_at DESC";

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
                        rs.getInt("vendor_id"),
                        rs.getString("first_name") + " " + rs.getString("last_name"),
                        rs.getString("request_type"),
                        rs.getString("status"),
                        rs.getString("admin_comments"),
                        rs.getTimestamp("created_at").toLocalDateTime()
                ));
            }
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Error", "Error loading approval requests: " + e.getMessage());
            e.printStackTrace();
        }
        approvalTable.getItems().setAll(requests);
    }

    private void processRequest(ApprovalRequest request, String action) {
        TextArea commentsArea = new TextArea();
        commentsArea.setPromptText("Enter comments (optional)");
        commentsArea.setPrefRowCount(4);
        commentsArea.setPrefColumnCount(30);

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(action.equals("approved") ? "Approve Product" : "Reject Product");
        dialog.setHeaderText("Confirm " + action + " for " + request.getRequestType() + " request of " + request.getProductName());
        dialog.getDialogPane().setContent(new VBox(10, new Label("Comments:"), commentsArea));
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.showAndWait().ifPresent(buttonType -> {
            if (buttonType == ButtonType.OK) {
                try (Connection conn = DatabaseConnection.getConnection()) {
                    conn.setAutoCommit(false);
                    try {
                        // Update ProductApproval
                        PreparedStatement approvalStmt = conn.prepareStatement(
                                "UPDATE ProductApproval SET status = ?, admin_comments = ? WHERE request_id = ?");
                        approvalStmt.setString(1, action);
                        approvalStmt.setString(2, commentsArea.getText());
                        approvalStmt.setInt(3, request.getRequestId());
                        approvalStmt.executeUpdate();

                        // Update Product
                        if (action.equals("approved")) {
                            if (request.getRequestType().equals("delete")) {
                                PreparedStatement deleteStmt = conn.prepareStatement(
                                        "DELETE FROM productapproval WHERE product_id = ?");
                                deleteStmt.setInt(1, request.getProductId());
                                deleteStmt.executeUpdate();
                            } else {
                                PreparedStatement productStmt = conn.prepareStatement(
                                        "UPDATE productapproval SET status = 'approved' WHERE product_id = ?");
                                productStmt.setInt(1, request.getProductId());
                                productStmt.executeUpdate();

                                productStmt = conn.prepareStatement("UPDATE product SET status = 'approved' WHERE product_id = ?");
                                productStmt.setInt(1, request.getProductId());
                                productStmt.executeUpdate();
                            }
                        } else {
                            if (!request.getRequestType().equals("delete")) {
                                PreparedStatement productStmt = conn.prepareStatement(
                                        "UPDATE productapproval SET status = 'rejected' WHERE product_id = ?");
                                productStmt.setInt(1, request.getProductId());
                                productStmt.executeUpdate();

                                productStmt = conn.prepareStatement("UPDATE product SET status = 'rejected' WHERE product_id = ?");
                                productStmt.setInt(1, request.getProductId());
                                productStmt.executeUpdate();
                            }
                        }

                        // Notify vendor
                        PreparedStatement notifyStmt = conn.prepareStatement(
                                "INSERT INTO Notification (user_id, message) VALUES (?, ?)");
                        notifyStmt.setInt(1, getVendorIdByProduct(request.getProductId()));
                        notifyStmt.setString(2, "Your " + request.getRequestType() + " request for product '" + request.getProductName() +
                                "' has been " + action + ". Comments: " + commentsArea.getText());
                        notifyStmt.executeUpdate();

                        conn.commit();
                        showAlert(Alert.AlertType.INFORMATION, "Success", "Product " + action + " successfully.");
                        loadApprovalRequests();
                    } catch (SQLException e) {
                        conn.rollback();
                        showAlert(Alert.AlertType.ERROR, "Error", "Failed to process request: " + e.getMessage());
                        e.printStackTrace();
                    }
                } catch (SQLException e) {
                    showAlert(Alert.AlertType.ERROR, "Error", "Database connection failed: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        });
    }

    private void showProductDetails(int productId) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT p.name, p.description, p.price, p.stock, p.discount, p.image_path, p.status, c.name AS category_name, pa.admin_comments " +
                             "FROM Product p " +
                             "JOIN Category c ON p.category_id = c.category_id " +
                             "LEFT JOIN ProductApproval pa ON p.product_id = pa.product_id " +
                             "WHERE p.product_id = ?")) {
            stmt.setInt(1, productId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                VBox details = new VBox(10);
                details.setPadding(new Insets(20));
                details.getStyleClass().add("dashboard-card");

                Label nameLabel = new Label("Name: " + rs.getString("name"));
                nameLabel.getStyleClass().add("dashboard-label");
                Label categoryLabel = new Label("Category: " + rs.getString("category_name"));
                categoryLabel.getStyleClass().add("dashboard-label");
                Label descriptionLabel = new Label("Description: " + (rs.getString("description") != null ? rs.getString("description") : "N/A"));
                descriptionLabel.getStyleClass().add("dashboard-label");
                Label priceLabel = new Label("Price: ₱" + String.format("%.2f", rs.getDouble("price")));
                priceLabel.getStyleClass().add("dashboard-label");
                Label stockLabel = new Label("Stock: " + rs.getInt("stock"));
                stockLabel.getStyleClass().add("dashboard-label");
                Label discountLabel = new Label("Discount: " + String.format("%.2f%%", rs.getDouble("discount")));
                discountLabel.getStyleClass().add("dashboard-label");
                Label imagePathLabel = new Label("Image Path: " + (rs.getString("image_path") != null ? rs.getString("image_path") : "N/A"));
                imagePathLabel.getStyleClass().add("dashboard-label");
                Label statusLabel = new Label("Status: " + rs.getString("status"));
                statusLabel.getStyleClass().add("dashboard-label");
                Label commentsLabel = new Label("Admin Comments: " + (rs.getString("admin_comments") != null ? rs.getString("admin_comments") : "N/A"));
                commentsLabel.getStyleClass().add("dashboard-label");

                details.getChildren().addAll(
                        nameLabel,
                        categoryLabel,
                        descriptionLabel,
                        priceLabel,
                        stockLabel,
                        discountLabel,
                        imagePathLabel,
                        statusLabel,
                        commentsLabel
                );

                Scene scene = new Scene(details, 400, 450);
                scene.getStylesheets().add(getClass().getResource("/com/example/maxecommerce/admin/admin.css").toExternalForm());
                Stage stage = new Stage();
                stage.initModality(Modality.APPLICATION_MODAL);
                stage.setTitle("Product Details");
                stage.setScene(scene);
                stage.show();
            } else {
                showAlert(Alert.AlertType.ERROR, "Error", "Product not found.");
            }
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Error", "Error loading product details: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private int getVendorIdByProduct(int productId) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT vendor_id FROM Product WHERE product_id = ?")) {
            stmt.setInt(1, productId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("vendor_id");
            }
            throw new SQLException("Vendor not found for product ID: " + productId);
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
