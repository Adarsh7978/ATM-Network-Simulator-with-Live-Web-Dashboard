package com.atm.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Transaction {
    public enum Type { WITHDRAW, BALANCE_CHECK, PIN_CHANGE, FAILED_PIN_ATTEMPT }
    public enum Status { SUCCESS, FAILED }

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final String timestamp;
    private final String atmId;
    private final Type type;
    private final long amount;
    private final Status status;

    public Transaction(String atmId, Type type, long amount, Status status) {
        this.timestamp = LocalDateTime.now().format(FMT);
        this.atmId = atmId;
        this.type = type;
        this.amount = amount;
        this.status = status;
    }

    // Constructor for reading from CSV
    public Transaction(String timestamp, String atmId, Type type, long amount, Status status) {
        this.timestamp = timestamp;
        this.atmId = atmId;
        this.type = type;
        this.amount = amount;
        this.status = status;
    }

    public String getTimestamp() { return timestamp; }
    public String getAtmId() { return atmId; }
    public Type getType() { return type; }
    public long getAmount() { return amount; }
    public Status getStatus() { return status; }

    public String toCsvLine() {
        return String.format("%s,%s,%s,%d,%s", timestamp, atmId, type.name(), amount, status.name());
    }

    public String toJson() {
        return String.format(
            "{\"timestamp\":\"%s\",\"atmId\":\"%s\",\"type\":\"%s\",\"amount\":%d,\"status\":\"%s\"}",
            timestamp, atmId, type.name(), amount, status.name()
        );
    }

    public static Transaction fromCsvLine(String line) {
        String[] parts = line.split(",");
        if (parts.length < 5) return null;
        try {
            String ts = parts[0].trim();
            String atmId = parts[1].trim();
            Type type = Type.valueOf(parts[2].trim());
            long amount = Long.parseLong(parts[3].trim());
            Status status = Status.valueOf(parts[4].trim());
            return new Transaction(ts, atmId, type, amount, status);
        } catch (Exception e) {
            return null;
        }
    }
}
