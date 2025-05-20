package com.example.maxecommerce.controller;

import com.example.maxecommerce.util.DatabaseConnection;
import com.example.maxecommerce.util.PasswordUtil;
import com.example.maxecommerce.util.Toast;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.control.PasswordField;
import javafx.stage.Stage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class LoginController {

    @FXML
    private TextField emailField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private Button loginButton;
    @FXML
    private Button registerButton;

    @FXML
    private void handleLogin() {
        String email = emailField.getText().trim();
        String password = passwordField.getText();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.show("Please fill in all fields.", Toast.ToastType.ERROR, loginButton.getScene());
            return;
        }

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT user_id, password, role FROM user WHERE email = ?")) {

            stmt.setString(1, email);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                String hashedPassword = rs.getString("password");
                if (PasswordUtil.validatePassword(password, hashedPassword)) {
                    int userId = rs.getInt("user_id");
                    String role = rs.getString("role");

                    FXMLLoader loader;
                    Parent root;
                    Stage stage = (Stage) loginButton.getScene().getWindow();

                    switch (role) {
                        case "vendor":
                            loader = new FXMLLoader(getClass().getResource("/com/example/maxecommerce/vendor/VendorView.fxml"));
                            root = loader.load();
                            stage.setScene(new Scene(root));
                            stage.setTitle("Vendor Dashboard");
                            break;

                        case "admin":
                            loader = new FXMLLoader(getClass().getResource("/com/example/maxecommerce/admin/AdminDashboard.fxml"));
                            root = loader.load();
                            stage.setScene(new Scene(root));
                            stage.setTitle("Admin Dashboard");
                            break;

                        case "customer":
                        default:
                            loader = new FXMLLoader(getClass().getResource("/com/example/maxecommerce/home/HomeView.fxml"));
                            root = loader.load();
                            stage.setScene(new Scene(root));
                            stage.setTitle("Customer Dashboard");
                            break;
                    }

                    Toast.show("Welcome back, " + email + "!", Toast.ToastType.SUCCESS, stage.getScene());

                } else {
                    Toast.show("Invalid email or password.", Toast.ToastType.ERROR, loginButton.getScene());
                }
            } else {
                Toast.show("Invalid email or password.", Toast.ToastType.ERROR, loginButton.getScene());
            }
        } catch (Exception e) {
            Toast.show("Login failed: " + e.getMessage(), Toast.ToastType.ERROR, loginButton.getScene());
            e.printStackTrace();
        }
    }


    @FXML
    private void handleRegister(ActionEvent event) {
        loadScene("/com/example/maxecommerce/auth/RegisterView.fxml", "Register");
    }

    private void loadScene(String fxmlPath, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath)); // Use the correct path
            Parent root = loader.load();

            Stage stage = (Stage) loginButton.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle(title);
        } catch (Exception e) {
            Toast.show("Error loading screen: " + e.getMessage(), Toast.ToastType.ERROR, loginButton.getScene());
            e.printStackTrace();
        }
    }

}
