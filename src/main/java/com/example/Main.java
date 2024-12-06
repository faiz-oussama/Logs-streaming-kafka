package com.example;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {
    private LogVisController controller;

    @Override
    public void start(Stage primaryStage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/logVis.fxml"));
        Parent root = loader.load();
        controller = loader.getController();

        primaryStage.setTitle("Real Time Log Visualization");
        primaryStage.setScene(new Scene(root, 1200, 600));
        primaryStage.show();

        // Start the log streaming process
        controller.startLogStreaming();

        // Add shutdown hook
        primaryStage.setOnCloseRequest(event -> {
            controller.shutdown();
            System.exit(0);
        });
    }

    public static void main(String[] args) {
        launch(args);
    }
}
