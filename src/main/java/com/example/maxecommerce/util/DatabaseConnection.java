package com.example.maxecommerce.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnection {
    private static final String URL = "jdbc:mysql://localhost:3306/ecommerce_db?useSSL=false";
    private static final String USER = "root"; // Default XAMPP MySQL user
    private static final String PASSWORD = ""; // Default XAMPP MySQL password (empty)
    private static final String STRIPE_API_KEY = "";
    public static Connection getConnection() throws SQLException {
        try {
            Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
            System.out.println("Database connection successful!");
            return conn;
        } catch (SQLException e) {
            System.err.println("Failed to connect to database: " + e.getMessage());
            throw e;
        }
    }
}
