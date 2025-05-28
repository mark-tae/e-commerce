package com.example.maxecommerce.controller;

import com.example.maxecommerce.model.CartItem;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.List;

public class CartController {

    @FXML
    private VBox cartForm;
    @FXML
    private Label cartLabel;
    @FXML
    private TableView<CartItem> cartTable;
    @FXML
    private TableColumn<CartItem, String> nameColumn;
    @FXML
    private TableColumn<CartItem, String> quantityColumn;
    @FXML
    private TableColumn<CartItem, String> priceColumn;
    @FXML
    private TableColumn<CartItem, Void> actionsColumn;
    @FXML
    private Button checkoutButton;

    private ProductController productController;
    private HomeController homeController;
    private List<CartItem> cartItems;

    public void initializeCart(ProductController productController, List<CartItem> cartItems) {
        this.productController = productController;
        this.cartItems = cartItems;
        setupTable();
        loadCartItems();
    }

    public void initializeCart2(HomeController homeController, List<CartItem> cartItems) {
        this.homeController = homeController;
        this.cartItems = cartItems;
        setupTable();
        loadCartItems();
    }

    private void setupTable() {
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("productName"));
        quantityColumn.setCellValueFactory(cellData -> new SimpleStringProperty(String.valueOf(cellData.getValue().getQuantity())));
        priceColumn.setCellValueFactory(cellData -> new SimpleStringProperty(String.format("₱%.2f", cellData.getValue().getPrice())));

        actionsColumn.setCellFactory(param -> new TableCell<>() {
            private final Button removeButton = new Button("Remove");
            private final HBox pane = new HBox(removeButton);

            {
                removeButton.getStyleClass().add("product-btn-secondary");
                removeButton.setOnAction(event -> {
                    CartItem item = getTableView().getItems().get(getIndex());
                    productController.removeFromCart(item.getCartItemId());
                    getTableView().getItems().remove(item);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : pane);
            }
        });

        cartTable.getItems().setAll(cartItems);
    }

    private void loadCartItems() {
        if (cartItems.isEmpty()) {
            cartTable.setPlaceholder(new Label("Your cart is empty."));
        }
    }

    @FXML
    private void handleCheckout() {
        productController.checkout(cartItems);
        Stage stage = (Stage) checkoutButton.getScene().getWindow();
        stage.close();
    }
}

