package com.beanchainbeta.genesis;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import com.bean_core.TXs.*;
import com.fasterxml.jackson.databind.ObjectMapper;

public class GenesisTX extends TX{
    private String from;
    private String to;
    private double amount;
    private String txhash;
    private String signature;
    private long timeStamp = 0L; 
    private int nonce = 0;

    public String getFrom() {return from;}
    public String getTo() {return to;}
    public double getAmount() {return amount;}
    public String getTxHash() {return txhash;}
    public String getSignature() {return signature;}
    public long getTimeStamp() { return timeStamp; }
    public int getNonce() { return nonce; }

    public GenesisTX() {

    }

    public GenesisTX(String to, double amount) {
        from = "BEANX:00000000000000000000";
        this.to = to;
        this.amount = amount;
        txhash = generateHash();
        signature = "GENESIS-SIGNATURE";

    }

    public String generateHash() {
        try {
            String formattedAmount = String.format("%.8f", amount); // Force 8 decimal places
            String data = from + to + formattedAmount;
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data.getBytes(StandardCharsets.UTF_8));
    
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                hexString.append(String.format("%02x", b));
            }
            return hexString.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public String createJSON() {
        String jsonString = "";
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            jsonString = objectMapper.writeValueAsString(this);
        } catch (Exception e) {
            System.out.println(e);
        }
        return jsonString;
    }
}
