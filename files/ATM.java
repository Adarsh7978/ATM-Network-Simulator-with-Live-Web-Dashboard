package com.atm.model;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class ATM {
    private final String id;
    private final String location;
    private final AtomicLong cashBalance;
    private final AtomicInteger failedPinAttempts;
    private volatile boolean suspicious;

    public ATM(String id, String location, long initialBalance) {
        this.id = id;
        this.location = location;
        this.cashBalance = new AtomicLong(initialBalance);
        this.failedPinAttempts = new AtomicInteger(0);
        this.suspicious = false;
    }

    public String getId() { return id; }
    public String getLocation() { return location; }
    public long getCashBalance() { return cashBalance.get(); }
    public int getFailedPinAttempts() { return failedPinAttempts.get(); }
    public boolean isSuspicious() { return suspicious; }
    public void setSuspicious(boolean suspicious) { this.suspicious = suspicious; }

    public boolean withdraw(long amount) {
        while (true) {
            long current = cashBalance.get();
            if (current < amount) return false;
            if (cashBalance.compareAndSet(current, current - amount)) return true;
        }
    }

    public void incrementFailedPin() {
        failedPinAttempts.incrementAndGet();
    }

    public String toJson() {
        return String.format(
            "{\"id\":\"%s\",\"location\":\"%s\",\"cashBalance\":%d,\"failedPinAttempts\":%d,\"suspicious\":%b}",
            id, location, cashBalance.get(), failedPinAttempts.get(), suspicious
        );
    }
}
