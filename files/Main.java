package com.atm;

import com.atm.api.DashboardServer;
import com.atm.service.ATMNetworkService;

public class Main {
    public static void main(String[] args) throws Exception {
        System.out.println("╔══════════════════════════════════════════╗");
        System.out.println("║     ATM Network Simulator Starting...    ║");
        System.out.println("╚══════════════════════════════════════════╝");

        ATMNetworkService networkService = new ATMNetworkService();
        DashboardServer httpServer = new DashboardServer(networkService);

        // Start services
        networkService.start();
        httpServer.start();

        System.out.println();
        System.out.println("  ✔  ATM simulator running (3 ATMs active)");
        System.out.println("  ✔  Dashboard: http://localhost:8080");
        System.out.println("  ✔  API: http://localhost:8080/api/atms");
        System.out.println("  ✔  API: http://localhost:8080/api/transactions");
        System.out.println();
        System.out.println("Press Ctrl+C to stop.");

        // Graceful shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\n[Shutdown] Stopping services...");
            networkService.stop();
            httpServer.stop();
            System.out.println("[Shutdown] Done.");
        }));

        // Keep main thread alive
        Thread.currentThread().join();
    }
}
