package com.example;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.stage.Stage;
import javafx.scene.Parent;

public class MainApplication extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        // Create tab pane
        TabPane tabPane = new TabPane();
        
        // Load Log Visualization view
        FXMLLoader logVisLoader = new FXMLLoader(getClass().getResource("/logVis.fxml"));
        Parent logVisRoot = logVisLoader.load();
        LogVisController logVisController = logVisLoader.getController();
        
        // Load Log Analytics view
        FXMLLoader analyticsLoader = new FXMLLoader(getClass().getResource("/logAnalytics.fxml"));
        Parent analyticsRoot = analyticsLoader.load();
        LogAnalyticsController analyticsController = analyticsLoader.getController();
        
        // Connect the controllers
        logVisController.setAnalyticsController(analyticsController);
        
        // Create tabs
        Tab logVisTab = new Tab("Log Visualization", logVisRoot);
        Tab analyticsTab = new Tab("Log Analytics", analyticsRoot);
        logVisTab.setClosable(false);
        analyticsTab.setClosable(false);
        
        // Add tabs to tab pane
        tabPane.getTabs().addAll(logVisTab, analyticsTab);
        
        // Create scene
        Scene scene = new Scene(tabPane, 1280, 800);
        scene.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
        
        // Set up stage
        primaryStage.setTitle("Log Management System");
        primaryStage.setScene(scene);
        primaryStage.show();
        
        // Start log streaming
        logVisController.startLogStreaming();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
