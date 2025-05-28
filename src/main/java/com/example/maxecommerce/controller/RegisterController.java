package com.example.maxecommerce.controller;

import com.example.maxecommerce.util.DatabaseConnection;
import com.example.maxecommerce.util.PasswordUtil;
import com.example.maxecommerce.util.Toast;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class RegisterController {

    @FXML
    private TextField emailField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private TextField firstNameField;
    @FXML
    private TextField lastNameField;
    @FXML
    private TextField addressField;
    @FXML
    private TextField phoneField;
    @FXML
    private ComboBox<String> roleComboBox;
    @FXML
    private Button registerButton;
    @FXML
    private Button backButton;

    @FXML
    private void handleRegister() {
        String email = emailField.getText().trim();
        String password = passwordField.getText();
        String firstName = firstNameField.getText().trim();
        String lastName = lastNameField.getText().trim();
        String address = addressField.getText().trim();
        String phone = phoneField.getText().trim();
        String selectedRole = roleComboBox.getSelectionModel().getSelectedItem();

        if (email.isEmpty() || password.isEmpty() || firstName.isEmpty() || lastName.isEmpty() ||
                address.isEmpty() || phone.isEmpty() || selectedRole == null) {
            Toast.show("All fields are required.", Toast.ToastType.ERROR, registerButton.getScene());
            return;
        }

        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                // Check if email exists
                PreparedStatement checkStmt = conn.prepareStatement("SELECT user_id FROM user WHERE email = ?");
                checkStmt.setString(1, email);
                ResultSet rs = checkStmt.executeQuery();
                if (rs.next()) {
                    Toast.show("Email already registered.", Toast.ToastType.ERROR, registerButton.getScene());
                    return;
                }

                // Insert user
                String hashedPassword = PasswordUtil.hashPassword(password);
                PreparedStatement userStmt = conn.prepareStatement(
                        "INSERT INTO user (email, password, first_name, last_name, address, phone, status) VALUES (?, ?, ?, ?, ?, ?, 'approved')",
                        PreparedStatement.RETURN_GENERATED_KEYS);
                userStmt.setString(1, email);
                userStmt.setString(2, hashedPassword);
                userStmt.setString(3, firstName);
                userStmt.setString(4, lastName);
                userStmt.setString(5, address);
                userStmt.setString(6, phone);
                userStmt.executeUpdate();

                ResultSet keys = userStmt.getGeneratedKeys();
                keys.next();
                int userId = keys.getInt(1);

                // Insert role
                if (selectedRole.equals("vendor")) {
                    // Insert vendor request instead of direct role
                    PreparedStatement requestStmt = conn.prepareStatement(
                            "INSERT INTO VendorRequest (user_id, status) VALUES (?, 'pending')");
                    requestStmt.setInt(1, userId);
                    requestStmt.executeUpdate();
                    // Assign customer role by default
                    PreparedStatement roleStmt = conn.prepareStatement(
                            "INSERT INTO user_roles (user_id, role) VALUES (?, 'customer')");
                    roleStmt.setInt(1, userId);
                    roleStmt.executeUpdate();
                } else {
                    PreparedStatement roleStmt = conn.prepareStatement(
                            "INSERT INTO user_roles (user_id, role) VALUES (?, ?)");
                    roleStmt.setInt(1, userId);
                    roleStmt.setString(2, selectedRole);
                    roleStmt.executeUpdate();
                }

                conn.commit();
                Toast.show("Registration successful! Awaiting approval.", Toast.ToastType.SUCCESS, registerButton.getScene());
                handleBack();
            } catch (SQLException e) {
                conn.rollback();
                Toast.show("Registration failed: " + e.getMessage(), Toast.ToastType.ERROR, registerButton.getScene());
                e.printStackTrace();
            }
        } catch (SQLException e) {
            Toast.show("Database error: " + e.getMessage(), Toast.ToastType.ERROR, registerButton.getScene());
            e.printStackTrace();
        }
    }

    @FXML
    private void handleBack() {
        loadScene("/com/example/maxecommerce/auth/LoginView.fxml", "UA Platform - Login");
    }

    private void loadScene(String fxmlPath, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();
            Stage stage = (Stage) backButton.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setResizable(true);
            stage.setTitle(title);
        } catch (Exception e) {
            Toast.show("Error loading screen: " + e.getMessage(), Toast.ToastType.ERROR, backButton.getScene());
            e.printStackTrace();
        }
    }
}