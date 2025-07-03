package com.beanchainbeta.services;

import com.beanchainbeta.controllers.DBManager;
import com.beanchainbeta.logger.BeanLoggerManager;
import com.beanchainbeta.nodePortal.portal;
import com.beanpack.TXs.*;
import org.iq80.leveldb.DB;
import org.iq80.leveldb.DBIterator;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.iq80.leveldb.impl.Iq80DBFactory.asString;

public class CleanupService {
    private static final DB rejectedDB = DBManager.getDB("rejectedDB");
    
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
            BeanLoggerManager.BeanLogger("Deleted old rejected TX: " + key);
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
                BeanLoggerManager.BeanLogger("Removed timed-out mempool TX: " + tx.getTxHash());
            }
        }
    }

    // Run both
    public static void runFullCleanup() {
        BeanLoggerManager.BeanLoggerFPrint("Running full cleanup...");
        cleanRejectedDB();
        cleanMempoolTimeouts();
        BeanLoggerManager.BeanLoggerFPrint("Cleanup complete");
    }
}
