package com.beanchainbeta.services;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import org.iq80.leveldb.DB;
import org.iq80.leveldb.DBIterator;

import com.beanchainbeta.controllers.DBManager;
import com.bean_core.TXs.*;
import com.fasterxml.jackson.databind.ObjectMapper;

public class RejectedService {
    private static final ObjectMapper mapper = new ObjectMapper();

    private static DB getRejectedDB() {
        return DBManager.getDB("rejectedDB");
    }

    public static void saveRejectedTransaction(TX tx) {
        try {
            String json = tx.createJSON();
            getRejectedDB().put(tx.getTxHash().getBytes(StandardCharsets.UTF_8), json.getBytes(StandardCharsets.UTF_8));
            System.out.println("Rejected TX saved: " + tx.getTxHash());
        } catch (Exception e) {
            System.out.println("Failed to save rejected TX: " + tx.getTxHash());
            e.printStackTrace();
        }
    }

    public static Map<String, String> getRejectedTxsForAddress(String address) {
        Map<String, String> result = new HashMap<>();
        try (DBIterator iterator = getRejectedDB().iterator()) {
            iterator.seekToFirst();
            while (iterator.hasNext()) {
                var entry = iterator.next();
                String txJson = new String(entry.getValue(), StandardCharsets.UTF_8);
                TX tx = TX.fromJSON(txJson);
                if (tx.getFrom().equals(address)) {
                    result.put(tx.getTxHash(), txJson);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }
}

