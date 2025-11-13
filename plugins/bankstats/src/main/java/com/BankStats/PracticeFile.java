package com.BankStats;

public class PracticeFile {

    private String name;
    private int id = 50;

    public PracticeFile(String name, int id) {
        this.name = name;
        this.id = id;
    }

    public void PrintInfo() {
        System.out.println("Name: " + name + ", ID: " + id);
    }





    public static void main(String[] args) {
        System.out.println("This is a practice file.");
        System.out.println("Creating an instance of PracticeFile...");
        PracticeFile pf = new PracticeFile("SampleName", 123);
        System.out.println("Printing info:");
        pf.PrintInfo();
    }
}
