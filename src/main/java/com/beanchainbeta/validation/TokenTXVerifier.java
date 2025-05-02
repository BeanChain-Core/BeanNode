package com.beanchainbeta.validation;

import com.beanchainbeta.network.Node;
import com.beanchainbeta.services.Layer2DBService;
import com.beanchainbeta.services.RejectedService;
import com.beanchainbeta.services.WalletService;
import com.fasterxml.jackson.databind.JsonNode;
import com.bean_core.TXs.*;
import com.bean_core.crypto.*;
import com.bean_core.Utils.*;

public class TokenTXVerifier {

    

    public static boolean verifyTransaction(TX tx) throws Exception{
        JsonNode metaNode = MetaHelper.getMetaNode(tx);
        boolean hasAddy = (tx.getFrom() !=null);
        boolean hasSignature = (tx.getSignature() !=null);
        boolean correctHash = (tx.getTxHash().equals(tx.generateHash()));
        String tokenHashRetrieved;

        if(!Layer2DBService.walletExists(tx.getFrom())){
            System.out.println("TOKEN WALLET NOT FOUND FOR: " + tx.getFrom());
            tx.setStatus("rejected");
            RejectedService.saveRejectedTransaction(tx);
            Node.broadcastRejection(tx.getTxHash());
            return false;
        }

        if (!metaNode.has("tokenHash")) {
            System.err.println("TOKEN TX REJECTED: Missing tokenHash");
            tx.setStatus("rejected");
            RejectedService.saveRejectedTransaction(tx);
            Node.broadcastRejection(tx.getTxHash());
            return false;
        }
        
        //set checks
        boolean addyMatch = false;
        boolean validOwner = false;
        boolean senderHasEnoughGas = false;
        boolean senderHasEnoughTokens = false;

        tokenHashRetrieved = metaNode.get("tokenHash").asText();

    

        if(hasAddy && hasSignature && correctHash) {
            try {
                //goooooood 
                addyMatch = TransactionVerifier.walletMatch(tx.getPublicKeyHex(), tx.getFrom());
                validOwner = TransactionVerifier.verifySHA256Transaction(tx.getPublicKeyHex(), hex.hexToBytes(tx.getTxHash()), tx.getSignature());
                senderHasEnoughGas = WalletService.hasCorrectAmount(tx.getFrom(), 0, tx.getGasFee());
                senderHasEnoughTokens = Layer2DBService.hasEnoughTokens(tx.getFrom(), tokenHashRetrieved, tx.getAmount());

                //baaaaad (bug fixing)
                // System.out.println("🔎 Starting TX field validations for: " + tx.getTxHash());

                // addyMatch = TransactionVerifier.walletMatch(tx.getPublicKeyHex(), tx.getFrom());
                // System.out.println("✅ Address Match: " + addyMatch);

                // validOwner = TransactionVerifier.verifySHA256Transaction(tx.getPublicKeyHex(), hex.hexToBytes(tx.getTxHash()), tx.getSignature());
                // System.out.println("✅ Valid Owner Signature: " + validOwner);

                // senderHasEnoughGas = WalletService.hasCorrectAmount(tx.getFrom(), 0, tx.getGasFee());
                // System.out.println("✅ Has Enough Gas: " + senderHasEnoughGas);

                // senderHasEnoughTokens = Layer2DBService.hasEnoughTokens(tx.getFrom(), tokenHashRetrieved, tx.getAmount());
                // System.out.println("✅ Has Enough Tokens: " + senderHasEnoughTokens);

                // System.out.println("🔚 Finished TX field validations for: " + tx.getTxHash());
                // System.out.println("📊 Wallet balance for token " + tokenHashRetrieved + ": " + Layer2DBService.getTokenBalance(tx.getFrom(), tokenHashRetrieved));
                // System.out.println("📊 Requested send amount: " + tx.getAmount());
                
            } catch (Exception e){
                    System.err.println("❌ Exception during token TX validation: " + tx.getTxHash());
                    e.printStackTrace();
                    tx.setStatus("rejected");
                    RejectedService.saveRejectedTransaction(tx);
                    Node.broadcastRejection(tx.getTxHash());
                    return false;
            }
            if(addyMatch && validOwner && senderHasEnoughGas && senderHasEnoughTokens) {
                return true;
            } else {
                System.out.println("** TX FAILED: " + tx.getTxHash() + " VERIFICATION FAILURE **");
                tx.setStatus("rejected");
                RejectedService.saveRejectedTransaction(tx);
                Node.broadcastRejection(tx.getTxHash());
                return false;
            }

        } else {
            System.out.println("** TX FAILED: " + tx.getTxHash() + " INFO MISMATCH **");
            tx.setStatus("rejected");
            RejectedService.saveRejectedTransaction(tx);
            Node.broadcastRejection(tx.getTxHash());
            return false;

        }
    }


}
