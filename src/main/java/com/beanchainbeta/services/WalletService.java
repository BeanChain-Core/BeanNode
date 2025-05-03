package com.beanchainbeta.services;

import java.io.IOException;
import org.iq80.leveldb.DB;
import org.springframework.stereotype.Service;

import com.beanchainbeta.config.ConfigLoader;
import com.beanchainbeta.controllers.DBManager;
import com.bean_core.Models.*;
import com.bean_core.TXs.*;
import com.bean_core.crypto.*;
import com.bean_core.Utils.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import org.iq80.leveldb.DBIterator;
import static org.iq80.leveldb.impl.Iq80DBFactory.*;

@Service
public class WalletService {
    
    private static DB db;

    public WalletService() {
        if (db == null) {
            db = DBManager.getDB(ConfigLoader.getStateDB());
        }
    }
    
    

    //dev testing

    public static boolean walletExists(String address) {
        try {
            byte[] value = db.get(address.getBytes(StandardCharsets.UTF_8));
            return value != null;
        } catch (Exception e) {
            System.err.println("⚠️ Error checking wallet existence: " + address);
            e.printStackTrace();
            return false;
        }
    }

    public static void genBlock() throws IOException {
        String teamWallet = "BEANX:0x1c8496175b3f4802e395db5fab4dd66e09c431b2";
        double teamAllocation = 2500000;
        StateWallet team = new StateWallet();
        InBeanTx(teamWallet, teamAllocation);
    
        String earlyWallet = "BEANX:0xEARLYWALLET";
        double earlyWalletAllocation = 5000000;
        StateWallet early = new StateWallet();
        early.setAddy(earlyWallet);
        early.setBeantoshi(beantoshinomics.toBeantoshi(earlyWalletAllocation));
        InBeanTx(earlyWallet, earlyWalletAllocation);
    
        String faucetWallet = "BEANX:0xFAUCETWALLET";
        double faucetWalletAllocation = 5000000;
        StateWallet faucet = new StateWallet();
        faucet.setAddy(faucetWallet);
        faucet.setBeantoshi(beantoshinomics.toBeantoshi(faucetWalletAllocation));
        InBeanTx(faucetWallet, faucetWalletAllocation);
    
        String nodeReward = "BEANX:0xNODEREWARD";
        double nodeRewardAllocation = 30000000;
        StateWallet node = new StateWallet();   // <-- this was missing
        node.setAddy(nodeReward);
        node.setBeantoshi(beantoshinomics.toBeantoshi(nodeRewardAllocation));
        InBeanTx(nodeReward, nodeRewardAllocation);
    }

    public static void genTxProcess(TX tx) throws IOException{
        if(tx.getSignature().equals("GENESIS-SIGNATURE")) {
            String toKey = tx.getTo();
            double amount = tx.getAmount();
            InBeanTx(toKey, amount);   
            outBeanTx(tx.getFrom(), amount, tx.getGasFee());

        }
    }


    //end dev testing wallet storage 

    // update sender wallet state 
    private static void outBeanTx(String from, double amount, long gasFee) throws IOException{
        ObjectMapper mapper = new ObjectMapper();
        String fromKey = from;
        //System.out.println("Gas from frontend: " + gasFee);

        if (!beantoshinomics.isValidAmount(String.valueOf(amount))) {
            System.out.println("Invalid amount in outBeanTx: " + amount);
            return;
        }

        long beantoshi = beantoshinomics.toBeantoshi(amount);
        long totalCost = beantoshi + gasFee;
        byte[] fromJsonWallet = db.get(fromKey.getBytes(StandardCharsets.UTF_8));

        //update sender wallet
        if(fromJsonWallet != null) {
            ObjectNode walletNode = (ObjectNode) mapper.readTree(new String(fromJsonWallet, StandardCharsets.UTF_8));
            long currentBeantoshi = walletNode.get("beantoshi").asLong();
            long newBalance = currentBeantoshi - totalCost;
            walletNode.put("beantoshi", newBalance);
            int walletNonce = walletNode.get("nonce").asInt();
            walletNode.put("nonce", (walletNonce + 1));

            String updatedSender = mapper.writeValueAsString(walletNode);
            db.put(fromKey.getBytes(StandardCharsets.UTF_8), updatedSender.getBytes(StandardCharsets.UTF_8)); 
        } else {
            System.out.println("No Wallet Found");
        }
    }

