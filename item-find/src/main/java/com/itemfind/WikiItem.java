package com.itemfind;

import java.text.DecimalFormat;
import java.text.NumberFormat;


public class WikiItem {

    // Generic
    private String src_spwn_sell;
    private String imageUrl;

    // Cons specific
    private String level;
    private int quantity;
    private String quantityStr;
    private String rarityStr;
    private double rarity;
    private boolean isDrop; // true for drops, false for shops

    // Shop specific
    private String location;
    private int stock;
    private String restockTime;
    private int soldPrice;
    private int buyPrice;
    private Boolean isShop; // true for shops, false for drops

    NumberFormat nf = NumberFormat.getNumberInstance();

    // Constructor for drops
    public WikiItem(String imageUrl, String src_spwn_sell, String level, int quantity, String quantityStr, String rarityStr, double rarity) {
        this.imageUrl = imageUrl;
        this.src_spwn_sell = src_spwn_sell;
        this.level = level;
        this.quantity = quantity;
        this.quantityStr = quantityStr;
        this.rarityStr = rarityStr;
        this.rarity = rarity;
        this.isDrop = true;
        this.isShop = false; // This is a drop item
    }

    // Constructor for shops
    // return new WikiItem(imageUrl, src_spwn_sell, location, stock, restockTime, soldPrice, buyPrice);
    public WikiItem(String imageUrl, String src_spwn_sell, String location, int stock, String restockTime, int soldPrice, int buyPrice) {
        this.imageUrl = imageUrl;
        this.src_spwn_sell = src_spwn_sell;
        this.location = location; // Using location as level for shops
        this.stock = stock; // Using stock as quantity for shops
        this.restockTime = restockTime;
        this.soldPrice = soldPrice;
        this.buyPrice = buyPrice;
        this.isDrop = false; // This is a shop item
        this.isShop = true; // This is a shop item
    }

    // Public getters for shop constructor
    public String getLocation() {
        return location;
    }
    public int getStock() {
        return stock;
    }
    public String getRestockTime() {
        return restockTime;
    }
    public int getSoldPrice() {
        return soldPrice;
    }
    public int getBuyPrice() {
        return buyPrice;
    }
    public boolean isShop() {
        return isShop;
    }

    // Public getters for drops constructor
    public String getLevel() {
        return level;
    }
    public int getQuantity() {
        return quantity;
    }
    public String getQuantityStr() {
        return quantityStr;
    }
    public String getImageUrl() {
        return imageUrl;
    }
    public double getRarity() {
        return rarity;
    }
    public String getRarityStr() {
        return rarityStr;
    }
    public String src_spwn_sell() {
        return src_spwn_sell;
    }
    public boolean isDrop() {
        return isDrop;
    }
    public String getQuantityLabelText() {
        if (quantityStr.contains("-") || quantityStr.endsWith(" (noted)")) {
            return "x" + quantityStr;
        }
        return quantity > 0 ? "x" + nf.format(quantity) : quantityStr;
    }
    public String getQuantityLabelTextShort() {
        if (quantityStr.endsWith(" (noted)")) {
            return "x" + quantityStr.replaceAll("\\(.*\\)", "(n)").trim();
        }
        return getQuantityValueText();
    }
    public String getQuantityValueText() {
        return quantity > 0 ? "x" + rsFormat(quantity) : "";
    }
    private static String rsFormat(double number) {
        int power;
        String suffix = " KMBT";
        String formattedNumber = "";

        NumberFormat formatter = new DecimalFormat("#,###.#");
        power = (int) StrictMath.log10(number);
        number = number / (Math.pow(10, (power / 3) * 3));
        formattedNumber = formatter.format(number);
        formattedNumber = formattedNumber + suffix.charAt(power / 3);
        return formattedNumber.length() > 4 ? formattedNumber.replaceAll("\\.[0-9]+", "") : formattedNumber;
    }

}