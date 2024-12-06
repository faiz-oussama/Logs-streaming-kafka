package com.example;

import javafx.beans.property.*;

public class EndpointStats {
    private final StringProperty endpoint;
    private final IntegerProperty hits;
    private final DoubleProperty avgResponseTime;
    private final StringProperty errorRate;
    private int totalResponseTime;
    private int errorCount;

    public EndpointStats(String endpoint) {
        this.endpoint = new SimpleStringProperty(endpoint);
        this.hits = new SimpleIntegerProperty(0);
        this.avgResponseTime = new SimpleDoubleProperty(0.0);
        this.errorRate = new SimpleStringProperty("0.0%");
        this.totalResponseTime = 0;
        this.errorCount = 0;
    }

    public void incrementHits() {
        hits.set(hits.get() + 1);
        updateErrorRate();
    }

    public void incrementErrors() {
        errorCount++;
        updateErrorRate();
    }

    public void updateResponseTime(long responseTime) {
        totalResponseTime += responseTime;
        avgResponseTime.set((double) totalResponseTime / hits.get());
    }

    private void updateErrorRate() {
        double rate = (double) errorCount / hits.get() * 100;
        errorRate.set(String.format("%.1f%%", rate));
    }

    // Getters for properties
    public StringProperty endpointProperty() {
        return endpoint;
    }

    public IntegerProperty hitsProperty() {
        return hits;
    }

    public DoubleProperty avgResponseTimeProperty() {
        return avgResponseTime;
    }

    public StringProperty errorRateProperty() {
        return errorRate;
    }

    // Regular getters
    public String getEndpoint() {
        return endpoint.get();
    }

    public int getHits() {
        return hits.get();
    }

    public double getAvgResponseTime() {
        return avgResponseTime.get();
    }

    public String getErrorRate() {
        return errorRate.get();
    }
}
