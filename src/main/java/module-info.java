module com.example.maxecommerce {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.web;
    requires org.controlsfx.controls;
    requires com.dlsc.formsfx;
    requires net.synedra.validatorfx;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.bootstrapfx.core;
    requires com.almasb.fxgl.all;
    requires java.sql;
    requires com.fasterxml.jackson.databind;
    requires mysql.connector.j;
    requires annotations;

    opens com.example.maxecommerce to javafx.fxml;
    opens com.example.maxecommerce.controller to javafx.fxml;
    opens com.example.maxecommerce.model to javafx.base;
    exports com.example.maxecommerce;
}