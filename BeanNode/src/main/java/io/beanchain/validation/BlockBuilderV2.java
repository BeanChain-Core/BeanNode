package io.beanchain.validation;

import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.beanpack.Block.Block;
import com.beanpack.Rejection.Flagger;
import com.beanpack.TXs.TX;
import com.beanpack.Utils.MetaHelper;
import com.beanpack.Utils.TXSorter;
import com.beanpack.crypto.WalletGenerator;
import io.beanchain.logger.BeanLoggerManager;
import io.beanchain.network.Node;
import io.beanchain.nodePortal.portal;
import io.beanchain.services.Layer2DBService;
import io.beanchain.services.MempoolService;
import io.beanchain.services.RejectedService;
import io.beanchain.services.WalletService;
import io.beanchain.services.blockchainDB;
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
        
        List<TX> rejectedTXs = sorter.getRejectedTX();

        for(TX tx: rejectedTXs){
            tx.setStatus("rejected");
            System.out.print("TX REJECTED NOT VALID (INVALID 'TYPE'): " + tx.getTxHash());
            
            try {
                Flagger.repackRejection(tx, "Invalid TX Type Field");
                RejectedService.saveRejectedTransaction(tx);
            } catch (Exception e) {
                BeanLoggerManager.BeanLoggerError("Failed to flag/save rejected TX: " + tx.getTxHash());
                e.printStackTrace();
            }
            MempoolService.removeSingleTx(tx.getTxHash());
            Node.broadcastRejection(tx.getTxHash(), tx);
        }
    
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

        BeanLoggerManager.BeanPrinter("NEW BLOCK: " + block.getHash() + " Params: Height: " + block.getHeight()+ " PrevHash: " + block.getPreviousHash() + " MerkleRoot: " + block.getMerkleRoot());
    
        blockchainDB.storeNewBlock(block);
        Node.broadcastBlock(block, null);
    
        BeanLoggerManager.BeanLogger("Block #" + block.getHeight() + " built with " + accepted.size() + " TXs, size = " + blockSize + " bytes");
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
                    BeanLoggerManager.BeanLoggerError("Nonce mismatch: " + tx.getTxHash() + " actual=" + actualNonce + " expected=" + expectedNonce + " sender=" + sender);
                    tx.setStatus("rejected");
                    BeanLoggerManager.BeanLoggerError("TX REJECTED NOT VALID: " + tx.getTxHash());
                    
                    try {
                        Flagger.repackRejection(tx, "Information mixmatch or credential failure.");
                        RejectedService.saveRejectedTransaction(tx);
                    } catch (Exception e) {
                        BeanLoggerManager.BeanLoggerError("Failed to flag/save rejected TX: " + tx.getTxHash());
                        e.printStackTrace();
                    }
                    MempoolService.removeSingleTx(tx.getTxHash());
                    Node.broadcastRejection(tx.getTxHash(), tx);
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
                
                try {
                        Flagger.repackRejection(tx, "INVALID SIGNATURE");
                        RejectedService.saveRejectedTransaction(tx);
                    } catch (Exception e) {
                        BeanLoggerManager.BeanLoggerError("Failed to flag/save rejected TX: " + tx.getTxHash());
                        e.printStackTrace();
                    }
                MempoolService.removeSingleTx(tx.getTxHash());
                Node.broadcastRejection(tx.getTxHash(), tx);
                continue;
            }

            int txSize = tx.createJSON().getBytes(StandardCharsets.UTF_8).length;
            if (blockSize + txSize > maxSize) break;

            if (!TXExecutor.execute(tx)) {
                tx.setStatus("rejected");
                BeanLoggerManager.BeanLoggerError("TX REJECTED NOT EXECUTED: " + tx.getTxHash());
                //TODO:LOG MORE REJECTIONS
                try {
                        Flagger.repackRejection(tx, "TX failed during Execution");
                        RejectedService.saveRejectedTransaction(tx);
                    } catch (Exception e) {
                        BeanLoggerManager.BeanLoggerError("Failed to flag/save rejected TX: " + tx.getTxHash());
                        e.printStackTrace();
                    }
                MempoolService.removeSingleTx(tx.getTxHash());
                Node.broadcastRejection(tx.getTxHash(), tx);
                continue;
            } else {
                tx.setStatus("complete");
                BeanLoggerManager.BeanLogger("COMPLETED: " + tx.getTxHash());
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

    private static int processReplayTXs(List<TX> txList, boolean isLayer2,
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
                    BeanLoggerManager.BeanLoggerError("Nonce mismatch: " + tx.getTxHash() + " actual=" + actualNonce + " expected=" + expectedNonce + " sender=" + sender);
                    tx.setStatus("rejected");
                    BeanLoggerManager.BeanLoggerError("TX REJECTED NOT VALID: " + tx.getTxHash());
                    
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
                MempoolService.removeSingleTx(tx.getTxHash());
                continue;
            }

            int txSize = tx.createJSON().getBytes(StandardCharsets.UTF_8).length;
            if (blockSize + txSize > maxSize) break;

            if (!TXExecutor.execute(tx)) {
                MempoolService.removeSingleTx(tx.getTxHash());
                continue;
            } else {
                tx.setStatus("complete");
                BeanLoggerManager.BeanLogger("COMPLETED: " + tx.getTxHash());
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


    private static boolean replayVerifyTXs(List<TX> txList, boolean isLayer2,
            Map<String, Integer> nonceMap) throws Exception {

        for (TX tx : txList) {
            JsonNode metaNode = (tx.getMeta() != null) ? MetaHelper.getMetaNode(tx) : null;
            boolean isAirdrop = "airdrop".equals(tx.getType());

            String sender = null;
            int actualNonce = -1;
            int expectedNonce = -1;

            if (!isAirdrop) {
                sender = isLayer2
                    ? (metaNode != null && metaNode.has("caller") && metaNode.get("caller") != null
                        ? metaNode.get("caller").asText()
                        : tx.getFrom())
                    : tx.getFrom();

                actualNonce = isLayer2
                    ? (metaNode.has("callerLayer2Nonce") ? metaNode.get("callerLayer2Nonce").asInt() : tx.getNonce())
                    : tx.getNonce();

                expectedNonce = nonceMap.containsKey(sender)
                    ? nonceMap.get(sender)
                    : (isLayer2 ? Layer2DBService.getLayer2Nonce(sender) : WalletService.getNonce(sender));

                if (actualNonce != expectedNonce) {
                    BeanLoggerManager.BeanLoggerError("Replay reject: Nonce mismatch for TX " + tx.getTxHash());
                    return false;
                }
            }

            boolean isValid = switch (tx.getType()) {
                case "transfer", "stake", "airdrop", "cen" -> TXVerifier.verifyTransaction(tx);
                case "mint" -> MintVerifier.verifyTransaction(tx);
                case "token" -> {
                    if (metaNode != null && metaNode.has("isCEN")) {
                        yield TokenCENTXVerifier.verifyTransaction(tx);
                    } else {
                        yield TokenTXVerifier.verifyTransaction(tx);
                    }
                }
                default -> {
                    BeanLoggerManager.BeanLoggerError("Unknown TX type during replay: " + tx.getType());
                    yield false;
                }
            };

            if (!isValid) {
                BeanLoggerManager.BeanLoggerError("Replay reject: Invalid TX " + tx.getTxHash());
                return false;
            }

            // Update nonceMap simulation
            if (!isAirdrop && sender != null) {
                nonceMap.put(sender, expectedNonce + 1);
            }

            
        }

        return true; // All passed
    }


    public static boolean blockReplay(Block newBlock) throws Exception {
        Map<String, Integer> verifyL1 = new HashMap<>();
        Map<String, Integer> verifyL2 = new HashMap<>();
        Map<String, Integer> execL1 = new HashMap<>();
        Map<String, Integer> execL2 = new HashMap<>();
        List<String> txHashList = newBlock.getTransactions();
        List<TX> mempoolReplay = new ArrayList<>();

        for(String txHash : txHashList) {
            TX tx = MempoolService.getTransaction(txHash);
            if (tx == null) {
                BeanLoggerManager.BeanLoggerError("[TXSorter] Null TX found in list!");
                continue;
            }
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

        boolean transfersPass = replayVerifyTXs(txTransfer, false, verifyL1);
        boolean mintPass = replayVerifyTXs(txMintTXs, false, verifyL1);
        boolean stakePass = replayVerifyTXs(txStakeTXs, false, verifyL1);
        boolean tokenPass= replayVerifyTXs(txTokenTXs, true, verifyL2);
        boolean tokenCENPass = replayVerifyTXs(txTokenCENTXs, true, verifyL2);
        boolean fundedCenCallPass = replayVerifyTXs(txFundedCallTXs, false, verifyL1);

        if(!transfersPass || !mintPass || !stakePass || !tokenPass || !tokenCENPass || !fundedCenCallPass){
            BeanLoggerManager.BeanLoggerError("Replayed block #" + newBlock.getHeight() + " failed validation. Invalid TX contained. Waiting for valid block at this height");
            return false;
        }
    
        blockSize = processReplayTXs(txTransfer, false, execL1, accepted, blockSize, maxSize);
        blockSize = processReplayTXs(txMintTXs, false, execL1, accepted, blockSize, maxSize);
        blockSize = processReplayTXs(txStakeTXs, false, execL1, accepted, blockSize, maxSize);
        blockSize = processReplayTXs(txTokenTXs, true, execL2, accepted, blockSize, maxSize);
        blockSize = processReplayTXs(txTokenCENTXs, true, execL2, accepted, blockSize, maxSize);
        blockSize = processReplayTXs(txFundedCallTXs, false, execL1, accepted, blockSize, maxSize);
    
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
            BeanLoggerManager.BeanLoggerError("Replayed block #" + newBlock.getHeight() + " failed validation.");
            return false;
        }

        if (newBlock.getHeight() % 500 == 0) {
            BeanLoggerManager.BeanLoggerFPrint("Sync Progress: Current Sync Height " + newBlock.getHeight());
        }

        BeanLoggerManager.BeanPrinter("NEW BLOCK: " + newBlock.getHash() + "Params: Height: " + newBlock.getHeight()+ " PrevHash: " + newBlock.getPreviousHash() + "MerkleRoot: " + newBlock.getMerkleRoot());

        blockchainDB.storeNewBlock(newBlock);
    
        BeanLoggerManager.BeanLogger("Block #" + newBlock.getHeight() + " rebuilt with " + accepted.size() + " TXs, size = " + blockSize + " bytes");
        return true;
    }
 
}
