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
import com.beanpack.Utils.MetaHelper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

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

    public static TX getRejectedTxByHash(String hash) {
        try {
            byte[] data = getRejectedDB().get(hash.getBytes(StandardCharsets.UTF_8));
            if (data != null) {
                String json = new String(data, StandardCharsets.UTF_8);
                return TX.fromJSON(json);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static Map<String, String> getAllRejectedTxs() {
        Map<String, String> result = new HashMap<>();
        try (DBIterator iterator = getRejectedDB().iterator()) {
            iterator.seekToFirst();
            while (iterator.hasNext()) {
                var entry = iterator.next();
                String key = new String(entry.getKey(), StandardCharsets.UTF_8);
                String txJson = new String(entry.getValue(), StandardCharsets.UTF_8);
                result.put(key, txJson);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

}

