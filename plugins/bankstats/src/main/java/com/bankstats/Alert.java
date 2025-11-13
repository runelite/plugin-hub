package com.bankstats;

import java.util.UUID;

public class Alert {
    private final String id;
    private final int itemId;
    private final String itemName;
    private final AlertType type;
    private final int targetValue;
    private final boolean enabled;
    private final long createdAt;

    public enum AlertType {
        PRICE_ABOVE("Price rises above"),
        PRICE_BELOW("Price falls below"),
        PRICE_EQUALS("Price equals");

        private final String display;

        AlertType(String display) {
            this.display = display;
        }

        @Override
        public String toString() {
            return display;
        }
    }

    public Alert(int itemId, String itemName, AlertType type, int targetValue, boolean enabled) {
        this.id = UUID.randomUUID().toString();
        this.itemId = itemId;
        this.itemName = itemName;
        this.type = type;
        this.targetValue = targetValue;
        this.enabled = enabled;
        this.createdAt = System.currentTimeMillis();
    }

    // Constructor for loading from disk
    public Alert(String id, int itemId, String itemName, AlertType type, int targetValue, boolean enabled, long createdAt) {
        this.id = id;
        this.itemId = itemId;
        this.itemName = itemName;
        this.type = type;
        this.targetValue = targetValue;
        this.enabled = enabled;
        this.createdAt = createdAt;
    }

    // Getters
    public String getId() { return id; }
    public int getItemId() { return itemId; }
    public String getItemName() { return itemName; }
    public AlertType getType() { return type; }
    public int getTargetValue() { return targetValue; }
    public boolean isEnabled() { return enabled; }
    public long getCreatedAt() { return createdAt; }

    public boolean checkCondition(int currentPrice) {
        if (!enabled) return false;

        switch (type) {
            case PRICE_ABOVE:
                return currentPrice > targetValue;
            case PRICE_BELOW:
                return currentPrice < targetValue;
            case PRICE_EQUALS:
                // Allow small margin for equality (±1%)
                int margin = Math.max(1, targetValue / 100);
                return Math.abs(currentPrice - targetValue) <= margin;
            default:
                return false;
        }
    }

    @Override
    public String toString() {
        return itemName + " - " + type + " " + targetValue + " gp";
    }
}

