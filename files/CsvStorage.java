package com.atm.util;

import com.atm.model.Transaction;

import java.io.*;
import java.nio.file.*;
import java.util.*;

public class CsvStorage {
    private static final String DATA_DIR = "data";
    private static final String CSV_FILE = DATA_DIR + "/transactions.csv";
    private static final String CSV_HEADER = "timestamp,atmId,type,amount,status";

    static {
        try {
            Files.createDirectories(Paths.get(DATA_DIR));
            File f = new File(CSV_FILE);
            if (!f.exists()) {
                try (PrintWriter pw = new PrintWriter(new FileWriter(f))) {
                    pw.println(CSV_HEADER);
                }
            }
        } catch (IOException e) {
            System.err.println("Failed to init CSV storage: " + e.getMessage());
        }
    }

    public static synchronized void append(Transaction t) {
        try (PrintWriter pw = new PrintWriter(new FileWriter(CSV_FILE, true))) {
            pw.println(t.toCsvLine());
        } catch (IOException e) {
            System.err.println("Failed to write transaction: " + e.getMessage());
        }
    }

    public static synchronized List<Transaction> readAll() {
        List<Transaction> list = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(CSV_FILE))) {
            String line;
            boolean first = true;
            while ((line = br.readLine()) != null) {
                if (first) { first = false; continue; } // skip header
                Transaction t = Transaction.fromCsvLine(line);
                if (t != null) list.add(t);
            }
        } catch (IOException e) {
            System.err.println("Failed to read transactions: " + e.getMessage());
        }
        return list;
    }

    public static synchronized List<Transaction> readLast(int n) {
        List<Transaction> all = readAll();
        int from = Math.max(0, all.size() - n);
        return all.subList(from, all.size());
    }

    /**
     * Returns all FAILED_PIN_ATTEMPT transactions for the given ATM
     * within the last windowMinutes minutes.
     */
    public static synchronized List<Transaction> recentFailedPins(String atmId, int windowMinutes) {
        List<Transaction> all = readAll();
        List<Transaction> result = new ArrayList<>();
        long nowMs = System.currentTimeMillis();
        long windowMs = (long) windowMinutes * 60 * 1000;
        for (Transaction t : all) {
            if (!t.getAtmId().equals(atmId)) continue;
            if (t.getType() != Transaction.Type.FAILED_PIN_ATTEMPT) continue;
            try {
                java.time.LocalDateTime ldt = java.time.LocalDateTime.parse(
                    t.getTimestamp(),
                    java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                );
                long txMs = ldt.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
                if (nowMs - txMs <= windowMs) result.add(t);
            } catch (Exception ignored) {}
        }
        return result;
    }
}
