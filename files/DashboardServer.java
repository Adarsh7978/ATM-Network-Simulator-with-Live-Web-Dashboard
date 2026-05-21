package com.atm.api;

import com.atm.service.ATMNetworkService;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.file.*;
import java.util.concurrent.Executors;

public class DashboardServer {
    private static final int PORT = 8080;
    private static final String WEB_DIR = "web";

    private final ATMNetworkService networkService;
    private HttpServer server;

    public DashboardServer(ATMNetworkService networkService) {
        this.networkService = networkService;
    }

    public void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress(PORT), 0);
        server.createContext("/api/atms", new AtmsHandler());
        server.createContext("/api/transactions", new TransactionsHandler());
        server.createContext("/", new StaticFileHandler());
        server.setExecutor(Executors.newFixedThreadPool(4));
        server.start();
        System.out.println("[DashboardServer] Running at http://localhost:" + PORT);
    }

    public void stop() {
        if (server != null) server.stop(0);
    }

    // ── /api/atms ────────────────────────────────────────────────────────────
    private class AtmsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange);
            if ("OPTIONS".equals(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }
            String json = networkService.getAtmsJson();
            sendJson(exchange, json);
        }
    }

    // ── /api/transactions ─────────────────────────────────────────────────────
    private class TransactionsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange);
            if ("OPTIONS".equals(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }
            String json = networkService.getTransactionsJson(50);
            sendJson(exchange, json);
        }
    }

    // ── Static file server ────────────────────────────────────────────────────
    private static class StaticFileHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String uriPath = exchange.getRequestURI().getPath();
            if ("/".equals(uriPath)) uriPath = "/index.html";

            Path filePath = Paths.get(WEB_DIR + uriPath);
            if (!Files.exists(filePath) || Files.isDirectory(filePath)) {
                String body = "404 Not Found";
                exchange.sendResponseHeaders(404, body.length());
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(body.getBytes());
                }
                return;
            }

            String contentType = detectContentType(uriPath);
            byte[] bytes = Files.readAllBytes(filePath);
            exchange.getResponseHeaders().set("Content-Type", contentType);
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        }

        private String detectContentType(String path) {
            if (path.endsWith(".html")) return "text/html; charset=utf-8";
            if (path.endsWith(".css"))  return "text/css; charset=utf-8";
            if (path.endsWith(".js"))   return "application/javascript; charset=utf-8";
            if (path.endsWith(".json")) return "application/json; charset=utf-8";
            if (path.endsWith(".ico"))  return "image/x-icon";
            return "application/octet-stream";
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
    private static void sendJson(HttpExchange exchange, String json) throws IOException {
        byte[] bytes = json.getBytes("UTF-8");
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(200, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private static void addCorsHeaders(HttpExchange exchange) {
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");
    }
}
