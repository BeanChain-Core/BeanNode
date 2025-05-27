package com.beanchainbeta.validation;

import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.bean_core.Block.Block;
import com.bean_core.TXs.TX;
import com.bean_core.Utils.MetaHelper;
import com.bean_core.Utils.TXSorter;
import com.bean_core.crypto.WalletGenerator;
import com.beanchainbeta.network.Node;
import com.beanchainbeta.nodePortal.portal;
import com.beanchainbeta.services.Layer2DBService;
import com.beanchainbeta.services.MempoolService;
import com.beanchainbeta.services.RejectedService;
import com.beanchainbeta.services.WalletService;
import com.beanchainbeta.services.blockchainDB;
import com.fasterxml.jackson.databind.JsonNode;

public class BlockBuilderV2 {
    
    public static void blockMaker(String validatorKey) throws Exception {
        Map<String, Integer> simulatedL1 = new HashMap<>();
        Map<String, Integer> simulatedL2 = new HashMap<>();
        ArrayList<TX> accepted = new ArrayList<>();
        int blockSize = 0;
        int maxSize = 500_000;
    
        ArrayList<TX> mempool = MempoolService.getTxFromPool();
        TXSorter sorter = new TXSorter();
        sorter.sort(mempool);
        sorter.sortEachList();

        List<TX> txTransfer = sorter.getTransferTX();
        List<TX> txTokenTXs = sorter.getTokenTX();
        List<TX> txTokenCENTXs = sorter.getTokenCENTX();
        List<TX> txStakeTXs = sorter.getStakeTX();
        List<TX> txMintTXs = sorter.getMintTX();
        List<TX> txFundedCallTXs = sorter.getFundedCallTX();
    
        blockSize = processTXs(txTransfer, false, simulatedL1, accepted, blockSize, maxSize);
        blockSize = processTXs(txMintTXs, false, simulatedL1, accepted, blockSize, maxSize);
        blockSize = processTXs(txStakeTXs, false, simulatedL1, accepted, blockSize, maxSize);
        blockSize = processTXs(txTokenTXs, true, simulatedL2, accepted, blockSize, maxSize);
        blockSize = processTXs(txTokenCENTXs, true, simulatedL2, accepted, blockSize, maxSize);
        blockSize = processTXs(txFundedCallTXs, false, simulatedL1, accepted, blockSize, maxSize);
    
        List<String> acceptedHashs = new ArrayList<>();
        long gasReward = 0;
        for(TX tx : accepted) {
            acceptedHashs.add(tx.getTxHash());
            gasReward += tx.getGasFee();
        }

        //WalletService.creditGaspoolFromBlock(gasReward);


        Block block = new Block(blockchainDB.getHeight() + 1, blockchainDB.getLatestBlock().getHash(), acceptedHashs, validatorKey);
        block.setHeight(blockchainDB.getHeight() + 1);
        block.setTimeStamp(System.currentTimeMillis());
        PrivateKey privateKey = WalletGenerator.restorePrivateKey(validatorKey);
        block.initHeader(gasReward);
        block.sign(privateKey);

        System.out.println("NEW BLOCK: " + block.getHash() + "Params: Height: " + block.getHeight()+ " PrevHash: " + block.getPreviousHash() + "MerkleRoot: " + block.getMerkleRoot());
    
        blockchainDB.storeNewBlock(block);
        Node.broadcastBlock(block);
    
        System.out.println("Block #" + block.getHeight() + " built with " + accepted.size() + " TXs, size = " + blockSize + " bytes");
    }

