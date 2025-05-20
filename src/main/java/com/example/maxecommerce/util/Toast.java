package com.example.maxecommerce.util;

import javafx.animation.PauseTransition;
import javafx.animation.TranslateTransition;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

public class Toast {
    public enum ToastType { SUCCESS, ERROR, WARNING, INFO }

    public static void show(String message, ToastType type, Scene scene) {
        Stage toastStage = new Stage();
        toastStage.initOwner(scene.getWindow());
        toastStage.initStyle(StageStyle.TRANSPARENT);
        toastStage.setAlwaysOnTop(true);

        Label toastLabel = new Label(message);
        toastLabel.getStyleClass().addAll("toast", "toast-text");
        switch (type) {
            case SUCCESS:
                toastLabel.getStyleClass().add("toast-success");
                break;
            case ERROR:
                toastLabel.getStyleClass().add("toast-error");
                break;
            case INFO:
                toastLabel.getStyleClass().add("toast-info");
                break;
        }

        StackPane pane = new StackPane(toastLabel);
        pane.setAlignment(Pos.CENTER);
        Scene toastScene = new Scene(pane, 300, 50);
        toastScene.setFill(null);
        toastScene.getStylesheets().add(Toast.class.getResource("/com/example/maxecommerce/auth/auth.css").toExternalForm());

        toastStage.setScene(toastScene);
        toastStage.setX(scene.getWindow().getX() + scene.getWindow().getWidth() - 320);
        toastStage.setY(scene.getWindow().getY() + 20);

        // Slide-in animation
        TranslateTransition slideIn = new TranslateTransition(Duration.millis(200), pane);
        slideIn.setFromX(300);
        slideIn.setToX(0);
        slideIn.play();

        // Auto-dismiss after 3 seconds
        PauseTransition delay = new PauseTransition(Duration.seconds(3));
        delay.setOnFinished(e -> {
            TranslateTransition slideOut = new TranslateTransition(Duration.millis(200), pane);
            slideOut.setToX(300);
            slideOut.setOnFinished(event -> toastStage.close());
            slideOut.play();
        });
        delay.play();
    }
}