package com.beanchainbeta.controllers;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;

import com.beanchainbeta.network.Node;
import com.beanchainbeta.services.Layer2DBService;
import com.beanchainbeta.services.MempoolService;
import com.beanchainbeta.services.RejectedService;
import com.beanchainbeta.services.WalletService;
import com.beanchainbeta.services.blockchainDB;
import com.beanpack.*;
import com.beanpack.CENdev.CallManager;
import com.beanpack.Models.Layer2Wallet;
import com.beanpack.Models.TokenStorage;
import com.beanpack.TXs.CENCALL;
import com.beanpack.TXs.TX;
import com.beanpack.Utils.beantoshinomics;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.tinylog.Logger;


//@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api")
public class DBController {
    private final blockchainDB db;
    private final MempoolService mempoolService;
    private final WalletService walletService;

    public DBController(blockchainDB db, MempoolService mempool, WalletService walletService){
        this.db = db;
        this.mempoolService = mempool;
        this.walletService = walletService;
    }

    @PostMapping("/submit-transaction")
    public ResponseEntity<?> submitTransaction(@RequestBody Map<String, String> request) {
        String txHash = request.get("txHash");
        String transactionJson = request.get("transactionJson");


        if (txHash == null || transactionJson == null) {
            return ResponseEntity.badRequest().body("{\"status\": \"error\", \"message\": \"Missing txHash or transactionJson\"}");
        }

        TX tx;
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            tx = objectMapper.readValue(transactionJson, TX.class);
        } catch (Exception e) {
            Logger.error(e);
            //e.printStackTrace();
            return ResponseEntity.badRequest().body("{\"status\": \"error\", \"message\": \"Invalid transaction JSON\"}");
        }

        if (!beantoshinomics.isValidAmount(String.valueOf(tx.getAmount()))) {
            return ResponseEntity.badRequest().body("{\"status\": \"error\", \"message\": \"Invalid amount: must be a valid multiple of 0.000001 BEAN and at least 1 beantoshi\"}");
        }

