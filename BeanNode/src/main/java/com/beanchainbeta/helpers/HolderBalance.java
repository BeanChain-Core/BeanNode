package com.beanchainbeta.helpers;

//helper for rest api query
public class HolderBalance {
    private String address;
    private double balance;

    public HolderBalance(String address, double balance) {
        this.address = address;
        this.balance = balance;
    }

    public String getAddress() {
        return address;
    }

    public double getBalance() {
        return balance;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public void setBalance(double balance) {
        this.balance = balance;
    }
}
