package com.example.maxecommerce;

import com.example.maxecommerce.controller.HomeController;
import com.example.maxecommerce.controller.LoginController;
import com.example.maxecommerce.util.SessionManager;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.util.Objects;

public class App extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/maxecommerce/home/HomeView.fxml"));
        Parent root = loader.load();
        HomeController controller = loader.getController();
        controller.setHostServices(getHostServices());
        primaryStage.setTitle("Welcome!");
        primaryStage.setScene(new Scene(root));
        primaryStage.show();
    }

    @Override
    public void init() throws Exception {
        super.init();
        // Ensure SessionManager is initialized
        SessionManager.getInstance();
    }

    public static void main(String[] args) {
        launch(args);
    }
}