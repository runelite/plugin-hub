package com.itemfind;

import java.text.DecimalFormat;
import java.text.NumberFormat;


public class WikiItem {

    private String src_spwn_sell;
    private String level;
    private String imageUrl;
    private int quantity;
    private String quantityStr;
    private String rarityStr;
    private double rarity;

    NumberFormat nf = NumberFormat.getNumberInstance();

    public WikiItem(String imageUrl, String src_spwn_sell, String level, int quantity, String quantityStr, String rarityStr, double rarity) {
        this.imageUrl = imageUrl;
        this.src_spwn_sell = src_spwn_sell;
        this.level = level;
        this.quantity = quantity;
        this.quantityStr = quantityStr;
        this.rarityStr = rarityStr;
        this.rarity = rarity;
    }

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