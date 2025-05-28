package com.example.maxecommerce.util;

import com.example.maxecommerce.util.DatabaseConnection;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class PaymentServer {
    private static final int PORT = 8080;
    private static HttpServer server;

    public static void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress(PORT), 0);
        server.createContext("/gcash-payment", exchange -> {
            String query = exchange.getRequestURI().getQuery();
            int orderId = parseOrderId(query);

            if (exchange.getRequestMethod().equalsIgnoreCase("GET")) {
                // Serve confirmation page
                String html = """
                    <html>
                    <head><title>GCash Payment</title></head>
                    <body>
                        <h1>GCash Payment Confirmation</h1>
                        <p>Order ID: %d</p>
                        <p>Amount: ₱1000.00</p>
                        <form method="POST" action="/gcash-payment?order_id=%d">
                            <button type="submit">Confirm Payment</button>
                        </form>
                    </body>
                    </html>
                    """.formatted(orderId, orderId);
                sendResponse(exchange, 200, html);
            } else if (exchange.getRequestMethod().equalsIgnoreCase("POST")) {
                // Handle payment confirmation
                updateOrderStatus(orderId, "completed");
                String html = """
                    <html>
                    <head><title>Payment Success</title></head>
                    <body>
                        <h1>Payment Successful</h1>
                        <p>Order ID: %d</p>
                        <p>Thank you for your payment!</p>
                    </body>
                    </html>
                    """.formatted(orderId);
                sendResponse(exchange, 200, html);
            }
        });
        server.setExecutor(null);
        server.start();
        System.out.println("Payment server started on port " + PORT);
    }

    public static void stopServer() {
        if (server != null) {
            server.stop(0);
            System.out.println("Payment server stopped");
        }
    }

    private static int parseOrderId(String query) {
        if (query != null && query.startsWith("order_id=")) {
            try {
                return Integer.parseInt(query.split("=")[1]);
            } catch (NumberFormatException e) {
                return -1;
            }
        }
        return -1;
    }

    private static void sendResponse(HttpExchange exchange, int statusCode, String response) throws IOException {
        exchange.sendResponseHeaders(statusCode, response.getBytes().length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(response.getBytes());
        }
    }

    private static void updateOrderStatus(int orderId, String status) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement("UPDATE orders SET status = ? WHERE order_id = ?")) {
            stmt.setString(1, status);
            stmt.setInt(2, orderId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}