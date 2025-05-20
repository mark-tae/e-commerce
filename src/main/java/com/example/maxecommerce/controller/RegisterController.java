package com.example.maxecommerce.controller;

import com.example.maxecommerce.util.DatabaseConnection;
import com.example.maxecommerce.util.PasswordUtil;
import com.example.maxecommerce.util.Toast;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.scene.control.PasswordField;
import javafx.stage.Stage;

import java.sql.Connection;
import java.sql.PreparedStatement;
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
    private Button registerButton;
    @FXML
    private Button backButton;

    @FXML
    private ComboBox<String> roleComboBox;

    @FXML
    private void handleRegister() {
        String email = emailField.getText().trim();
        String password = passwordField.getText();
        String firstName = firstNameField.getText().trim();
        String lastName = lastNameField.getText().trim();
        String address = addressField.getText().trim();
        String phone = phoneField.getText().trim();
        String role = roleComboBox.getValue();

        if (email.isEmpty() || password.isEmpty() || firstName.isEmpty() || lastName.isEmpty() || role == null) {
            Toast.show("Please fill in all required fields.", Toast.ToastType.ERROR, registerButton.getScene());
            return;
        }

        String hashedPassword = PasswordUtil.hashPassword(password);

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "INSERT INTO user (email, password, first_name, last_name, address, phone, role) VALUES (?, ?, ?, ?, ?, ?, ?)")) {
            stmt.setString(1, email);
            stmt.setString(2, hashedPassword);
            stmt.setString(3, firstName);
            stmt.setString(4, lastName);
            stmt.setString(5, address.isEmpty() ? null : address);
            stmt.setString(6, phone.isEmpty() ? null : phone);
            stmt.setString(7, role); // New
            stmt.executeUpdate();

            Toast.show("Registration successful! Please login.", Toast.ToastType.SUCCESS, registerButton.getScene());
            loadScene("/com/example/maxecommerce/auth/LoginView.fxml", "Login");
        } catch (SQLException e) {
            if (e.getErrorCode() == 1062) {
                Toast.show("Email already exists.", Toast.ToastType.ERROR, registerButton.getScene());
            } else {
                Toast.show("Database error: " + e.getMessage(), Toast.ToastType.ERROR, registerButton.getScene());
                e.printStackTrace();
            }
        }
    }


    @FXML
    private void handleBack() {
        loadScene("/com/example/maxecommerce/auth/LoginView.fxml", "Login");
    }

    private void loadScene(String fxmlPath, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();
            Stage stage = (Stage) registerButton.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle(title);
        } catch (Exception e) {
            Toast.show("Error loading screen: " + e.getMessage(), Toast.ToastType.ERROR, registerButton.getScene());
            e.printStackTrace();
        }
    }
}