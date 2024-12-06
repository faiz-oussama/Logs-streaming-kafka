package com.example;

import javafx.fxml.FXML;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.layout.Region;
import javafx.geometry.Pos;
import javafx.scene.shape.Shape;

import java.net.HttpURLConnection;
import java.net.URL;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Timer;
import java.util.TimerTask;
import org.kordamp.ikonli.javafx.FontIcon;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import java.util.List;
import java.io.File;
import java.util.concurrent.atomic.AtomicReference;

public class LogVisController {

    @FXML
    private ImageView logo;
    @FXML
    private HBox navbar;
    @FXML
    private TableView<LogEntry> logsTable;
    @FXML
    private TableColumn<LogEntry, String> columnIP;
    @FXML
    private TableColumn<LogEntry, String> columnTimestamp;
    @FXML
    private TableColumn<LogEntry, String> columnRequestType;
    @FXML
    private TableColumn<LogEntry, String> columnStatusCode;
    @FXML
    private TableColumn<LogEntry, String> columnAction;
    @FXML
    private TableColumn<LogEntry, String> columnLogLevel;
    @FXML
    private DatePicker startDate;
    @FXML
    private DatePicker endDate;
    @FXML
    private Button filterButton;

    private KafkaLogConsumer logConsumer;
    private Timer updateTimer;
    private final ObservableList<LogEntry> logEntries = FXCollections.observableArrayList();
    private ObjectMapper objectMapper = new ObjectMapper();
    private ElasticSearchClient elasticSearchClient;
    private AtomicReference<String> lastProcessedId = new AtomicReference<>("");
    private LogAnalyticsController analyticsController;

    public void setAnalyticsController(LogAnalyticsController controller) {
        this.analyticsController = controller;
    }

    @FXML
    public void initialize() {
        setupTableColumns();
    }

    public void startLogStreaming() {
        // Initialize Kafka consumer first
        logConsumer = new KafkaLogConsumer("localhost:9092", "log-vis-group", "logs-topic");
        logConsumer.start();

        // Start periodic UI updates from Kafka
        updateTimer = new Timer(true);
        updateTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                updateLogsTable();
            }
        }, 0, 500); // Update every 500ms

        // Initialize Elasticsearch and start fetching logs
        initializeElasticSearch();
    }

    private void setupTableColumns() {
        columnIP.setCellValueFactory(new PropertyValueFactory<>("clientIp"));
        columnTimestamp.setCellValueFactory(new PropertyValueFactory<>("timestamp"));
        columnRequestType.setCellValueFactory(new PropertyValueFactory<>("httpMethod"));
        columnStatusCode.setCellValueFactory(new PropertyValueFactory<>("statusCode"));
        columnAction.setCellValueFactory(new PropertyValueFactory<>("action"));
        columnLogLevel.setCellValueFactory(new PropertyValueFactory<>("logLevel"));
        
        setupLogLevelColumn();
        
        logsTable.setItems(logEntries);
    }

    private void setupLogLevelColumn() {
        columnLogLevel.setCellFactory(column -> new TableCell<LogEntry, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                    setStyle("");
                } else {
                    HBox container = new HBox();
                    container.getStyleClass().add("log-level");
                    container.setAlignment(Pos.CENTER);
                    
                    // Create icon
                    FontIcon icon = new FontIcon();
                    icon.getStyleClass().add("log-level-icon");
                    
                    // Create label with the log level text
                    Label label = new Label(item);
                    label.getStyleClass().add("log-level-text");
                    
                    // Add appropriate style classes based on log level
                    switch (item.toUpperCase()) {
                        case "ERROR":
                            container.getStyleClass().add("log-level-error");
                            icon.setIconLiteral("fas-exclamation-circle");
                            break;
                        case "WARN":
                            container.getStyleClass().add("log-level-warn");
                            icon.setIconLiteral("fas-exclamation-triangle");
                            break;
                        case "INFO":
                            container.getStyleClass().add("log-level-info");
                            icon.setIconLiteral("fas-info-circle");
                            break;
                    }
                    
                    container.getChildren().addAll(icon, label);
                    setGraphic(container);
                    setText(null);
                }
            }
        });
    }

    private void updateLogsTable() {
        List<LogEntry> newLogs = logConsumer.getNewLogs();
        if (!newLogs.isEmpty()) {
            Platform.runLater(() -> {
                logEntries.addAll(newLogs);
                // Keep only the last 1000 logs to prevent memory issues
                if (logEntries.size() > 1000) {
                    logEntries.remove(0, logEntries.size() - 1000);
                }
                logsTable.refresh();
                // Auto-scroll to latest logs
                logsTable.scrollTo(logEntries.size() - 1);
                
                // Send logs to analytics controller
                if (analyticsController != null) {
                    for (LogEntry log : newLogs) {
                        analyticsController.processNewLog(log);
                    }
                }
            });
        }
    }

    private void initializeElasticSearch() {
        String serverUrl = "https://localhost:9200";
        String apiKey = "amtLeTQ1SUJoeXBuVTRjeTRGZGg6b05ReEd4aWxRZVNLOC1mdE5yUFhaQQ==";
        
        try {
            elasticSearchClient = new ElasticSearchClient(serverUrl, apiKey);
            KafkaLogProducer kafkaProducer = new KafkaLogProducer("localhost:9092", "logs-topic");
            
            // Start a background thread to poll Elasticsearch and send to Kafka
            Thread elasticSearchThread = new Thread(() -> {
                AtomicReference<String> lastProcessedId = new AtomicReference<>("");
                
                while (!Thread.currentThread().isInterrupted()) {
                    try {
                        List<LogEntry> newLogs = elasticSearchClient.fetchNewLogs(lastProcessedId);
                        for (LogEntry log : newLogs) {
                            kafkaProducer.sendLog(log);
                        }
                        Thread.sleep(1000); // Poll every second
                    } catch (Exception e) {
                        System.err.println("Error polling Elasticsearch: " + e.getMessage());
                        try {
                            Thread.sleep(5000); // Wait longer on error
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            break;
                        }
                    }
                }
                kafkaProducer.close();
            });
            elasticSearchThread.setDaemon(true);
            elasticSearchThread.start();
        } catch (Exception e) {
            System.err.println("Failed to initialize Elasticsearch: " + e.getMessage());
        }
    }

    public void shutdown() {
        if (updateTimer != null) {
            updateTimer.cancel();
        }
        if (logConsumer != null) {
            logConsumer.stop();
        }
        if (elasticSearchClient != null) {
            elasticSearchClient.close();
        }
    }
}