        if (mempoolService.addTransaction(txHash, transactionJson)) {
            Node.broadcastTransactionStatic(tx);
            return ResponseEntity.ok("{\"status\": \"success\", \"txHash\": \"" + txHash + "\"}");
        } else {
            return ResponseEntity.badRequest().body("{\"status\": \"error\", \"message\": \"Transaction rejected or already exists\"}");
        }
    }

    @PostMapping("/CENCALL")
    public ResponseEntity<?> sendCENCALL(@RequestBody Map<String, String> request) throws Exception {
        String callHash = request.get("cenHash");
        String cencallJson = request.get("callJson");

        if (callHash == null || cencallJson == null) {
            return ResponseEntity.badRequest().body("{\"status\": \"error\", \"message\": \"Missing cenHash or callJson\"}");
        }

        CallManager manager = new CallManager(cencallJson);

        if (!manager.isSignatureValid()) {
            return ResponseEntity.badRequest().body("{\"status\": \"error\", \"message\": \"Invalid CENCALL signature\"}");
        }

        String cenIp = manager.getCENIP();
        ObjectMapper mapper = new ObjectMapper();
        CENCALL call = mapper.readValue(cencallJson, CENCALL.class);

        Node.broadcastCENCALL(cenIp, call);

        return ResponseEntity.ok("{\"status\": \"success\", \"cenHash\": \"" + callHash + "\"}");
    }


    @PostMapping("/balance")
    public ResponseEntity<?> getBalance(@RequestBody Map<String, String> request) {
        String address = request.get("address");

        if (address == null || address.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                "status", "error",
                "message", "Missing or empty address"
            ));
        }

        try {
            
            double balance = WalletService.getBeanBalance(address);
            return ResponseEntity.ok(Map.of(
                "status", "success",
                "address", address,
                "balance", balance
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "status", "error",
                "message", "Failed to retrieve balance",
                "details", e.getMessage()
            ));
        }
    }

    @PostMapping("/label")
    public ResponseEntity<String> updateLabel(@RequestBody Map<String, String> payload) {
        String address = payload.get("address");
        String label = payload.get("label");
        String signature = payload.get("signature");
        String publicKeyHex = payload.get("publicKeyHex");

        try {
            boolean success = WalletService.updateWalletLabel(address, label, signature, publicKeyHex);
            if (success) {
                return ResponseEntity.ok("Label updated successfully.");
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid label update request.");
            }
        } catch (SecurityException se) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Signature or address verification failed.");
        } catch (IllegalArgumentException ie) {
            return ResponseEntity.badRequest().body("Invalid label format.");
        } catch (Exception e) {
            Logger.error(e);
            //e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to update label.");
        }
    }

    //explorer posts

    @PostMapping("/block")
    public ResponseEntity<?> getBlockByHeight(@RequestBody Map<String, Integer> request) {
        Integer height = request.get("height");
        if (height == null) {
            return ResponseEntity.badRequest().body("Missing 'height'");
        }

        try {
            String key = "block-" + height;
            byte[] data = db.getRaw(key);
            if (data == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Block not found.");
            }

            String json = new String(data, StandardCharsets.UTF_8);
            return ResponseEntity.ok(json);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error fetching block");
        }
    }

    @PostMapping("/transaction")
    public ResponseEntity<?> getTransactionByHash(@RequestBody Map<String, String> request) {
        String txHash = request.get("txHash");
        if (txHash == null || txHash.isEmpty()) {
            return ResponseEntity.badRequest().body("Missing 'txHash'");
        }

        try {
            String key = "tran-" + txHash;
            byte[] data = db.getRaw(key);
            if (data == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Transaction not found.");
            }

            String json = new String(data, StandardCharsets.UTF_8);
            return ResponseEntity.ok(json);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error fetching transaction");
        }
    }

    @PostMapping("/chain-info")
    public ResponseEntity<?> getChainInfo() {
        try {
            int height = blockchainDB.getHeight();
            return ResponseEntity.ok(Map.of("height", height));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error fetching height");
        }
    }

    @PostMapping("/layer2/tokens")
    public ResponseEntity<?> getTokensForWallet(@RequestBody Map<String, String> payload) {
        try {
            String address = payload.get("address");
            if (address == null || address.isEmpty()) {
                return ResponseEntity.badRequest().body("Missing address");
            }

            Layer2Wallet wallet = Layer2DBService.loadWallet(address);
            if (wallet == null) {
                return ResponseEntity.status(404).body("Wallet not found");
            }

            List<Map<String, Object>> result = new ArrayList<>();

            for (String tokenHash : wallet.getTokenBalances().keySet()) {
                TokenStorage token = Layer2DBService.loadToken(tokenHash);
                if (token == null) continue;

                JsonNode meta = token.getTokenMetaAsJson();

                Map<String, Object> entry = new HashMap<>();
                entry.put("tokenHash", tokenHash);
                entry.put("balance", wallet.getBalance(tokenHash));
                entry.put("token", meta.has("token") ? meta.get("token").asText() : "Unknown");
                entry.put("symbol", meta.has("symbol") ? meta.get("symbol").asText() : "???");
                entry.put("capped", !meta.has("mintable") || !meta.get("mintable").asBoolean());
                entry.put("openMint", meta.has("open") && meta.get("open").asBoolean());

                result.add(entry);
            }

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            Logger.error(e,"Error in /layer2/tokens");
            //System.err.println("Error in /layer2/tokens: " + e.getMessage());
            return ResponseEntity.status(500).body("Internal server error");
        }
    }

    @PostMapping("/layer2nonce")
    public ResponseEntity<Integer> getLayer2Nonce(@RequestBody Map<String, String> payload) {
        try {
            String address = payload.get("address");
            if (address == null || address.isEmpty()) {
                return ResponseEntity.badRequest().body(-1);
            }
            int layer2Nonce = Layer2DBService.getLayer2Nonce(address);
            return ResponseEntity.ok(layer2Nonce);
        } catch (Exception e) {
            Logger.error(e,"Error fetching Layer2 nonce");
            //System.err.println("❌ Error fetching Layer2 nonce.");
            //e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(-1);
        }
    }

    @PostMapping("/token-details")
    public ResponseEntity<?> getTokenDetails(@RequestBody Map<String, String> payload) {
        try {
            String tokenHash = payload.get("tokenHash");
            if (tokenHash == null || tokenHash.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Missing tokenHash"));
            }

            if (!Layer2DBService.tokenExists(tokenHash)) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Token not found"));
            }

            TokenStorage tokenInfo = Layer2DBService.loadToken(tokenHash);
            if (tokenInfo == null) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Failed to load token data"));
            }

            return ResponseEntity.ok(tokenInfo);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Unexpected error"));
        }
    }





