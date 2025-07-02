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

    public static TX rejectionFlagTx(TX tx, String reason) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode metaNode = (ObjectNode) mapper.readTree(tx.getMeta());

        // Check if there's already a rejectionReason field
        if (metaNode.has("rejectionReason")) {
            JsonNode existingNode = metaNode.get("rejectionReason");

            // If it's already an array, append to it
            if (existingNode.isArray()) {
                ((com.fasterxml.jackson.databind.node.ArrayNode) existingNode).add(reason);
            }
            // If it's a string, convert to array and add new reason
            else if (existingNode.isTextual()) {
                com.fasterxml.jackson.databind.node.ArrayNode arr = mapper.createArrayNode();
                arr.add(existingNode.asText());
                arr.add(reason);
                metaNode.set("rejectionReason", arr);
            }
        } else {
            // Just set the single reason
            metaNode.put("rejectionReason", reason);
        }

        tx.setMeta(metaNode.toString());
        return tx;
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

