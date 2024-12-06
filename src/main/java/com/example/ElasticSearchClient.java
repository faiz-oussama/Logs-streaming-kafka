package com.example;

import java.io.IOException;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import javax.net.ssl.SSLContext;

import org.apache.http.Header;
import org.apache.http.HttpHost;
import org.apache.http.message.BasicHeader;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.ssl.TrustStrategy;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.indices.GetIndexResponse;
import co.elastic.clients.json.JsonData;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.FieldValue;

public class ElasticSearchClient {
    private final ElasticsearchClient esClient;
    private final KafkaLogProducer kafkaProducer;
    private volatile boolean isRunning = true;
    private Thread pollingThread;
    private static final int BATCH_SIZE = 1;
    private AtomicInteger currentFrom = new AtomicInteger(0);

    public ElasticSearchClient(String serverUrl, String apiKey) {
        try {
            SSLContext sslContext = SSLContextBuilder.create()
                    .loadTrustMaterial((TrustStrategy) (X509Certificate[] chain, String authType) -> true)
                    .build();

            RestClientBuilder builder = RestClient.builder(HttpHost.create(serverUrl))
                    .setDefaultHeaders(new Header[]{
                            new BasicHeader("Authorization", "ApiKey " + apiKey)
                    })
                    .setHttpClientConfigCallback(httpClientBuilder -> 
                            httpClientBuilder.setSSLContext(sslContext));

            ObjectMapper objectMapper = new ObjectMapper()
                    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                    .registerModule(new JavaTimeModule());

            RestClient restClient = builder.build();
            ElasticsearchTransport transport = new RestClientTransport(
                    restClient,
                    new JacksonJsonpMapper(objectMapper));

            this.esClient = new ElasticsearchClient(transport);
            this.kafkaProducer = new KafkaLogProducer("localhost:9092", "logs-topic");

        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize Elasticsearch client", e);
        }
    }

    public void startPolling() {
        try {
            currentFrom.set(0);  // Reset the counter when starting
            System.out.println("Starting polling for new logs...");
            pollingThread = new Thread(() -> {
                AtomicReference<String> lastProcessedId = new AtomicReference<>();
                while (isRunning) {
                    try {
                        List<LogEntry> newLogs = fetchNewLogs(lastProcessedId);
                        for (LogEntry log : newLogs) {
                            kafkaProducer.sendLog(log);
                        }
                        Thread.sleep(1000); // Poll every second
                    } catch (Exception e) {
                        System.err.println("Error in polling thread: " + e.getMessage());
                        e.printStackTrace();
                    }
                }
            });
            pollingThread.start();
        } catch (Exception e) {
            System.err.println("Error starting polling: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void stopPolling() {
        isRunning = false;
        if (pollingThread != null) {
            pollingThread.interrupt();
        }
        if (kafkaProducer != null) {
            kafkaProducer.close();
        }
    }

    public List<LogEntry> fetchNewLogs(AtomicReference<String> lastProcessedId) throws IOException {
        List<LogEntry> logs = new ArrayList<>();
        
        try {
            SearchResponse<LogEntry> response = esClient.search(s -> s
                    .index("filebeat-logs-*")
                    .from(currentFrom.get())
                    .size(BATCH_SIZE)
                    .sort(sort -> sort.field(f -> f.field("@timestamp").order(SortOrder.Asc)))
                    .query(q -> q
                            .matchAll(m -> m)
                    ), LogEntry.class);

            List<Hit<LogEntry>> hits = response.hits().hits();
            if (!hits.isEmpty()) {
                Hit<LogEntry> hit = hits.get(0);
                if (hit.source() != null) {
                    logs.add(hit.source());
                    currentFrom.incrementAndGet();
                    System.out.println("Fetched log " + currentFrom.get() + " of " + response.hits().total().value());
                }
            } else {
                // If no more results, reset the counter to get new logs that might have been added
                Thread.sleep(1000);  // Wait a bit before resetting to avoid tight loop
                currentFrom.set(0);
            }
        } catch (Exception e) {
            System.err.println("Error fetching logs: " + e.getMessage());
            e.printStackTrace();
        }

        return logs;
    }

    public List<LogEntry> fetchAllLogs() {
        return fetchLogs("filebeat-logs-*");
    }

    public List<LogEntry> fetchLogs(String index) {
        List<LogEntry> logs = new ArrayList<>();
        AtomicInteger from = new AtomicInteger(0);

        try {
            boolean hasMoreResults = true;
            while (hasMoreResults) {    
                SearchResponse<LogEntry> response = esClient.search(s -> s
                        .index(index)
                        .from(from.get())
                        .size(BATCH_SIZE)
                        .query(q -> q
                                .matchAll(m -> m)
                        ), LogEntry.class);

                List<Hit<LogEntry>> hits = response.hits().hits();
                if (hits.isEmpty()) {
                    hasMoreResults = false;
                } else {
                    for (Hit<LogEntry> hit : hits) {
                        logs.add(hit.source());
                    }
                    from.addAndGet(BATCH_SIZE);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return logs;
    }

    public void close() {
        stopPolling();
        if (kafkaProducer != null) {
            kafkaProducer.close();
        }
    }

    public static void main(String[] args) {
        String serverUrl = "https://localhost:9200";
        String apiKey = "amtLeTQ1SUJoeXBuVTRjeTRGZGg6b05ReEd4aWxRZVNLOC1mdE5yUFhaQQ==";
        ElasticSearchClient client = new ElasticSearchClient(serverUrl, apiKey);
        client.startPolling();
    }
}