    // update reciever wallet state 
    private static void InBeanTx(String to, double amount) throws IOException {
        System.out.println("[IN_BEAN_TX] To Address: " + to + " | Length: " + to.length());

        ObjectMapper mapper = new ObjectMapper();
        String toKey = to;

        if (!beantoshinomics.isValidAmount(String.valueOf(amount))) {
            System.out.println("Invalid amount in InBeanTx: " + amount);
            return;
        }

        long beantoshi = beantoshinomics.toBeantoshi(amount);
        byte[] toJsonWallet = db.get(toKey.getBytes(StandardCharsets.UTF_8));
        ObjectNode walletNode;
    
        if (toJsonWallet != null) {
    
            try {
                walletNode = (ObjectNode) mapper.readTree(new String(toJsonWallet, StandardCharsets.UTF_8));
                long currentBeantoshi = walletNode.get("beantoshi").asLong();
                walletNode.put("beantoshi", currentBeantoshi + beantoshi);
            } catch (Exception e) {
                e.printStackTrace();
                System.out.println("Error parsing JSON from DB!");
                return;
            }
        } else {
            System.out.println("No Wallet Found for: " + to + ", creating new one.");
            walletNode = mapper.createObjectNode();
            //staging to remove early wallet logic for reward node
            // if(getBeanBalance("BEANX:0xEARLYWALLET") > 100){
            //     TX tx = new TX("BEANX:0xEARLYWALLET","SYSTEM", to, 100 ,getNonce("BEANX:0xEARLYWALLET"), 0);
            //     tx.setSignature("GENESIS-SIGNATURE");
            //     MempoolService.addTransaction(tx.getTxHash(), tx.createJSON());
            // }
            walletNode.put("beantoshi", beantoshi);
            walletNode.put("nonce", 0);
        }
    
        // Try serializing JSON
        String updatedSender = "";
        try {
            updatedSender = mapper.writeValueAsString(walletNode);
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Error serializing JSON!");
            return;
        }
    
        System.out.println("Updated Wallet JSON: " + updatedSender);
    
        // Ensure JSON is not empty before saving
        if (updatedSender == null || updatedSender.trim().isEmpty()) {
            System.out.println("ERROR: Updated wallet JSON is empty, NOT writing to DB!");
            return;
        }
    
        // Save the updated wallet to LevelDB
        db.put(toKey.getBytes(StandardCharsets.UTF_8), updatedSender.getBytes(StandardCharsets.UTF_8));
    
        System.out.println("Successfully updated wallet for: " + to);
    }
    
    //close DB
    public void closeDB() {
        try {
            db.close();
        } catch (Exception e) {
            System.err.println(e);
        }
    }

    public static String getData(String key) {
        return asString(db.get(bytes(key)));
    }

    public static double getBeanBalance(String addy) {
        String json = getData(addy);
        double balance = 0;
        ObjectMapper objectMapper = new ObjectMapper();
    
        try {
            if (json == null) {
                System.err.println("⚠️ No data found for address: " + addy);
                return 0.0;
            }
    
            JsonNode rootNode = objectMapper.readTree(json);
            long beantoshiBalance = rootNode.get("beantoshi").asLong();
            balance = beantoshinomics.toBean(beantoshiBalance);
    
        } catch (Exception e) {
            e.printStackTrace();
        }
    
        return balance;
    }

    public static boolean hasCorrectAmount(String addy, double amount, long gasFee) {
        double totalRequired = amount + beantoshinomics.toBean(gasFee);
        double balance = getBeanBalance(addy);
    
        //System.out.println("Checking balance: " + balance + " BEAN vs required: " + totalRequired + " BEAN");
    
        return balance >= totalRequired;
    }

    public static void transfer(TX tx) throws IOException {
        //System.out.println("[TRANSFER] To Address: " + tx.getTo() + " | Length: " + tx.getTo().length());

        outBeanTx(tx.getFrom(), tx.getAmount(), tx.getGasFee());
        
        //System.out.println("TX TO: " + tx.getTo());
        InBeanTx(tx.getTo(), tx.getAmount());

        if (tx.getGasFee() > 0) {
            InBeanTx("BEANX:0xGASPOOL", beantoshinomics.toBean(tx.getGasFee()));
            //System.out.println("Credited " + tx.getGasFee() + " beantoshi to GASPOOL from " + tx.getFrom());
        }

        System.out.println("Transferred " + tx.getAmount() + " BEAN from " + tx.getFrom() + " to " + tx.getTo());

        
    }

    public static int getNonce(String addy){
        int nonce = 0;
        ObjectMapper mapper = new ObjectMapper();
    
        byte[] toJsonWallet = db.get(addy.getBytes(StandardCharsets.UTF_8));
        ObjectNode walletNode;
    
        if (toJsonWallet != null) {
            //System.out.println("Existing wallet data found for: " + addy);
            //System.out.println("Raw JSON from DB: " + new String(toJsonWallet, StandardCharsets.UTF_8));
    
            try {
                walletNode = (ObjectNode) mapper.readTree(new String(toJsonWallet, StandardCharsets.UTF_8));
                int currentNonce = walletNode.get("nonce").asInt();
                nonce = currentNonce;
            } catch (Exception e) {
                e.printStackTrace();
                System.out.println("Error parsing JSON from DB!");
            }
        } else {
            System.out.println("No Wallet Found for: " + addy + ".");
            
        }
        return nonce;
    }

