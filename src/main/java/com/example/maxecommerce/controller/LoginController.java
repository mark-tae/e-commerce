package com.example.maxecommerce.controller;

import com.example.maxecommerce.util.DatabaseConnection;
import com.example.maxecommerce.util.PasswordUtil;
import com.example.maxecommerce.util.SessionManager;
import javafx.application.HostServices;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Logger;

public class LoginController {

    private static final Logger LOGGER = Logger.getLogger(LoginController.class.getName());

    @FXML
    private TextField emailField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private Button loginButton;
    @FXML
    private Button registerButton;
    @FXML
    private Button guestButton;

    private HostServices hostServices;


    @FXML
    private void handleLogin() {
        String email = emailField.getText().trim();
        String password = passwordField.getText();

        if (email.isEmpty() || password.isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Error", "Please fill in all fields.");
            return;
        }

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT user_id, password, status, vendor_status FROM user WHERE email = ?")) {

            stmt.setString(1, email);
            ResultSet rs = stmt.executeQuery();

            if (!rs.next()) {
                showAlert(Alert.AlertType.ERROR, "Error", "Invalid email or password.");
                return;
            }

            int userId = rs.getInt("user_id");
            String hashedPassword = rs.getString("password");
            String status = rs.getString("status");
            String vendorStatus = rs.getString("vendor_status");

            if (!"approved".equals(status)) {
                showAlert(Alert.AlertType.ERROR, "Error", "Account is " + status + ".");
                return;
            }

            if (!PasswordUtil.validatePassword(password, hashedPassword)) {
                showAlert(Alert.AlertType.ERROR, "Error", "Invalid email or password.");
                return;
            }

            LOGGER.info("User ID: " + userId + ", Email: " + email + ", Vendor Status: " + vendorStatus);

            // Set session
            SessionManager.getInstance().login(userId);

            // Navigate based on user type
            String fxmlPath;
            String title;
            if (userId == 1) { // Assuming user_id = 1 is admin for simplicity
                fxmlPath = "/com/example/maxecommerce/admin/AdminDashboard.fxml";
                title = "Admin Dashboard";
            } else if ("approved".equals(vendorStatus)) {
                fxmlPath = "/com/example/maxecommerce/vendor/VendorView.fxml";
                title = "Vendor Dashboard";
            } else {
                fxmlPath = "/com/example/maxecommerce/home/HomeView.fxml";
                title = "Home";
            }

            LOGGER.info("Navigating to: " + fxmlPath);

            // Load FXML
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root;
            try {
                root = loader.load();
                // Inject HostServices for HomeController
                if (fxmlPath.equals("/com/example/maxecommerce/home/HomeView.fxml")) {
                    HomeController controller = loader.getController();
                    if (hostServices != null) {
                        controller.setHostServices(hostServices);
                        LOGGER.info("HostServices injected into HomeController");
                    } else {
                        LOGGER.warning("HostServices is null in LoginController");
                    }
                }
            } catch (Exception e) {
                LOGGER.severe("Failed to load FXML: " + fxmlPath + ", Error: " + e.getMessage());
                e.printStackTrace();
                showAlert(Alert.AlertType.ERROR, "Error", "Failed to load dashboard: " + e.getMessage());
                SessionManager.getInstance().logout();
                return;
            }

            Stage stage = (Stage) loginButton.getScene().getWindow();
            stage.setScene(new Scene(root));

            stage.setTitle(title);
            showAlert(Alert.AlertType.INFORMATION, "Success", "Welcome back, " + email + "!");

        } catch (SQLException e) {
            LOGGER.severe("Database error: " + e.getMessage());
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Error", "Database error: " + e.getMessage());
        }
    }

    @FXML
    private void handleGuestLogin() {
        try {
            SessionManager.getInstance().loginAsGuest();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/maxecommerce/home/HomeView.fxml"));
            Parent root = loader.load();
            HomeController controller = loader.getController();
            if (hostServices != null) {
                controller.setHostServices(hostServices);
                LOGGER.info("HostServices injected into HomeController for guest");
            } else {
                LOGGER.warning("HostServices is null for guest login");
            }
            Stage stage = (Stage) guestButton.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Home");
            showAlert(Alert.AlertType.INFORMATION, "Success", "Logged in as guest.");
        } catch (Exception e) {
            LOGGER.severe("Error loading guest view: " + e.getMessage());
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Error", "Error loading guest view: " + e.getMessage());
        }
    }

    @FXML
    private void handleRegister() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/maxecommerce/auth/RegisterView.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) loginButton.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setResizable(true);
            stage.setTitle("Register");
            showAlert(Alert.AlertType.INFORMATION, "Success", "Navigated to registration.");
        } catch (Exception e) {
            LOGGER.severe("Error loading register view: " + e.getMessage());
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Error", "Error loading register view: " + e.getMessage());
        }
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}