package io.beanchain.validation;

import io.beanchain.logger.BeanLoggerManager;
import io.beanchain.network.Node;
import io.beanchain.services.Layer2DBService;
import io.beanchain.services.RejectedService;
import io.beanchain.services.WalletService;
import com.fasterxml.jackson.databind.JsonNode;
import com.beanpack.Rejection.Flagger;
import com.beanpack.TXs.*;
import com.beanpack.crypto.*;
import com.beanpack.Utils.*;

public class TokenTXVerifier {

    

    public static boolean verifyTransaction(TX tx) throws Exception{
        JsonNode metaNode = MetaHelper.getMetaNode(tx);
        boolean hasAddy = (tx.getFrom() !=null);
        boolean hasSignature = (tx.getSignature() !=null);
        boolean correctHash = (tx.getTxHash().equals(tx.generateHash()));
        String tokenHashRetrieved;

        if(!Layer2DBService.walletExists(tx.getFrom())){
            BeanLoggerManager.BeanLoggerError("TOKEN WALLET NOT FOUND FOR: " + tx.getFrom());
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
                // BeanLoggerManager.BeanLogger("🔎 Starting TX field validations for: " + tx.getTxHash());

                // addyMatch = TransactionVerifier.walletMatch(tx.getPublicKeyHex(), tx.getFrom());
                // BeanLoggerManager.BeanLogger("✅ Address Match: " + addyMatch);

                // validOwner = TransactionVerifier.verifySHA256Transaction(tx.getPublicKeyHex(), hex.hexToBytes(tx.getTxHash()), tx.getSignature());
                // BeanLoggerManager.BeanLogger("✅ Valid Owner Signature: " + validOwner);

                // senderHasEnoughGas = WalletService.hasCorrectAmount(tx.getFrom(), 0, tx.getGasFee());
                // BeanLoggerManager.BeanLogger("✅ Has Enough Gas: " + senderHasEnoughGas);

                // senderHasEnoughTokens = Layer2DBService.hasEnoughTokens(tx.getFrom(), tokenHashRetrieved, tx.getAmount());
                // BeanLoggerManager.BeanLogger("✅ Has Enough Tokens: " + senderHasEnoughTokens);

                // BeanLoggerManager.BeanLogger("🔚 Finished TX field validations for: " + tx.getTxHash());
                // BeanLoggerManager.BeanLogger("📊 Wallet balance for token " + tokenHashRetrieved + ": " + Layer2DBService.getTokenBalance(tx.getFrom(), tokenHashRetrieved));
                // BeanLoggerManager.BeanLogger("📊 Requested send amount: " + tx.getAmount());
                
            } catch (Exception e){
                    System.err.println("❌ Exception during token TX validation: " + tx.getTxHash());
                    e.printStackTrace();
                    tx.setStatus("rejected");
                    try {
                        Flagger.repackRejection(tx, "Exception thrown during validation");
                        RejectedService.saveRejectedTransaction(tx);
                    } catch (Exception a) {
                        BeanLoggerManager.BeanLoggerError("Failed to flag/save rejected TX: " + tx.getTxHash());
                        a.printStackTrace();
                    }
                    Node.broadcastRejection(tx.getTxHash());
                    return false;
            }
            if(addyMatch && validOwner && senderHasEnoughGas && senderHasEnoughTokens) {
                return true;
            } else {
                BeanLoggerManager.BeanLoggerError("** TX FAILED: " + tx.getTxHash() + " VERIFICATION FAILURE **");
                tx.setStatus("rejected");
                try {
                        Flagger.repackRejection(tx, "Not enough funds, or indavlid sender.**");
                        RejectedService.saveRejectedTransaction(tx);
                    } catch (Exception e) {
                        BeanLoggerManager.BeanLoggerError("Failed to flag/save rejected TX: " + tx.getTxHash());
                        e.printStackTrace();
                    }
                Node.broadcastRejection(tx.getTxHash());
                return false;
            }

        } else {
            BeanLoggerManager.BeanLoggerError("** TX FAILED: " + tx.getTxHash() + " INFO MISMATCH **");
            tx.setStatus("rejected");
            RejectedService.saveRejectedTransaction(tx);
            Node.broadcastRejection(tx.getTxHash());
            return false;

        }
    }


}
