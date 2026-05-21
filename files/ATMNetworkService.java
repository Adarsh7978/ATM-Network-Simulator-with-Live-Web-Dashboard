package com.atm.service;

import com.atm.model.ATM;
import com.atm.model.Transaction;
import com.atm.model.Transaction.Type;
import com.atm.model.Transaction.Status;
import com.atm.util.CsvStorage;

import java.util.*;
import java.util.concurrent.*;

public class ATMNetworkService {

    private final Map<String, ATM> atms = new LinkedHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
    private final Random random = new Random();

    public ATMNetworkService() {
        atms.put("ATM-101", new ATM("ATM-101", "Connaught Place, New Delhi", 50_000));
        atms.put("ATM-102", new ATM("ATM-102", "MG Road, Bengaluru", 50_000));
        atms.put("ATM-103", new ATM("ATM-103", "Marine Drive, Mumbai", 50_000));
    }

    public void start() {
        // Transaction simulator: each ATM fires every 2–5s independently
        for (ATM atm : atms.values()) {
            scheduleNextTransaction(atm);
        }

        // Fraud detector: runs every 30s
        scheduler.scheduleAtFixedRate(this::detectSuspiciousActivity, 5, 30, TimeUnit.SECONDS);

        System.out.println("[ATMNetworkService] Started — simulating " + atms.size() + " ATMs.");
    }

    private void scheduleNextTransaction(ATM atm) {
        long delayMs = 2000 + random.nextInt(3000); // 2–5 seconds
        scheduler.schedule(() -> {
            simulateTransaction(atm);
            scheduleNextTransaction(atm); // chain next
        }, delayMs, TimeUnit.MILLISECONDS);
    }

    private void simulateTransaction(ATM atm) {
        int roll = random.nextInt(100);
        Type type;
        long amount = 0;
        Status status = Status.SUCCESS;

        if (roll < 60) {
            type = Type.WITHDRAW;
            // Random amount: multiple of 500 between 500 and 10000
            amount = (500L + random.nextInt(19) * 500L);
            boolean ok = atm.withdraw(amount);
            status = ok ? Status.SUCCESS : Status.FAILED;
        } else if (roll < 80) {
            type = Type.BALANCE_CHECK;
        } else if (roll < 90) {
            type = Type.PIN_CHANGE;
        } else {
            type = Type.FAILED_PIN_ATTEMPT;
            atm.incrementFailedPin();
            status = Status.FAILED;
        }

        Transaction tx = new Transaction(atm.getId(), type, amount, status);
        CsvStorage.append(tx);

        System.out.printf("[TX] %s | %-18s | %-20s | ₹%,6d | %s%n",
            tx.getTimestamp(), atm.getId(), type, amount, status);
    }

    private void detectSuspiciousActivity() {
        for (ATM atm : atms.values()) {
            int failedCount = CsvStorage.recentFailedPins(atm.getId(), 10).size();
            boolean wasSuspicious = atm.isSuspicious();
            atm.setSuspicious(failedCount >= 3);
            if (!wasSuspicious && atm.isSuspicious()) {
                System.out.printf("[ALERT] %s marked SUSPICIOUS — %d failed PIN attempts in last 10 min%n",
                    atm.getId(), failedCount);
            }
        }
    }

    public Collection<ATM> getAllATMs() {
        return atms.values();
    }

    public String getAtmsJson() {
        StringBuilder sb = new StringBuilder("[");
        boolean first = true;
        for (ATM atm : atms.values()) {
            if (!first) sb.append(",");
            sb.append(atm.toJson());
            first = false;
        }
        sb.append("]");
        return sb.toString();
    }

    public String getTransactionsJson(int limit) {
        List<Transaction> txs = CsvStorage.readLast(limit);
        // Reverse so newest first
        Collections.reverse(txs);
        StringBuilder sb = new StringBuilder("[");
        boolean first = true;
        for (Transaction t : txs) {
            if (!first) sb.append(",");
            sb.append(t.toJson());
            first = false;
        }
        sb.append("]");
        return sb.toString();
    }

    public void stop() {
        scheduler.shutdownNow();
    }
}
