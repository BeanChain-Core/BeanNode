package com.beanchainbeta.services;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import com.beanchainbeta.config.ConfigLoader;
import com.beanchainbeta.controllers.DBManager;
import com.beanchainbeta.logger.BeanLoggerManager;
import com.beanpack.Models.*;

import com.beanpack.TXs.TX;
import com.beanpack.Utils.MetaHelper;
import com.beanpack.Utils.beantoshinomics;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.iq80.leveldb.DB;
import org.iq80.leveldb.DBIterator;
import org.springframework.stereotype.Service;

@Service
public class Layer2DBService {
    private static final DB db = DBManager.getDB(ConfigLoader.getLayer2DB());
    private static final ObjectMapper mapper = new ObjectMapper();

    // Check if wallet exists
    public static boolean walletExists(String address) {
        try {
            byte[] value = db.get(address.getBytes(StandardCharsets.UTF_8));
            return value != null;
        } catch (Exception e) {
            System.err.println("Error checking wallet existence: " + address);
            e.printStackTrace();
            return false;
        }
    }

    // Load wallet
    public static Layer2Wallet loadWallet(String address) {
        try {
            byte[] value = db.get(address.getBytes(StandardCharsets.UTF_8));
            if (value != null) {
                String json = new String(value, StandardCharsets.UTF_8);
                return mapper.readValue(json, Layer2Wallet.class);
            }
        } catch (Exception e) {
            System.err.println("Error loading wallet: " + address);
            e.printStackTrace();
        }
        return null;
    }

