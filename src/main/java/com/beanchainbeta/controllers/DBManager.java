package com.beanchainbeta.controllers;

import org.iq80.leveldb.DB;
import org.iq80.leveldb.Options;
import org.springframework.stereotype.Component;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.iq80.leveldb.impl.Iq80DBFactory.*;

@Component
public class DBManager {
    private static final Map<String, DB> databases = new HashMap<>();
    private static DB dbInstance;

    private DBManager() {}

    public static synchronized DB getDB(String dbName) {
        if (!databases.containsKey(dbName)) {
            try {
                Options options = new Options();
                options.createIfMissing(true);
                DB db = factory.open(new File(dbName), options);
                databases.put(dbName, db);
            } catch (IOException e) {
                throw new RuntimeException("Failed to open LevelDB for " + dbName + ": " + e.getMessage(), e);
            }
        }
        return databases.get(dbName);
    }

    public static void closeDB(String dbName) {
        if (databases.containsKey(dbName)) {
            try {
                databases.get(dbName).close();
                databases.remove(dbName);
                System.out.println("LevelDB closed: " + dbName);
            } catch (IOException e) {
                System.err.println("Error closing LevelDB for " + dbName + ": " + e.getMessage());
            }
        }
    }

    public static void closeAll() {
        for (String dbName : databases.keySet()) {
            closeDB(dbName);
        }
    }
}

