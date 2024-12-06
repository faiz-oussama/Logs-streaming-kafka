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
        }, 0, 1000); // Update every 500ms

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
        
        // Custom cell factory for log level column
        columnLogLevel.setCellFactory(column -> new TableCell<LogEntry, String>() {
            private final HBox container = new HBox();
            private final Label levelLabel = new Label();
            private final Region icon = new Region();
            
            {
                container.setAlignment(Pos.CENTER);
                container.setSpacing(5);
                icon.setMinSize(12, 12);
                icon.setMaxSize(12, 12);
                container.getChildren().addAll(icon, levelLabel);
                container.getStyleClass().add("log-level-cell");
            }
            
            @Override
            protected void updateItem(String level, boolean empty) {
                super.updateItem(level, empty);
                
                if (empty || level == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    levelLabel.setText(level);
                    container.getStyleClass().removeAll("log-level-ERROR", "log-level-WARNING", 
                        "log-level-INFO", "log-level-SUCCESS");
                    icon.getStyleClass().add("log-icon");
                    
                    switch (level) {
                        case "ERROR":
                            container.getStyleClass().add("log-level-ERROR");
                            icon.setStyle("-fx-shape: 'M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm1 15h-2v-2h2v2zm0-4h-2V7h2v6z';");
                            break;
                        case "WARNING":
                            container.getStyleClass().add("log-level-WARNING");
                            icon.setStyle("-fx-shape: 'M1 21h22L12 2 1 21zm12-3h-2v-2h2v2zm0-4h-2v-4h2v4z';");
                            break;
                        case "INFO":
                            container.getStyleClass().add("log-level-INFO");
                            icon.setStyle("-fx-shape: 'M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm1 15h-2v-6h2v6zm0-8h-2V7h2v2z';");
                            break;
                        default:
                            container.getStyleClass().add("log-level-SUCCESS");
                            icon.setStyle("-fx-shape: 'M9 16.17L4.83 12l-1.42 1.41L9 19 21 7l-1.41-1.41z';");
                    }
                    
                    setGraphic(container);
                    setText(null);
                }
            }
        });
        
        logsTable.setItems(logEntries);
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
