package com.BankStats;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class AlertManager {
    private final List<Alert> alerts = new ArrayList<>();
    private final Path alertsFile;
    private final Gson gson;

    public AlertManager() {
        Path dir = Paths.get(System.getProperty("user.home"), ".bank-prices");
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            // Ignore
        }
        this.alertsFile = dir.resolve("alerts.json");
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        loadAlerts();
    }

    public void addAlert(Alert alert) {
        alerts.add(alert);
        saveAlerts();
    }

    public void removeAlert(String id) {
        alerts.removeIf(a -> a.getId().equals(id));
        saveAlerts();
    }

    public List<Alert> getAlerts() {
        return new ArrayList<>(alerts);
    }

    public List<Alert> getEnabledAlerts() {
        return alerts.stream()
                .filter(Alert::isEnabled)
                .collect(Collectors.toList());
    }

    public void checkAlerts(List<BankStatsPlugin.Row> currentData) {
        for (Alert alert : getEnabledAlerts()) {
            for (BankStatsPlugin.Row row : currentData) {
                if (row.id == alert.getItemId() && row.currentHigh != null) {
                    if (alert.checkCondition(row.currentHigh)) {
                        triggerAlert(alert, row.currentHigh);
                    }
                    break;
                }
            }
        }
    }

    private void triggerAlert(Alert alert, int currentPrice) {
        // For now, just print to console
        // Later: show notification, play sound, etc.
        System.out.println("🔔 ALERT TRIGGERED: " + alert + " (Current: " + currentPrice + " gp)");
    }

    public void saveAlerts() {
        try (BufferedWriter writer = Files.newBufferedWriter(alertsFile, StandardCharsets.UTF_8)) {
            gson.toJson(alerts, writer);
        } catch (IOException e) {
            System.err.println("Failed to save alerts: " + e.getMessage());
        }
    }

    public void loadAlerts() {
        if (!Files.exists(alertsFile)) {
            return;
        }

        try (BufferedReader reader = Files.newBufferedReader(alertsFile, StandardCharsets.UTF_8)) {
            List<Alert> loaded = gson.fromJson(reader, new TypeToken<List<Alert>>(){}.getType());
            if (loaded != null) {
                alerts.clear();
                alerts.addAll(loaded);
            }
        } catch (IOException e) {
            System.err.println("Failed to load alerts: " + e.getMessage());
        }
    }
}

