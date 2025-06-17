package com.beanchainbeta.services;


import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.iq80.leveldb.DB;
import org.iq80.leveldb.DBIterator;
import org.springframework.stereotype.Service;


import com.beanpack.TXs.*;
import com.beanchainbeta.config.ConfigLoader;
import com.beanchainbeta.controllers.DBManager;
import com.beanchainbeta.helpers.DevConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.iq80.leveldb.impl.Iq80DBFactory.*;

@Service
public class MempoolService {
    private static ConcurrentHashMap<String, String> transactions = new ConcurrentHashMap<>();
    public static ConcurrentHashMap<String, TX> rejectedTransactions = new ConcurrentHashMap<>();
    private static DB db;
    
    public MempoolService() {
        try {
            db = DBManager.getDB(ConfigLoader.getMempoolDB());
            loadMempoolFromDB();
        } catch (Exception e) {
            throw new RuntimeException("Error initializing mempool DB", e);
        }
    }
    
    public static boolean addTransaction(String txHash, String transactionJson) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode txNode = mapper.readTree(transactionJson);

            if (txNode.has("signature") && "GENESIS-SIGNATURE".equals(txNode.get("signature").asText())) {
                if (DevConfig.devMode) {System.out.println("Skipping Genesis TX, not adding to mempool: " + txHash);}
                return false;
            }

            if (!transactions.containsKey(txHash)) {
                transactions.put(txHash, transactionJson);
                try {
                    db.put(bytes(txHash), bytes(transactionJson));
                } catch (Exception e) {
                    System.err.println("Error saving transaction to DB: " + e.getMessage());
                }
                return true;
            }
        } catch (Exception e) {
            System.err.println("Error parsing transaction JSON: " + e.getMessage());
        }

        return false;
    }

        

        public static void removeTXs(ArrayList<TX> acceptedTxs, ConcurrentHashMap<String, TX> rejectedTxs) {
            for (TX tx : acceptedTxs) {
                String txHash = tx.getTxHash();
                if (transactions.containsKey(txHash)) {
                    transactions.remove(txHash);
                    try {
                        db.delete(bytes(txHash));
                    } catch (Exception e) {
                        System.err.println("Error deleting accepted TX from DB: " + e.getMessage());
                    }
                } else {
                    //System.out.println(txHash + ": not found in mempool");
                }
            }
        
            for (String txHash : rejectedTxs.keySet()) {
                if (transactions.containsKey(txHash)) {
                    transactions.remove(txHash);
                    try {
                        db.delete(bytes(txHash));
                    } catch (Exception e) {
                        System.err.println("Error deleting rejected TX from DB: " + e.getMessage());
                    }
                } else {
                    System.out.println(txHash + ": not found in mempool");
                }
            }
        }

        public static void removeSingleTx(String txHash) {
            if (transactions.containsKey(txHash)) {
                transactions.remove(txHash);
                try {
                    db.delete(bytes(txHash));
                    System.out.println("Removed TX from mempool DB and memory: " + txHash);
                } catch (Exception e) {
                    System.err.println("Error deleting TX from DB: " + txHash);
                    e.printStackTrace();
                }
            } else {
                System.out.println("TX not found in mempool: " + txHash);
            }
        }
        
        
    
    public ConcurrentHashMap<String, String> getTransactions() {
        return transactions;
    }
    
    private void loadMempoolFromDB() {
        try (DBIterator iterator = db.iterator()) {
            iterator.seekToFirst();
            while (iterator.hasNext()) {
                java.util.Map.Entry<byte[], byte[]> entry = iterator.next();
                String txHash = new String(entry.getKey(), StandardCharsets.UTF_8);
                String transactionJson = new String(entry.getValue(), StandardCharsets.UTF_8);
                transactions.put(txHash, transactionJson);
            }
            System.out.println("Mempool restored from LevelDB.");
        } catch (Exception e) {
            System.err.println("Error loading mempool from DB: " + e.getMessage());
        }
    }
    
    public static ArrayList<TX> getTxFromPool(){
        ArrayList<TX> txList = new ArrayList<>();
        for( String d: transactions.values()) {
        txList.add(TX.fromJSON(d));
        //System.out.println(TX.fromJSON(d));
    }
    return txList; 
    }

    public static ArrayList<TX> getPending(String addy) {
        ArrayList<TX> pendingTX = new ArrayList<>();
    
        for (String d : transactions.values()) {
            TX tx = TX.fromJSON(d);
            if (tx != null && tx.getFrom().equals(addy)) {
                pendingTX.add(tx);
            }
        }
    
        return pendingTX;
    }

    public static boolean contains(String txHash) {
        return transactions.containsKey(txHash);
    }

    public static TX getTransaction(String txHash) {
        String json = transactions.get(txHash);
        if (json == null) return null;
    
        try {
            return TX.fromJSON(json);
        } catch (Exception e) {
            System.err.println("❌ Failed to parse TX from mempool: " + txHash);
            e.printStackTrace();
            return null;
        }
    }

    public static Set<String> getAllTXHashes() {
        return transactions.keySet();
    }

    public static void removeTxByHash(String txHash) {
        TX tx = getTransaction(txHash); // get the full object before it’s removed
        if (tx != null) {
            tx.setStatus("rejected");
            RejectedService.saveRejectedTransaction(tx);
        }
        removeSingleTx(txHash);
    }


}

