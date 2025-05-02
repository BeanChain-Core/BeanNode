package com.beanchainbeta.services;

import com.beanchainbeta.network.Node;
import com.beanchainbeta.network.PeerInfo;
import com.beanchainbeta.nodePortal.portal;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.PrintWriter;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class MempoolSyncService {

    private static final ObjectMapper mapper = new ObjectMapper();

    public static void start() {
        Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(() -> {
            try {
                if (portal.isSyncing) return;

                Set<String> mempoolHashes = MempoolService.getAllTXHashes();

                // 🔇 Skip if empty
                if (mempoolHashes.isEmpty()) {
                    System.out.println("⏭️ Mempool is empty, skipping sync broadcast.");
                    return;
                }

                ObjectNode summary = mapper.createObjectNode();
                summary.put("type", "mempool_summary");

                ArrayNode hashArray = mapper.createArrayNode();
                for (String hash : mempoolHashes) {
                    hashArray.add(hash);
                }

                ObjectNode payload = mapper.createObjectNode();
                payload.set("txHashes", hashArray);
                summary.set("payload", payload);

                String message = mapper.writeValueAsString(summary);

                for (PeerInfo peer : Node.getConnectedPeers()) {
                    try {
                        PrintWriter out = new PrintWriter(peer.getSocket().getOutputStream(), true);
                        out.println(message);
                    } catch (Exception e) {
                        System.err.println("❌ Failed to send mempool summary to " + peer.getAddress());
                    }
                }

                System.out.println("🔄 Mempool summary broadcasted to peers. TX count: " + mempoolHashes.size());

            } catch (Exception e) {
                System.err.println("❌ Error in MempoolSyncService loop:");
                e.printStackTrace();
            }

        }, 60, 30, TimeUnit.SECONDS); 
    }
}
