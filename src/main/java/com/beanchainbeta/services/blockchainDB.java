package com.beanchainbeta.services;

import org.iq80.leveldb.*;

import com.bean_core.Block.*;
import com.beanchainbeta.config.ConfigLoader;
import com.beanchainbeta.controllers.DBManager;
import com.beanchainbeta.genesis.GenesisTX;
import com.bean_core.TXs.*;
import java.nio.charset.StandardCharsets;
import static org.iq80.leveldb.impl.Iq80DBFactory.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;


public class blockchainDB {
    public static DB db;
    public static int currentHeight;
    public static String previousHash;
    ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);



    public blockchainDB() {
        blockchainDB.db = DBManager.getDB(ConfigLoader.getChainDB()); 

        try {
            if (!checkGenBlock()) {
                loadGenBlock();
            }
        } catch (Exception e) {
            System.err.println("Error initializing blockchainDB: " + e.getMessage());
        }
    }

    private void storeBlock(Block block) {
        try {
            db.put(bytes("block-0"), bytes(block.createJSON()));
            //System.out.println("Genesis block persisted to DB.");
        } catch (Exception e) {
            System.err.println(e);
        }
    }

    public static void storeNewBlock(Block block) throws Exception {
        //System.out.println("CHECK****");
        if(block.signatureValid()) {
            try{
                String key = "block-" + block.getHeight();
                db.put(bytes(key), bytes(block.createJSON()));
                String blockLog = 
                "{NEW-BLOCK}{" + block.getHeight() + "}\n" +
                block.createJSON(); 
                System.out.println(blockLog);
            } catch (Exception e) {
                System.err.println(e);
            }
        } else {
            System.out.println("ERROR INVALID BLOCK");
        }
    }


    private boolean checkGenBlock() {
        byte[] flag = db.get(bytes("genesis-created"));
        byte[] genBlock = db.get(bytes("block-0"));
    
        return flag != null && asString(flag).equals("true") && genBlock != null;
    }

    public static Boolean checkBlock() {
        byte[] data = db.get(bytes("block-1"));
        return data != null;

    }

    public String getData(String key) {
        return asString(db.get(bytes(key)));
    }

    public static Block getBlockByHeight(int height) {
        try {
            String key = "block-" + height;
            byte[] data = db.get(bytes(key));
            if (data != null) {
                String json = asString(data);
                return Block.fromJSON(json);
            } else {
                System.err.println("⚠️ No block found at height: " + height);
            }
        } catch (Exception e) {
            System.err.println("❌ Failed to load block at height " + height);
            e.printStackTrace();
        }
        return null;
    }
    

    

    public void storeTX(TX TX) {
        try {
            db.put(bytes("tran-" + TX.getTxHash()), bytes(TX.createJSON()));
        } catch (Exception e) {
            System.err.println(e);
        }
    }

    public void closeDB() {
        try {
            db.close();
        } catch (Exception e) {
            System.err.println(e);
        }
    }

    private void loadGenBlock() throws Exception{
        GenesisTX genTX1 = new GenesisTX("BEANX:0xFAUCETWALLET", 5000000); //handled by RN
        GenesisTX genTX2 = new GenesisTX("BEANX:0xEARLYWALLET", 5000000);  //handled by RN
        GenesisTX genTX3 = new GenesisTX("BEANX:0xSTAKEREWARD", 15000000); //need funds released programatically
        GenesisTX genTX4 = new GenesisTX("BEANX:0xNODEREWARD", 30000000); //handled by RN 
        GenesisTX genTX5 = new GenesisTX("BEANX:0x1c8496175b3f4802e395db5fab4dd66e09c431b2", 2500000); // needs to be sent to wallet in chunks over time to avoid rug pull
        GenesisTX genTX6 = new GenesisTX("BEANX:0xLIQUIDITY", 12500000); // held by the team promised to be used for liquidity or aborted and transfered to rewards based on future vote

        WalletService walletService = new WalletService();
        WalletService.genBlock();
        
        List<String> genesisTransactions = Arrays.asList(
            genTX1.getTxHash(), genTX2.getTxHash(), genTX3.getTxHash(), genTX4.getTxHash(), genTX5.getTxHash(), genTX6.getTxHash()
        );
        Collections.sort(genesisTransactions);

        String genPrivateKey = "d9c3a43797fded25aa2e38a2e59b70c35a6f62c5378a2078b8a3f84bd0860a3c"; // HARD CODED GEN KEY! - TODO: BLOCK THIS KEY FROM USE IN NETWORK OTHER THAN GENESIS

        Block genesisBlock = new Block(0, "00000000000000000000", genesisTransactions, genPrivateKey);
        genesisBlock.initHeader(0L);
        //System.out.println("📦 Bootstrap Genesis Hash: " + genesisBlock.getHash());

        


        storeBlock(genesisBlock);
        System.out.println("GEN BLOCK HASH: " + genesisBlock.getHash() + "Params: Height: " + genesisBlock.getHeight()+ " PrevHash: " + genesisBlock.getPreviousHash() + "MerkleRoot: " + genesisBlock.getMerkleRoot());
        genPrivateKey = "";
        List<TX> confirmed = Arrays.asList(genTX1, genTX2, genTX3, genTX4, genTX5, genTX6);

        for(TX t: confirmed){
            storeTX(t);
        }

        db.put(bytes("genesis-created"), bytes("true"));

    }

    public List<Block> getAllBlocks() {
    List<Block> blocks = new ArrayList<>();

    try (DBIterator iterator = db.iterator()) {
        iterator.seekToFirst();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            String key = asString(entry.getKey());

            if (key.startsWith("block-")) {
                String json = asString(entry.getValue());
                //System.out.print(key);
                blocks.add(Block.fromJSON(json)); 
            }
        }
    } catch (Exception e) {
        e.printStackTrace();
    }

    return blocks;
    }

    public List<TX> getAllTransactions() {
        List<TX> txs = new ArrayList<>();

        try (DBIterator iterator = db.iterator()) {
            iterator.seekToFirst();
            while (iterator.hasNext()) {
                var entry = iterator.next();
                String key = asString(entry.getKey());

                if (key.startsWith("tran-")) {
                    String json = asString(entry.getValue());
                    //System.out.println(json);
                    txs.add(TX.fromJSON(json)); 
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return txs;
    }

    public static List<TX> getWalletCompleteTXs(String addy) {
        List<TX> txs = new ArrayList<>();

        try (DBIterator iterator = db.iterator()) {
            iterator.seekToFirst();
            while (iterator.hasNext()) {
                var entry = iterator.next();
                String key = asString(entry.getKey());

                if (key.startsWith("tran-")) {
                    String json = asString(entry.getValue());
                    //System.out.println(json);
                    TX tx = TX.fromJSON(json); 
                    if(tx.getFrom().equals(addy)) {
                        txs.add(tx);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        txs.sort((a, b) -> Long.compare(b.getTimeStamp(), a.getTimeStamp()));
        return txs;
    }

    public static List<TX> getWalletInTXs(String addy) {
        List<TX> txs = new ArrayList<>();

        try (DBIterator iterator = db.iterator()) {
            iterator.seekToFirst();
            while (iterator.hasNext()) {
                var entry = iterator.next();
                String key = asString(entry.getKey());

                if (key.startsWith("tran-")) {
                    String json = asString(entry.getValue());
                    //System.out.println(json);
                    TX tx = TX.fromJSON(json); 
                    if(tx.getTo().equals(addy)) {
                        txs.add(tx);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        txs.sort((a, b) -> Long.compare(b.getTimeStamp(), a.getTimeStamp()));
        return txs;
    }


    public static BlockInfo getNextBlockInfo() {
        Block latestBlock = getLatestBlock();

        if (latestBlock == null) {
            // Default to genesis block values if no blocks exist
            return new BlockInfo("00000000000000000000", 0);
        }

        return new BlockInfo(latestBlock.getHash(), latestBlock.getHeight() + 1);
    }

    // Method to fetch the latest block from LevelDB
    public static Block getLatestBlock() {
        Block latestBlock = null;
        int highestHeight = -1;
    
        try (DBIterator iterator = db.iterator()) {
            iterator.seekToFirst();
            while (iterator.hasNext()) {
                var entry = iterator.next();
                String key = asString(entry.getKey());
    
                if (key.startsWith("block-")) {
                    String json = asString(entry.getValue());
                    Block block = Block.fromJSON(json);
    
                    if (block != null) { // << ADDED SAFEGUARD
                        if (block.getHeader() == null) { // << ADDED: missing header fix
                            System.err.println("⚠️ WARNING: Missing header detected on block height " + block.getHeight() + ", injecting default header...");
                            BlockHeader safeHeader = new BlockHeader();
                            safeHeader.setValidator(block.getValidatorPubKey());
                            safeHeader.setHeight(block.getHeight());
                            safeHeader.setPreviousHash(block.getPreviousHash());
                            safeHeader.setGasFeeReward(0L);
                            block.setHeader(safeHeader);
                        }
    
                        if (block.getHeight() > highestHeight) {
                            highestHeight = block.getHeight();
                            latestBlock = block;
                        }
                    } else {
                        System.err.println("❌ Failed to parse block JSON at key: " + key);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    
        return latestBlock;
    }

    // Helper class to return both previousHash and blockHeight
    public static class BlockInfo {
        private static String previousHash;
        private static int blockHeight;

        public BlockInfo(String previousHash, int blockHeight) {
            this.previousHash = previousHash;
            this.blockHeight = blockHeight;
        }

        public String getPreviousHash() {
            return previousHash;
        }

        public int getBlockHeight() {
            return blockHeight;
        }
    }

    public static int getHeight() {
    int maxHeight = -1;

    try (DBIterator iterator = db.iterator()) {
        for (iterator.seekToFirst(); iterator.hasNext(); iterator.next()) {
            String key = new String(iterator.peekNext().getKey(), StandardCharsets.UTF_8);

            if (key.startsWith("block-")) {
                try {
                    int height = Integer.parseInt(key.substring(6)); // "block-" is 6 chars
                    if (height > maxHeight) {
                        maxHeight = height;
                    }
                } catch (NumberFormatException e) {
                    System.err.println("Invalid block key: " + key);
                }
            }
        }
    } catch (Exception e) {
        e.printStackTrace();
    }

    return maxHeight;
    }

    public static List<String> getTransactionsByHashes(List<String> hashes) {
        List<String> result = new ArrayList<>();
    
        for (String hash : hashes) {
            try {
                String key = "tran-" + hash;
                byte[] data = db.get(bytes(key));
                if (data != null) {
                    result.add(asString(data)); // JSON string from DB
                } else {
                    System.err.println("⚠️ TX not found in DB for hash: " + hash);
                }
            } catch (Exception e) {
                System.err.println("❌ Error fetching TX from DB: " + hash);
                e.printStackTrace();
            }
        }
    
        return result;
    }

    //dev explorer 

    public byte[] getRaw(String key) {
        try {
            return blockchainDB.db.get(key.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            System.err.println("❌ Failed to get raw key from DB: " + key);
            return null;
        }
    }
    
}

