package com.example;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Paths;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

public class LocalHttpServer {

    public static HttpServer startServer() throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(8000), 0);
        
        server.createContext("/", new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                String filePath = "src/main/resources" + exchange.getRequestURI().getPath();
                
                if (Files.exists(Paths.get(filePath))) {
                    byte[] content = Files.readAllBytes(Paths.get(filePath));
                    exchange.sendResponseHeaders(200, content.length);
                    OutputStream os = exchange.getResponseBody();
                    os.write(content);
                    os.close();
                } else {
                    String notFoundMessage = "404 (Not Found)";
                    exchange.sendResponseHeaders(404, notFoundMessage.length());
                    OutputStream os = exchange.getResponseBody();
                    os.write(notFoundMessage.getBytes());
                    os.close();
                }
            }
        });
        
        server.setExecutor(null); 
        server.start();
        return server;
    }
}