//GET

    @GetMapping("/mempool")
    public ResponseEntity<?> getMempool() {
        return ResponseEntity.ok(mempoolService.getTransactions());
    }

    @GetMapping("/rejected/{address}")
    public ResponseEntity<?> getRejected(@PathVariable String address) {
        try {
            Map<String, String> rejected = RejectedService.getRejectedTxsForAddress(address);

            return ResponseEntity
                .ok()
                .contentType(APPLICATION_JSON)
                .body(rejected);
        } catch (Exception e) {
            System.err.println("ERROR FETCHING REJECTED TXs: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                .body("Failed to fetch rejected transactions.");
        }
    }

    @GetMapping("/nonce/{address}")
    public ResponseEntity<?> getNonce(@PathVariable String address) {
        try {
            int nonce = WalletService.getNonce(address); 
            return ResponseEntity.ok(Collections.singletonMap("nonce", nonce));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                .body(Collections.singletonMap("error", e.getMessage()));
        }
    }

    @GetMapping("/txs/sent/{address}")
    public ResponseEntity<List<TX>> getSentTxs(@PathVariable String address) {
        List<TX> sentTxs = blockchainDB.getWalletCompleteTXs(address);
        List<TX> filtered = sentTxs.stream()
            .filter(tx -> "complete".equals(tx.getStatus()))
            .toList();
        return ResponseEntity.ok(filtered);
    }

    @GetMapping("/txs/received/{address}")
    public ResponseEntity<List<TX>> getReceivedTxs(@PathVariable String address) {
        List<TX> receivedTxs = blockchainDB.getWalletInTXs(address);
        List<TX> filtered = receivedTxs.stream()
            .filter(tx -> "complete".equals(tx.getStatus()))
            .toList();
        return ResponseEntity.ok(filtered);
    }

    @GetMapping("/mempool/pending/{address}")
    public ResponseEntity<List<TX>> getPending(@PathVariable String address) {
        List<TX> pending = MempoolService.getPending(address);
        return ResponseEntity.ok(pending);
    }

    @GetMapping("/label/{address}")
    public ResponseEntity<String> getLabel(@PathVariable String address) {
        try {
            String data = WalletService.getData(address);
            if (data == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Wallet not found");
            }

            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(data);
            JsonNode labelNode = root.get("label");

            if (labelNode != null) {
                return ResponseEntity.ok(labelNode.asText());
            } else {
                return ResponseEntity.ok(""); // No label set
            }
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error retrieving label");
        }
    }

    @GetMapping("/txs/all/{address}")
    public ResponseEntity<Map<String, List<TX>>> getAllUserTxs(@PathVariable String address) {
        try {
            List<TX> pending = MempoolService.getPending(address);
            Map<String, String> rejectedRaw = RejectedService.getRejectedTxsForAddress(address);
            List<TX> rejected = rejectedRaw.values().stream()
                .map(TX::fromJSON)
                .filter(tx -> tx != null)
                .toList();

            List<TX> confirmed = blockchainDB.getWalletCompleteTXs(address).stream()
                .filter(tx -> "complete".equals(tx.getStatus()))
                .toList();

            Map<String, List<TX>> result = Map.of(
                "pending", pending,
                "complete", confirmed,
                "rejected", rejected
            );

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Collections.emptyMap());
        }
    }



    
}
