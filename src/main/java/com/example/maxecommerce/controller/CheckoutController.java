package com.example.maxecommerce.controller;

import com.example.maxecommerce.util.DatabaseConnection;
import com.example.maxecommerce.util.SessionManager;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.stripe.Stripe;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.imageio.ImageIO;

public class CheckoutController {
    private static final String STRIPE_API_KEY = System.getenv("STRIPE_API_KEY"); // Your Stripe test secret key
    private static final double ORDER_AMOUNT = 1000.00; // Fixed amount in PHP
    private static final String PAYMENT_BASE_URL = "http://localhost:8080/gcash-payment";

    private Stage stage;
    private TextField locationField;
    private Button cardButton;
    private Button gcashButton;
    private Label statusLabel;

    public void startCheckout(Stage stage, int userId) {
        this.stage = stage;

        // Initialize UI
        VBox root = new VBox(20);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(20));

        Label titleLabel = new Label("Checkout");
        titleLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold;");

        Label locationLabel = new Label("Delivery Location:");
        locationField = new TextField();
        locationField.setPromptText("Enter delivery address");
        locationField.setMaxWidth(300);

        cardButton = new Button("Pay with Card (Stripe)");
        gcashButton = new Button("Pay with GCash (QR Code)");
        statusLabel = new Label();

        cardButton.setOnAction(e -> handleCardPayment(userId));
        gcashButton.setOnAction(e -> handleGCashPayment(userId));

        root.getChildren().addAll(titleLabel, locationLabel, locationField, cardButton, gcashButton, statusLabel);

        Scene scene = new Scene(root, 500, 800);
        stage.setScene(scene);
        stage.setTitle("Checkout");
        stage.setResizable(false);
        stage.centerOnScreen();
        stage.show();
    }

    private void handleCardPayment(int userId) {
        String location = locationField.getText().trim();
        if (location.isEmpty()) {
            statusLabel.setText("Please enter a delivery location.");
            return;
        }

        try {
            Stripe.apiKey = STRIPE_API_KEY;
            SessionCreateParams params = SessionCreateParams.builder()
                    .addPaymentMethodType(SessionCreateParams.PaymentMethodType.CARD)
                    .setMode(SessionCreateParams.Mode.PAYMENT)
                    .addLineItem(
                            SessionCreateParams.LineItem.builder()
                                    .setPriceData(
                                            SessionCreateParams.LineItem.PriceData.builder()
                                                    .setCurrency("php")
                                                    .setUnitAmount((long) (ORDER_AMOUNT * 100)) // Amount in cents
                                                    .setProductData(
                                                            SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                                                    .setName("Order")
                                                                    .build()
                                                    )
                                                    .build()
                                    )
                                    .setQuantity(1L)
                                    .build()
                    )
                    .setSuccessUrl("http://localhost:8080/success?order_id={CHECKOUT_SESSION_ID}")
                    .setCancelUrl("http://localhost:8080/cancel")
                    .build();

            Session session = Session.create(params);
            int orderId = saveOrder(userId, location, "card", "pending", null);
            openBrowser(session.getUrl());
            statusLabel.setText("Redirecting to Stripe Checkout...");
        } catch (Exception e) {
            statusLabel.setText("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void handleGCashPayment(int userId) {
        String location = locationField.getText().trim();
        if (location.isEmpty()) {
            statusLabel.setText("Please enter a delivery location.");
            return;
        }

        try {
            int orderId = saveOrder(userId, location, "gcash", "pending", PAYMENT_BASE_URL + "?order_id=");
            String paymentUrl = PAYMENT_BASE_URL + "?order_id=" + orderId;
            displayQRCode(paymentUrl);
            statusLabel.setText("Scan the QR code with GCash to pay.");
        } catch (Exception e) {
            statusLabel.setText("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private int saveOrder(int userId, String location, String paymentMethod, String status, String paymentLinkBase) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "INSERT INTO orders (user_id, delivery_location, total_amount, payment_method, status, payment_link) VALUES (?, ?, ?, ?, ?, ?)",
                     PreparedStatement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, userId);
            stmt.setString(2, location);
            stmt.setDouble(3, ORDER_AMOUNT);
            stmt.setString(4, paymentMethod);
            stmt.setString(5, status);
            String paymentLink = paymentLinkBase != null ? paymentLinkBase + getNextOrderId(conn) : null;
            stmt.setString(6, paymentLink);
            stmt.executeUpdate();
            ResultSet rs = stmt.getGeneratedKeys();
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            statusLabel.setText("Database error: " + e.getMessage());
            e.printStackTrace();
        }
        return -1;
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

    private void displayQRCode(String url) {
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
            qrBox.getChildren().addAll(new Label("Scan with GCash"), qrView, statusLabel);

            Scene qrScene = new Scene(qrBox, 500, 800);
            stage.setScene(qrScene);
            stage.setTitle("GCash Payment");
            stage.setResizable(false);
            stage.centerOnScreen();
        } catch (Exception e) {
            statusLabel.setText("QR Code error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void openBrowser(String url) {
        try {
            java.awt.Desktop.getDesktop().browse(new java.net.URI(url));
        } catch (Exception e) {
            statusLabel.setText("Browser error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}