    public static int incrementNonce(String addy) {
        int newNonce = 0;
        ObjectMapper mapper = new ObjectMapper();
    
        byte[] toJsonWallet = db.get(addy.getBytes(StandardCharsets.UTF_8));
    
        if (toJsonWallet != null) {
            try {
                ObjectNode walletNode = (ObjectNode) mapper.readTree(new String(toJsonWallet, StandardCharsets.UTF_8));
                int currentNonce = walletNode.get("nonce").asInt();
                newNonce = currentNonce + 1;
                walletNode.put("nonce", newNonce);
    
                String updatedJson = mapper.writeValueAsString(walletNode);
                db.put(addy.getBytes(StandardCharsets.UTF_8), updatedJson.getBytes(StandardCharsets.UTF_8));
    
                //System.out.println("Nonce updated to: " + newNonce);
    
            } catch (Exception e) {
                e.printStackTrace();
                System.out.println("Error parsing or updating wallet JSON!");
            }
        } else {
            System.out.println("ERROR: Tried to increment nonce for non-existent wallet: " + addy);
            // Optional: throw exception or return -1
        }
    
        return newNonce;
    }

    public static List<StateWallet> getAllWallets() {
        List<StateWallet> txs = new ArrayList<>();

        try (DBIterator iterator = db.iterator()) {
            iterator.seekToFirst();
            while (iterator.hasNext()) {
                var entry = iterator.next();
                String key = asString(entry.getKey());

                if (key.startsWith("BEANX:0x")) {
                    String json = asString(entry.getValue());
                    //System.out.println(key + " " + json);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return txs;
    }

    public static boolean updateWalletLabel(String address, String label, String signature, String publicKeyHex) throws Exception {
        // Step 1: Validate label prefix
        if (!label.startsWith("WUN:")) {
            throw new IllegalArgumentException("Only WUN: labels allowed");
        }
    
        // Step 2: Construct the verification message
        String message = "SET_LABEL:" + address + ":" + label;
    
        // Step 3: Hash the message
        byte[] messageHash = MessageDigest.getInstance("SHA-256").digest(message.getBytes(StandardCharsets.UTF_8));
    
        // Step 4: Verify signature using custom logic
        boolean isValid = TransactionVerifier.verifySHA256Transaction(publicKeyHex, messageHash, signature);
        if (!isValid) {
            throw new SecurityException("Invalid signature");
        }
    
        // Step 5: Make sure public key maps to the address
        boolean addressMatches = TransactionVerifier.walletMatch(publicKeyHex, address);
        if (!addressMatches) {
            throw new SecurityException("Public key does not match address");
        }
    
        // Step 6: Update LevelDB entry
        ObjectMapper mapper = new ObjectMapper();
        byte[] walletBytes = db.get(address.getBytes(StandardCharsets.UTF_8));
        ObjectNode walletNode = mapper.createObjectNode();
    
        if (walletBytes != null) {
            walletNode = (ObjectNode) mapper.readTree(new String(walletBytes, StandardCharsets.UTF_8));
        }
    
        walletNode.put("label", label);
    
        db.put(address.getBytes(StandardCharsets.UTF_8), mapper.writeValueAsBytes(walletNode));
        System.out.println("Label set for " + address + ": " + label);
        return true;
    }
    
    public static void creditGaspoolFromBlock(long gasFeeReward) throws IOException {
        String gaspool = "BEANX:0xGASPOOL";
        InBeanTx(gaspool, beantoshinomics.toBean(gasFeeReward));
        System.out.println("Gaspool credited with " + gasFeeReward + " beantoshi");
    }

    public static void payGasOnly(String from, long gasFee) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        String fromKey = from;
    
        if (gasFee <= 0) {
            System.out.println("Invalid gas fee in payGasOnly: " + gasFee);
            return;
        }
    
        byte[] fromJsonWallet = db.get(fromKey.getBytes(StandardCharsets.UTF_8));
    
        if (fromJsonWallet != null) {
            ObjectNode walletNode = (ObjectNode) mapper.readTree(new String(fromJsonWallet, StandardCharsets.UTF_8));
            long currentBeantoshi = walletNode.get("beantoshi").asLong();
    
            if (currentBeantoshi < gasFee) {
                System.err.println("❌ Insufficient balance for gas payment by: " + from);
                throw new IOException("Insufficient gas funds."); // force the TX to fail
            }
    
            long newBalance = currentBeantoshi - gasFee;
            walletNode.put("beantoshi", newBalance);
    
            int walletNonce = walletNode.get("nonce").asInt();
            walletNode.put("nonce", (walletNonce + 1));
    
            String updatedSender = mapper.writeValueAsString(walletNode);
            db.put(fromKey.getBytes(StandardCharsets.UTF_8), updatedSender.getBytes(StandardCharsets.UTF_8));
    
            System.out.println("✅ Gas fee of " + gasFee + " paid by: " + from);
        } else {
            System.err.println("❌ Wallet not found for gas payment: " + from);
            throw new IOException("Wallet not found.");
        }
    }

}