    // Save wallet
    public static void saveWallet(Layer2Wallet wallet) {
        try {
            String json = mapper.writeValueAsString(wallet);
            db.put(wallet.getAddress().getBytes(StandardCharsets.UTF_8), json.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            System.err.println("Error saving wallet: " + wallet.getAddress());
            e.printStackTrace();
        }
    }

    // Create or load wallet
    public static Layer2Wallet getOrCreateWallet(String address) {
        Layer2Wallet wallet = loadWallet(address);
        if (wallet == null) {
            wallet = new Layer2Wallet(address);
            saveWallet(wallet);
        }
        return wallet;
    }

    // Get token balance
    public static double getTokenBalance(String address, String tokenHash) {
        Layer2Wallet wallet = getOrCreateWallet(address);
        return wallet.getBalance(tokenHash);
    }

    //has enough tokens 
    public static boolean hasEnoughTokens(String address, String tokenHash, double amount) {
        Layer2Wallet wallet = getOrCreateWallet(address);
        double balance = wallet.getBalance(tokenHash);
        return balance >= amount;
    }

    // Increment L2 nonce
    public static void incrementNonce(String address) {
        Layer2Wallet wallet = getOrCreateWallet(address);
        wallet.incrementNonce();
        saveWallet(wallet);
    }

    // Transfer token between wallets
    public static boolean transferToken(String fromAddress, String toAddress, String tokenHash, double amount) {
        try {
            Layer2Wallet from = getOrCreateWallet(fromAddress);
            Layer2Wallet to = getOrCreateWallet(toAddress);

            double fromBalance = from.getBalance(tokenHash);
            if (fromBalance < amount) {
                System.err.println("Insufficient balance for transfer: " + tokenHash + " | " + fromAddress);
                return false;
            }

            from.adjustBalance(tokenHash, -amount);
            to.adjustBalance(tokenHash, amount);
            from.incrementNonce();

            saveWallet(from);
            saveWallet(to);

            return true;
        } catch (Exception e) {
            System.err.println("Transfer failed from " + fromAddress + " to " + toAddress + " | " + tokenHash);
            e.printStackTrace();
            return false;
        }
    }

    public static boolean burnToken(String fromAddress, String tokenHash, double amount) {
        try {
            Layer2Wallet from = getOrCreateWallet(fromAddress);

            double fromBalance = from.getBalance(tokenHash);
            if (fromBalance < amount) {
                System.err.println("Insufficient balance for burn: " + tokenHash + " | " + fromAddress);
                return false;
            }

            if (amount <=0) {
                BeanLoggerManager.BeanLoggerError("INVALID AMOUNT of burn! rejected");
                return false;
            }

            from.adjustBalance(tokenHash, -amount);
            from.incrementNonce();

            saveWallet(from);

            return true;
        } catch (Exception e) {
            System.err.println("Burn failed from " + fromAddress + " | " + tokenHash);
            e.printStackTrace();
            return false;
        }
    }

    //token registry 

    public static void saveToken(TokenStorage token) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            ObjectNode tokenNode = mapper.createObjectNode();
            tokenNode.put("tokenHash", token.getTokenHash());
            tokenNode.put("tokenData", token.getTokenData());
            db.put(("token:" + token.getTokenHash()).getBytes(StandardCharsets.UTF_8), mapper.writeValueAsBytes(tokenNode));
        } catch (Exception e) {
            System.err.println("Error saving token: " + token.getTokenHash());
            e.printStackTrace();
        }
    }

    public static TokenStorage loadToken(String tokenHash) {
        try {
            byte[] value = db.get(("token:" + tokenHash).getBytes(StandardCharsets.UTF_8));
            if (value != null) {
                return mapper.readValue(new String(value, StandardCharsets.UTF_8), TokenStorage.class);
            }
        } catch (Exception e) {
            System.err.println("Error loading token: " + tokenHash);
            e.printStackTrace();
        }
        return null;
    }

    public static boolean tokenExists(String tokenHash) {
        try {
            return db.get(("token:" + tokenHash).getBytes(StandardCharsets.UTF_8)) != null;
        } catch (Exception e) {
            return false;
        }
    }

    public static List<TokenStorage> getAllTokens() {
        List<TokenStorage> tokens = new ArrayList<>();
        try (DBIterator iterator = db.iterator()) {
            for (iterator.seekToFirst(); iterator.hasNext(); iterator.next()) {
                String key = new String(iterator.peekNext().getKey(), StandardCharsets.UTF_8);
                if (key.startsWith("token:")) {
                    TokenStorage token = mapper.readValue(new String(iterator.peekNext().getValue(), StandardCharsets.UTF_8), TokenStorage.class);
                    tokens.add(token);
                }
            }
        } catch (Exception e) {
            System.err.println("Error fetching all tokens");
            e.printStackTrace();
        }
        return tokens;
    }


    public static boolean mintToTokenSupply(String tokenHash, String callerAddress, long amountToAdd) {
        try {
            TokenStorage token = loadToken(tokenHash);
            if (token == null) {
                System.err.println("❌ Cannot mint: token not found.");
                return false;
            }

            // Parse metadata
            JsonNode meta = token.getTokenMetaAsJson();
            boolean mintable = false;
            boolean open = true;
            String minter = meta.has("minter") ? meta.get("minter").asText() : null;


            if (!open && (minter == null || !minter.equals(callerAddress))) {
                System.err.println("❌ Unauthorized mint attempt by: " + callerAddress + " on token: " + tokenHash);
                return false;
            }

            // Add to supply
            long currentSupply = meta.has("supply") ? meta.get("supply").asLong() : 0;
            long newSupply = currentSupply + amountToAdd;

            // Rebuild tokenData with new supply
            TokenStorage updated = new TokenStorage(
                tokenHash,
                meta.get("token").asText(),
                meta.get("symbol").asText(),
                beantoshinomics.toBean(newSupply), // back to double for constructor
                minter,
                mintable,
                open
            );


            saveToken(updated);
            return mintTokenToWallet(callerAddress, tokenHash, beantoshinomics.toBean(amountToAdd));

        } catch (Exception e) {
            System.err.println("❌ Error minting to token supply for: " + tokenHash);
            e.printStackTrace();
            return false;
        }
    }

    public static int getLayer2Nonce(String address) {
        Layer2Wallet wallet = getOrCreateWallet(address);
        return wallet.getL2Nonce();
    }

    //mint function 
    private static boolean mintTokenToWallet(String toAddress, String tokenHash, double amount) {
        try {
            Layer2Wallet to = getOrCreateWallet(toAddress);
    
            to.adjustBalance(tokenHash, amount);
            saveWallet(to);
    
            BeanLoggerManager.BeanLogger("Sucessfully minted " + amount + " of " + tokenHash + " to : " + toAddress);
            BeanLoggerManager.BeanLogger("Wallet after mint: " + to.getTokenBalances());

            return true;
        } catch (Exception e) {
            BeanLoggerManager.BeanLoggerError("Minting failed to " + toAddress + " for token: " + tokenHash);
            e.printStackTrace();
            return false;
        }
    }

    public static boolean newMint(TX tx) throws Exception{
        JsonNode metaNode = MetaHelper.getMetaNode(tx);
        String mode = metaNode.get("mode").asText();
        String tokenHash = metaNode.get("tokenHash").asText();
        if(tokenExists(tokenHash)){
            BeanLoggerManager.BeanLoggerError("Token already exists for hash: " + tokenHash);
            return false;
        }
        String token = metaNode.get("token").asText();
        double supply = metaNode.get("supply").asDouble();
        String symbol = metaNode.get("symbol").asText();
        boolean capped = metaNode.get("capped").asBoolean();
        boolean openMint = metaNode.get("openMint").asBoolean();

        TokenStorage tokenStorage = new TokenStorage(tokenHash, token, symbol, supply, tx.getFrom(), capped, openMint);
            try {
                saveToken(tokenStorage);
                if (supply > 0) {
                    mintTokenToWallet(tx.getFrom(), tokenHash, supply);
                }
                return true;
            } catch (Exception e) {
                System.out.println(e);
                return false;
            }  
    }

    public static void refreshWallet(String address) {
        // Just force re-load from LevelDB
        Layer2Wallet wallet = loadWallet(address);
        if (wallet != null) {
            saveWallet(wallet); // optional
        }
    }

}
