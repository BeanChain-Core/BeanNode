package com.beanchainbeta.services;

import com.beanchainbeta.controllers.DBManager;
import com.beanchainbeta.nodePortal.portal;
import com.bean_core.TXs.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.iq80.leveldb.DB;
import org.iq80.leveldb.DBIterator;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.iq80.leveldb.impl.Iq80DBFactory.asString;

public class CleanupService {
    private static final DB rejectedDB = DBManager.getDB("rejectedDB");
    private static final ObjectMapper mapper = new ObjectMapper();
    private static final long MAX_AGE_MS = 6 * 60 * 60 * 1000; // 6 hours in milliseconds

    // Remove old rejected TXs
    public static void cleanRejectedDB() {
        long now = System.currentTimeMillis();
        List<String> keysToDelete = new ArrayList<>();

        try (DBIterator iterator = rejectedDB.iterator()) {
            iterator.seekToFirst();
            while (iterator.hasNext()) {
                var entry = iterator.next();
                String key = asString(entry.getKey());
                String json = asString(entry.getValue());
                TX tx = TX.fromJSON(json);
                if (tx == null) continue;

                long age = now - tx.getTimeStamp();
                if (age > MAX_AGE_MS) {
                    keysToDelete.add(key);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        for (String key : keysToDelete) {
            rejectedDB.delete(key.getBytes(StandardCharsets.UTF_8));
            System.out.println("Deleted old rejected TX: " + key);
        }
    }

    // Remove timed-out mempool TXs
    public static void cleanMempoolTimeouts() {
        long now = System.currentTimeMillis();
        ArrayList<TX> mempool = MempoolService.getTxFromPool();
    
        for (TX tx : mempool) {
            if (tx == null) continue;
    
            long age = now - tx.getTimeStamp();
            if (tx.getTimeStamp() > portal.BOOT_TIME && age > MAX_AGE_MS) {
                MempoolService.removeSingleTx(tx.getTxHash());
                System.out.println("Removed timed-out mempool TX: " + tx.getTxHash());
            }
        }
    }

    // Run both
    public static void runFullCleanup() {
        System.out.println("Running full cleanup...");
        cleanRejectedDB();
        cleanMempoolTimeouts();
        System.out.println("Cleanup complete");
    }
}