    private static int processTXs(List<TX> txList, boolean isLayer2,
        Map<String, Integer> nonceMap, ArrayList<TX> accepted,
        int blockSize, int maxSize) throws Exception {

        for (TX tx : txList) {

            JsonNode metaNode;
            if(tx.getMeta() != null) {
                metaNode = MetaHelper.getMetaNode(tx);
            } else {
                metaNode = null;
            }
            boolean isAirdrop = tx.getType().equals("airdrop");

            String sender = null;
            int actualNonce = -1;
            int expectedNonce = -1;

            if (isAirdrop) {
                // Skip nonce validation for airdrops
            } else {
                sender = isLayer2
                    ? (metaNode != null && metaNode.has("caller") && metaNode.get("caller") != null
                        ? metaNode.get("caller").asText()
                        : tx.getFrom())
                    : tx.getFrom();

                actualNonce = isLayer2
                    ? (metaNode.has("callerLayer2Nonce") ? metaNode.get("callerLayer2Nonce").asInt() : tx.getNonce())
                    : tx.getNonce();

                expectedNonce = nonceMap.getOrDefault(
                    sender,
                    isLayer2 ? Layer2DBService.getLayer2Nonce(sender) : WalletService.getNonce(sender)
                );

                if (actualNonce != expectedNonce) {
                    System.out.println("Nonce mismatch: " + tx.getTxHash() + " actual=" + actualNonce + " expected=" + expectedNonce + " sender=" + sender);
                    tx.setStatus("rejected");
                    System.out.print("TX REJECTED NOT VALID: " + tx.getTxHash());
                    Node.broadcastRejection(tx.getTxHash());
                    RejectedService.saveRejectedTransaction(tx);
                    MempoolService.removeSingleTx(tx.getTxHash());
                    continue;
                }
            }

            boolean isValid = false;

            switch (tx.getType()) {
                case "transfer":
                case "stake":
                case "airdrop":
                case "cen":
                    isValid = TXVerifier.verifyTransaction(tx);
                    break;

                case "mint":
                    isValid = MintVerifier.verifyTransaction(tx);
                    break;

                case "token":
                    if (metaNode != null && metaNode.has("isCEN")) {
                        isValid = TokenCENTXVerifier.verifyTransaction(tx);
                    } else {
                        isValid = TokenTXVerifier.verifyTransaction(tx);
                    }
                    break;

                default:
                    System.err.println("Unknown TX type or missing verifier: " + tx.getType());
            }

            if (!isValid) {
                tx.setStatus("rejected");
                System.out.print("TX REJECTED NOT VALID: " + tx.getTxHash());
                Node.broadcastRejection(tx.getTxHash());
                RejectedService.saveRejectedTransaction(tx);
                MempoolService.removeSingleTx(tx.getTxHash());
                continue;
            }

            int txSize = tx.createJSON().getBytes(StandardCharsets.UTF_8).length;
            if (blockSize + txSize > maxSize) break;

            if (!TXExecutor.execute(tx)) {
                tx.setStatus("rejected");
                System.out.print("TX REJECTED NOT EXECUTED: " + tx.getTxHash());
                Node.broadcastRejection(tx.getTxHash());
                RejectedService.saveRejectedTransaction(tx);
                MempoolService.removeSingleTx(tx.getTxHash());
                continue;
            } else {
                tx.setStatus("complete");
                System.out.print("COMPLETED: " + tx.getTxHash());
                portal.beanchainTest.storeTX(tx);
                MempoolService.removeSingleTx(tx.getTxHash());
                accepted.add(tx);
            }

            if (!isAirdrop) { 
                nonceMap.put(sender, expectedNonce + 1);
            }

            blockSize += txSize;
        }

        return blockSize;
    }


    public static void blockReplay(Block newBlock) throws Exception {
        Map<String, Integer> simulatedL1 = new HashMap<>();
        Map<String, Integer> simulatedL2 = new HashMap<>();
        List<String> txHashList = newBlock.getTransactions();
        List<TX> mempoolReplay = new ArrayList<>();

        for(String txHash : txHashList) {
            TX tx = MempoolService.getTransaction(txHash);
            mempoolReplay.add(tx);
        }

        
        ArrayList<TX> accepted = new ArrayList<>();
        int blockSize = 0;
        int maxSize = 500_000;
    
        
        TXSorter sorter = new TXSorter();
        sorter.sort(mempoolReplay);
        sorter.sortEachList();

        List<TX> txTransfer = sorter.getTransferTX();
        List<TX> txTokenTXs = sorter.getTokenTX();
        List<TX> txTokenCENTXs = sorter.getTokenCENTX();
        List<TX> txStakeTXs = sorter.getStakeTX();
        List<TX> txMintTXs = sorter.getMintTX();
        List<TX> txFundedCallTXs = sorter.getFundedCallTX();
    
        blockSize = processTXs(txTransfer, false, simulatedL1, accepted, blockSize, maxSize);
        blockSize = processTXs(txMintTXs, false, simulatedL1, accepted, blockSize, maxSize);
        blockSize = processTXs(txStakeTXs, false, simulatedL1, accepted, blockSize, maxSize);
        blockSize = processTXs(txTokenTXs, true, simulatedL2, accepted, blockSize, maxSize);
        blockSize = processTXs(txTokenCENTXs, true, simulatedL2, accepted, blockSize, maxSize);
        blockSize = processTXs(txFundedCallTXs, false, simulatedL1, accepted, blockSize, maxSize);
    
        List<String> acceptedHashs = new ArrayList<>();
        long gasReward = 0;
        for(TX tx : accepted) {
            acceptedHashs.add(tx.getTxHash());
            gasReward += tx.getGasFee();
        }

        //WalletService.creditGaspoolFromBlock(gasReward);
        if (newBlock.getHeader() == null) {
            newBlock.initHeader(gasReward);
        }


        if (!newBlock.validateBlock(blockchainDB.getLatestBlock().getHash())) {
            System.err.println("❌ Replayed block #" + newBlock.getHeight() + " failed validation.");
            return;
        }

        System.out.println("NEW BLOCK: " + newBlock.getHash() + "Params: Height: " + newBlock.getHeight()+ " PrevHash: " + newBlock.getPreviousHash() + "MerkleRoot: " + newBlock.getMerkleRoot());

        blockchainDB.storeNewBlock(newBlock);
    
        System.out.println("Block #" + newBlock.getHeight() + " rebuilt with " + accepted.size() + " TXs, size = " + blockSize + " bytes");
    }
 
}
