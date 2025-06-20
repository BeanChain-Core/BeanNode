package com.beanchainbeta.services;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import org.iq80.leveldb.DB;
import org.iq80.leveldb.DBIterator;

import com.beanchainbeta.config.ConfigLoader;
import com.beanchainbeta.controllers.DBManager;
import com.beanchainbeta.logger.BeanLoggerManager;
import com.beanpack.TXs.*;

public class RejectedService {
    private static DB getRejectedDB() {
        return DBManager.getDB(ConfigLoader.getRejectedDB());
    }

    public static void saveRejectedTransaction(TX tx) {
        try {
            String json = tx.createJSON();
            getRejectedDB().put(tx.getTxHash().getBytes(StandardCharsets.UTF_8), json.getBytes(StandardCharsets.UTF_8));
            BeanLoggerManager.BeanLogger("Rejected TX saved: " + tx.getTxHash());
        } catch (Exception e) {
            BeanLoggerManager.BeanLoggerError("Failed to save rejected TX: " + tx.getTxHash());
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

