package com.beanchainbeta.network;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import com.beanpack.Block.*;

public class PendingBlockManager {
    private static final Map<String, Block> pendingBlocks = new ConcurrentHashMap<>();
    private static final List<Block> bufferedDuringSync = new ArrayList<>();

    public static void storePendingBlock(String blockHash, Block block) {
        pendingBlocks.put(blockHash, block);
    }

    public static Block getPendingBlock(String blockHash) {
        return pendingBlocks.get(blockHash);
    }

    public static void remove(String blockHash) {
        pendingBlocks.remove(blockHash);
    }

    public static Block getPendingBlockByMatchingTx(String txHash) {
        for (Block block : pendingBlocks.values()) {
            if (block.getTransactions().contains(txHash)) {
                return block;
            }
        }
        return null;
    }

    // Buffer incoming blocks during sync
    public static void bufferDuringSync(Block block) {
        synchronized (bufferedDuringSync) {
            bufferedDuringSync.add(block);
        }
    }

    // Retrieve buffered blocks after sync completes
    public static List<Block> getBufferedBlocks() {
        synchronized (bufferedDuringSync) {
            return new ArrayList<>(bufferedDuringSync);
        }
    }

    // Clear buffer once blocks have been processed
    public static void clearBufferedBlocks() {
        synchronized (bufferedDuringSync) {
            bufferedDuringSync.clear();
        }
    }

    public static Block getMostRecentPendingBlock() {
        if (pendingBlocks.isEmpty()) return null;
        return pendingBlocks.values().stream()
            .max(Comparator.comparingInt(Block::getHeight))
            .orElse(null);
    }
}

