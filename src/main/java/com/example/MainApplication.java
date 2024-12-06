package com.example;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.scene.Parent;
import javafx.scene.control.Label;

public class MainApplication extends Application {
    private Parent logVisRoot;
    private Parent analyticsRoot;
    private LogVisController logVisController;
    private LogAnalyticsController analyticsController;
    private StackPane mainContainer;

    @Override
    public void start(Stage primaryStage) throws Exception {
        // Create main container
        mainContainer = new StackPane();
        
        // Load Log Visualization view
        FXMLLoader logVisLoader = new FXMLLoader(getClass().getResource("/logVis.fxml"));
        logVisRoot = logVisLoader.load();
        logVisController = logVisLoader.getController();
        
        // Load Log Analytics view
        FXMLLoader analyticsLoader = new FXMLLoader(getClass().getResource("/logAnalytics.fxml"));
        analyticsRoot = analyticsLoader.load();
        analyticsController = analyticsLoader.getController();
        
        // Connect the controllers
        logVisController.setAnalyticsController(analyticsController);
        
        // Set up view switching
        setupViewSwitching();
        
        // Initially show log visualization view
        mainContainer.getChildren().add(logVisRoot);
        
        // Create scene
        Scene scene = new Scene(mainContainer, 1280, 800);
        scene.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
        
        // Set up stage
        primaryStage.setTitle("Log Management System");
        primaryStage.setScene(scene);
        primaryStage.show();
        
        // Start log streaming
        logVisController.startLogStreaming();
    }

    private void setupViewSwitching() {
        // Get navbar items from LogVisController
        Label dashboardLabel = logVisController.getDashboardLabel();
        Label logsLabel = logVisController.getLogsLabel();
        
        // Add click handlers
        dashboardLabel.setOnMouseClicked(event -> {
            mainContainer.getChildren().clear();
            mainContainer.getChildren().add(analyticsRoot);
            
            // Update active states
            dashboardLabel.getStyleClass().add("nav-item-active");
            logsLabel.getStyleClass().remove("nav-item-active");
        });
        
        logsLabel.setOnMouseClicked(event -> {
            mainContainer.getChildren().clear();
            mainContainer.getChildren().add(logVisRoot);
            
            // Update active states
            logsLabel.getStyleClass().add("nav-item-active");
            dashboardLabel.getStyleClass().remove("nav-item-active");
        });
    }

    public static void main(String[] args) {
        launch(args);
    }
}
