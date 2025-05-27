package com.beanchainbeta.network;

import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.iq80.leveldb.DBIterator;

import com.bean_core.Block.Block;
import com.beanchainbeta.config.ConfigLoader;
import com.beanchainbeta.nodePortal.portal;
import com.beanchainbeta.services.MempoolService;
import com.beanchainbeta.services.blockchainDB;
import com.beanchainbeta.validation.BlockBuilderV2;
import com.bean_core.TXs.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class MessageRouter {

    public MessageRouter() {}

    public void route(JsonNode message, Socket peer) {
        if (!message.has("type")) {
            System.out.println("Invalid message (missing 'type')");
            return;
        }

        String type = message.get("type").asText();

        switch (type) {
            case "handshake":
                handleHandshake(message, peer);
                break;
            case "sync_request":
                handleSyncRequest(message, peer);
                break;
            case "sync_response":
                handleSyncResponse(message);
                break;
            case "transaction":
                handleIncomingTransaction(message);
                break;
            case "block":
                handleIncomingBlock(message, peer);
                break;
            case "mempool_summary":
                handleMempoolSummary(message.get("payload"), peer);
                break;
            case "txRequestBatch":
                handleTxRequestBatch(message.get("payload"), peer);
                break;
            case "txBatch":
                handleTxBatch(message.get("payload"));
                break;
            case "tx_rejected": 
                String txHash = message.get("payload").get("txHash").asText();
                System.out.println("🔁 Rejection gossip received for TX: " + txHash);
                MempoolService.removeTxByHash(txHash);
                break;    
            default:
                System.out.println("Unknown message type: " + type);
        }
    }

    private void handleHandshake(JsonNode msg, Socket peer) {
        try {
    
            String peerAddress = msg.has("address") ? msg.get("address").asText() : "UNKNOWN";
            int peerHeight = msg.has("blockHeight") ? msg.get("blockHeight").asInt() : 0;
            boolean requestSync = msg.has("requestSync") && msg.get("requestSync").asBoolean();
            String syncMode = msg.has("syncMode") ? msg.get("syncMode").asText() : "FULL";
            boolean isValidator = msg.has("isValidator") && msg.get("isValidator").asBoolean();
            boolean isPublicNode = msg.has("isPublicNode") && msg.get("isPublicNode").asBoolean(); 
            boolean isReply = msg.has("reply") && msg.get("reply").asBoolean();
            String nodeType = msg.has("nodeType") ? msg.get("nodeType").asText() : "BEANNODE";
    
            System.out.println("Received handshake from " + peerAddress +
                " (height=" + peerHeight + ", wantsSync=" + requestSync +
                ", mode=" + syncMode + ", validator=" + isValidator + ", public=" + isPublicNode + "nodeType=" + nodeType  + ")");
    
            PeerInfo info = new PeerInfo(peer, peerAddress, syncMode, nodeType, isValidator);
            Node.registerPeer(peer, info);
    
            int myHeight = blockchainDB.getHeight();
    
            if (requestSync && myHeight > peerHeight) {
                sendSyncResponse(peer, peerHeight, syncMode);
            }
            if(!isReply) {
                sendHandshakeBack(peer);
            }

    
        } catch (Exception e) {
            System.out.println("Failed to parse handshake request.");
            e.printStackTrace();
        }
    }

    private void sendSyncResponse(Socket peer, int peerHeight, String syncMode) {
        try {
            PrintWriter out = new PrintWriter(peer.getOutputStream(), true);
            ObjectMapper mapper = new ObjectMapper();
    
            ArrayNode blocksArray = mapper.createArrayNode();
            ArrayNode txArray = mapper.createArrayNode();
            ArrayNode mempoolArray = mapper.createArrayNode();
    
            int myHeight = blockchainDB.getHeight();
    
            // 🧱 Add blocks only if full sync
            if (!"TX_ONLY".equalsIgnoreCase(syncMode)) {
                for (int i = peerHeight + 1; i <= myHeight; i++) {
                    byte[] blockBytes = portal.beanchainTest.db.get(("block-" + i).getBytes(StandardCharsets.UTF_8));
                    if (blockBytes != null) {
                        JsonNode blockJson = mapper.readTree(new String(blockBytes, StandardCharsets.UTF_8));
                        blocksArray.add(blockJson);
                    }
                }
            }
    
            // ✅ Always add confirmed TXs
            try (DBIterator iterator = portal.beanchainTest.db.iterator()) {
                for (iterator.seekToFirst(); iterator.hasNext(); iterator.next()) {
                    String key = new String(iterator.peekNext().getKey(), StandardCharsets.UTF_8);
                    if (key.startsWith("tran-")) {
                        String txJson = new String(iterator.peekNext().getValue(), StandardCharsets.UTF_8);
                        txArray.add(mapper.readTree(txJson));
                    }
                }
            }
    
            // 🧃 Add mempool only if full sync
            if (!"TX_ONLY".equalsIgnoreCase(syncMode)) {
                for (TX tx : MempoolService.getTxFromPool()) {
                    String txJson = mapper.writeValueAsString(tx);
                    mempoolArray.add(mapper.readTree(txJson));
                }
            }
    
            // 📨 Build response
            ObjectNode response = mapper.createObjectNode();
            response.put("type", "sync_response");
            response.put("latestHeight", myHeight);
            response.set("confirmedTxs", txArray);
    
            if (!"TX_ONLY".equalsIgnoreCase(syncMode)) {
                response.set("blocks", blocksArray);
                response.set("mempool", mempoolArray);
            }
    
            out.println(mapper.writeValueAsString(response));
    
            System.out.println("📤 Sent " + (syncMode.equalsIgnoreCase("TX_ONLY") ? "TX_ONLY" : "FULL") +
                " sync_response to " + peer.getInetAddress().getHostAddress() +
                " | Confirmed TXs: " + txArray.size() +
                (syncMode.equalsIgnoreCase("TX_ONLY") ? "" :
                    " | Blocks: " + blocksArray.size() +
                    " | Mempool TXs: " + mempoolArray.size()));
    
        } catch (Exception e) {
            System.out.println("❌ Failed to send sync_response.");
            e.printStackTrace();
        }
    }

    private void handleSyncRequest(JsonNode message, Socket peer) {
        try {
            PrintWriter out = new PrintWriter(peer.getOutputStream(), true);
            ObjectMapper mapper = new ObjectMapper();
    
            String syncMode = message.has("syncMode") ? message.get("syncMode").asText() : "FULL";
            int peerHeight = message.has("latestHeight") ? message.get("latestHeight").asInt() : -1;
            int myHeight = blockchainDB.getHeight();

            
            if (peerHeight >= myHeight) {
                System.out.println("Peer " + peer.getInetAddress().getHostAddress() + " already in sync. No sync_response sent.");
                return;
            }
    
            ArrayNode blocksArray = mapper.createArrayNode();
            ArrayNode confirmedTxArray = mapper.createArrayNode();
            ArrayNode mempoolArray = mapper.createArrayNode();
    
            // 🔹 Step 1: Always load confirmed TXs
            Set<String> blockTxHashes = new HashSet<>();
            for (int i = 0; i <= myHeight; i++) {
                byte[] blockBytes = portal.beanchainTest.db.get(("block-" + i).getBytes(StandardCharsets.UTF_8));
                if (blockBytes != null) {
                    JsonNode blockJson = mapper.readTree(new String(blockBytes, StandardCharsets.UTF_8));
                    blocksArray.add(blockJson);

                    JsonNode txList = blockJson.get("transactions");
                    if (txList != null && txList.isArray()) {
                        for (JsonNode txHashNode : txList) {
                            blockTxHashes.add(txHashNode.asText());
                        }
                    }
                }
            }

            for (String txHash : blockTxHashes) {
                byte[] txBytes = portal.beanchainTest.db.get(("tran-" + txHash).getBytes(StandardCharsets.UTF_8));
                if (txBytes != null) {
                    JsonNode txJson = mapper.readTree(new String(txBytes, StandardCharsets.UTF_8));
                    if (txJson.has("signature") && txJson.get("signature").asText().equals("GENESIS-SIGNATURE")) {
                        System.out.println("Skipping genesis TX from sync: " + txHash);
                        continue;
                    }
                    confirmedTxArray.add(txJson);
                } else {
                    System.err.println("Could not find TX from block: " + txHash);
                }
            }
    
            if (syncMode.equalsIgnoreCase("TX_ONLY")) {
                // ✂️ Skip blocks and mempool
                ObjectNode response = mapper.createObjectNode();
                response.put("type", "sync_response");
                response.put("latestHeight", myHeight);
                response.set("confirmedTxs", confirmedTxArray);
    
                out.println(mapper.writeValueAsString(response));
                System.out.println("Sent TX_ONLY sync_response to: " + peer.getInetAddress());
    
                return;
            }
    
            // 🔹 Otherwise, do full sync
            for (int i = 1; i <= myHeight; i++) {
                byte[] blockBytes = portal.beanchainTest.db.get(("block-" + i).getBytes(StandardCharsets.UTF_8));
                if (blockBytes != null) {
                    JsonNode blockJson = mapper.readTree(new String(blockBytes, StandardCharsets.UTF_8));
                    blocksArray.add(blockJson);
                }
            }
    
            for (TX tx : MempoolService.getTxFromPool()) {
                String txJson = mapper.writeValueAsString(tx);
                mempoolArray.add(mapper.readTree(txJson));
            }
    
            ObjectNode response = mapper.createObjectNode();
            response.put("type", "sync_response");
            response.put("latestHeight", myHeight);
            response.set("blocks", blocksArray);
            response.set("confirmedTxs", confirmedTxArray);
            response.set("mempool", mempoolArray);
    
            out.println(mapper.writeValueAsString(response));
    
            System.out.println("Sent FULL sync_response to " + peer.getInetAddress().getHostAddress() +
                " | Blocks: " + blocksArray.size() +
                " | Confirmed TXs: " + confirmedTxArray.size() +
                " | Mempool TXs: " + mempoolArray.size());
    
        } catch (Exception e) {
            System.err.println("Failed to handle sync_request:");
            e.printStackTrace();
        }
    }
    

    private void handleSyncResponse(JsonNode msg) {
        portal.setIsSyncing(true);
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode confirmedTxs = msg.get("confirmedTxs");
            JsonNode blocks = msg.get("blocks");
            JsonNode mempool = msg.get("mempool");
    
            // ✅ Step 1: Load TXs into mempool
            System.out.println("Loading confirmed and mempool TXs into local mempool...");
    
            for (JsonNode txNode : confirmedTxs) {
                TX tx = mapper.treeToValue(txNode, TX.class);
                MempoolService.addTransaction(tx.getTxHash(), tx.createJSON());
            }
    
            for (JsonNode txNode : mempool) {
                TX tx = mapper.treeToValue(txNode, TX.class);
                MempoolService.addTransaction(tx.getTxHash(), tx.createJSON());
            }
    
            System.out.println("Mempool now contains " + MempoolService.getTxFromPool().size() + " total TXs.");

            
    
            // ✅ Step 2: Parse and sort all blocks to replay
            List<Block> blocksToReplay = new ArrayList<>();

            

            for (JsonNode blockNode : blocks) {
                Block block = mapper.treeToValue(blockNode, Block.class);
                blocksToReplay.add(block);
            }
    
            blocksToReplay.sort(Comparator.comparingInt(Block::getHeight));

            if (blocksToReplay.isEmpty() || blocksToReplay.get(blocksToReplay.size() - 1).getHeight() <= blockchainDB.getHeight()) {
                        System.out.println("Node is already at latest block height. Skipping sync replay.");
                        portal.setIsSyncing(false);
                        return;
                    }
            // ✅ Step 3: Replay blocks
            for (Block block : blocksToReplay) {
                if (block.getHeight() == 0) continue; // Skip genesis
                System.out.println("REPLAY HASH: " + block.getHash() + " Params: Height: " + block.getHeight() + " PrevHash: " + block.getPreviousHash() + " MerkleRoot: " + block.getMerkleRoot());
                System.out.println("Replaying block #" + block.getHeight());
                BlockBuilderV2.blockReplay(block);
            }
    
            // ✅ Step 4: Exit sync mode
            portal.setIsSyncing(false);
            System.out.println("✅ Replay complete. Node is now in sync.");
    
            // ✅ Step 5: Now process any buffered blocks
            List<Block> buffered = PendingBlockManager.getBufferedBlocks();
            System.out.println("⏭ Processing " + buffered.size() + " buffered blocks...");
    
            for (Block b : buffered) {
                if (b.getHeight() <= blockchainDB.getHeight()) {
                    System.out.println("Skipping buffered block at height " + b.getHeight() + " (already processed or duplicate)");
                    continue;
                }
                System.out.println("▶️ Buffered block: #" + b.getHeight());
                BlockBuilderV2.blockReplay(b);
            }
    
            // ✅ Step 6: Clean up
            PendingBlockManager.clearBufferedBlocks();
            System.out.println("✔️ Sync fully completed and buffer flushed.");
    
        } catch (Exception e) {
            System.err.println("❌ Failed during sync_response processing:");
            e.printStackTrace();
        }
    }

    private void handleIncomingTransaction(JsonNode msg) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            TX tx = mapper.treeToValue(msg.get("payload"), TX.class);
            if(tx==null){
                System.out.println("INCOMING TX NULL");
                return;
            }
            String txHash = tx.getTxHash();
    
            // 🔁 Check if already in mempool
            if (MempoolService.contains(txHash)) {
                System.out.println("Duplicate TX received, already in mempool: " + txHash);
                return;
            }
            
            if (blockchainDB.checkTxConfirmed(txHash)) {
                System.out.println("Duplicate TX received, already 'COMPLETE': " + txHash);
                return;
            }
    
            // ✅ Add and broadcast
            MempoolService.addTransaction(txHash, tx.createJSON());
            System.out.println("New TX added to mempool: " + txHash);
    
            // 🌐 Gossip to other peers
            Node.broadcastTransactionStatic(tx);

            System.out.println("➡️ Raw incoming TX: " + tx.createJSON());
            //System.out.println("➡️ From: " + tx.getFrom() + " | Nonce: " + tx.getNonce());
            //System.out.println("➡️ Hash: " + tx.getTxHash());
            //System.out.println("➡️ Valid JSON: " + tx.createJSON().contains(tx.getTxHash())); // sanity

            TX pulled = MempoolService.getTransaction(tx.getTxHash());
            if (pulled == null) {
                System.err.println("🛑 TX was not saved in memory map.");
            } else {
                System.out.println("✅ TX saved in mempool memory.");
            }

    
        } catch (Exception e) {
            System.err.println("❌ Error handling incoming TX");
            e.printStackTrace();
        }
    }
    

    private void handleIncomingBlock(JsonNode msg, Socket peer) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode payload = msg.get("payload");
            Block incomingBlock = mapper.treeToValue(payload, Block.class);
    
            if (portal.isSyncing) {
                PendingBlockManager.bufferDuringSync(incomingBlock);
                System.out.println("Held block #" + incomingBlock.getHeight() + " during sync.");
                return;
            }
    
            // Step 1: Validate block signature
            if (!incomingBlock.signatureValid()) {
                System.err.println("Invalid block signature for block #" + incomingBlock.getHeight());
                return;
            }
    
            // Step 2: Check height and previous hash
            int localHeight = blockchainDB.getHeight();
            if (incomingBlock.getHeight() != localHeight + 1) {
                System.err.println("Block height mismatch. Expected: " + (localHeight + 1) +
                                   " but got: " + incomingBlock.getHeight());
                return;
            }
    
            Block latestBlock = blockchainDB.getLatestBlock();
            String expectedPrevHash = latestBlock != null ? latestBlock.getHash() : "00000000000000000000";
    
            if (!incomingBlock.getPreviousHash().equals(expectedPrevHash)) {
                System.err.println("Previous hash mismatch for block #" + incomingBlock.getHeight());
                return;
            }
    
            // Step 3: Replay block from mempool using known-safe logic
            System.out.println("Replaying and validating block #" + incomingBlock.getHeight());
            BlockBuilderV2.blockReplay(incomingBlock);
    
            //Step 4: Remove TXs from mempool
            List<String> hashes = incomingBlock.getTransactions();
            ArrayList<TX> toRemove = new ArrayList<>();
            for (String hash : hashes) {
                TX tx = MempoolService.getTransaction(hash);
                if (tx != null) {
                    toRemove.add(tx);
                }
            }
            MempoolService.removeTXs(toRemove, new ConcurrentHashMap<>());
    
            System.out.println("Incoming block #" + incomingBlock.getHeight() + " accepted and rebuilt.");
    
        } catch (Exception e) {
            System.err.println("Failed to process incoming block:");
            e.printStackTrace();
        }
    }

    private void handleMempoolSummary(JsonNode payload, Socket peer) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode remoteHashesNode = payload.get("txHashes");
            if (remoteHashesNode == null || !remoteHashesNode.isArray()) return;
    
            Set<String> remoteHashes = new HashSet<>();
            for (JsonNode node : remoteHashesNode) {
                remoteHashes.add(node.asText());
            }
    
            Set<String> localHashes = MempoolService.getAllTXHashes();
            Set<String> missingHashes = new HashSet<>(remoteHashes);
            missingHashes.removeAll(localHashes);
    
            if (!missingHashes.isEmpty()) {
                ObjectNode request = mapper.createObjectNode();
                request.put("type", "txRequestBatch");
                ArrayNode reqHashes = mapper.createArrayNode();
                for (String h : missingHashes) reqHashes.add(h);
                request.set("payload", reqHashes);
    
                PrintWriter out = new PrintWriter(peer.getOutputStream(), true);
                out.println(mapper.writeValueAsString(request));
                System.out.println("📥 Requested missing TXs: " + missingHashes.size());
            }

            System.out.println("📥 Received mempool summary from peer: " + peer.getInetAddress());

    
        } catch (Exception e) {
            System.err.println("❌ Failed to handle mempool_summary:");
            e.printStackTrace();
        }
    }

    private void handleTxRequestBatch(JsonNode payload, Socket peer) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            List<String> requestedHashes = new ArrayList<>();
            for (JsonNode node : payload) requestedHashes.add(node.asText());
    
            ArrayNode txBatch = mapper.createArrayNode();
            for (String hash : requestedHashes) {
                TX tx = MempoolService.getTransaction(hash);
                if (tx != null) {
                    txBatch.add(mapper.readTree(tx.createJSON()));
                }
            }
    
            ObjectNode response = mapper.createObjectNode();
            response.put("type", "txBatch");
            response.set("payload", txBatch);
    
            PrintWriter out = new PrintWriter(peer.getOutputStream(), true);
            out.println(mapper.writeValueAsString(response));
            System.out.println("📤 Sent TX batch with " + txBatch.size() + " TXs");
    
        } catch (Exception e) {
            System.err.println("❌ Failed to handle txRequestBatch:");
            e.printStackTrace();
        }
    }

    private void handleTxBatch(JsonNode payload) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            for (JsonNode node : payload) {
                TX tx = mapper.treeToValue(node, TX.class);
                if (!MempoolService.contains(tx.getTxHash())) {
                    MempoolService.addTransaction(tx.getTxHash(), tx.createJSON());
                    System.out.println("🔁 Recovered TX from peer: " + tx.getTxHash());
                }
            }
        } catch (Exception e) {
            System.err.println("❌ Failed to handle txBatch:");
            e.printStackTrace();
        }
    }

    private void sendHandshakeBack(Socket peer) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            ObjectNode handshake = mapper.createObjectNode();
            handshake.put("type", "handshake");
            handshake.put("address", portal.admin.address);
            handshake.put("blockHeight", blockchainDB.getHeight());
            handshake.put("requestSync", false); // already syncing the other way
            handshake.put("syncMode", "FULL");
            String nodeType = ConfigLoader.getNodeType();
            handshake.put("nodeType", nodeType);
            if(nodeType.equals("BEANNODE")){
                handshake.put("isValidator", true); 
            } else {
                handshake.put("isValidator", false);
            }
            handshake.put("isPublicNode", true); // optional if you're tracking this
            handshake.put("reply", true);
    
            PrintWriter out = new PrintWriter(peer.getOutputStream(), true);
            out.println(mapper.writeValueAsString(handshake));
    
            System.out.println("↩️ Sent handshake back to peer: " + peer.getInetAddress());
    
        } catch (Exception e) {
            System.err.println("❌ Failed to send handshake back to peer");
            e.printStackTrace();
        }
    }

}