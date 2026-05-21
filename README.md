# ATM Network Simulator with Live Web Dashboard

A real-time ATM monitoring system built with pure Java and Vanilla JS — no frameworks used.

## Features
- Simulates 3 ATMs (Delhi, Bengaluru, Mumbai)
- Live dashboard with auto-refresh every 3 seconds
- Cash level monitoring (Green/Orange/Red)
- Fraud detection — alerts on 3+ failed PIN attempts
- All transactions logged to CSV

## Tech Stack
- Core Java (JDK 11+) — Multithreading, HTTP Server
- Vanilla HTML/CSS/JavaScript
- File-based storage (CSV)

## How to Run
```bash
java -jar atm-simulator.jar
```
Open browser: http://localhost:8080

## Project Structure
- `files/` — Java source files
- `web/` — Frontend dashboard
- `data/` — Transaction logs (auto-created)
