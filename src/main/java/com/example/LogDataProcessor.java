package com.example;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class LogDataProcessor {

    public Map<String, Long> getProtocolDistribution(List<LogEntry> logs) {
        return logs.stream()
                   .collect(Collectors.groupingBy(LogEntry::getHttpMethod, Collectors.counting()));
    }

    public Map<Integer, Long> getStatusCodeDistribution(List<LogEntry> logs) {
        return logs.stream()
                   .collect(Collectors.groupingBy(LogEntry::getStatusCode, Collectors.counting()));
    }

}

