package com.beanchainbeta.validation;

import com.bean_core.TXs.TX;
import com.bean_core.crypto.TransactionVerifier;
import com.beanchainbeta.network.Node;
import com.beanchainbeta.services.Layer2DBService;
import com.beanchainbeta.services.RejectedService;
import com.beanchainbeta.services.WalletService;
import com.fasterxml.jackson.databind.JsonNode;
import com.bean_core.Utils.*;

public class MintVerifier {

    public static boolean verifyTransaction(TX tx) throws Exception{
        JsonNode metaNode = MetaHelper.getMetaNode(tx);

        if (!metaNode.has("tokenHash") || metaNode.get("tokenHash").asText().isBlank()) {
            System.err.println("MINT REJECTED: Missing or empty tokenHash");
            tx.setStatus("rejected");
            RejectedService.saveRejectedTransaction(tx);
            Node.broadcastRejection(tx.getTxHash());
            return false;
        }

        boolean ignoreMinted = false;
        
        if(metaNode.has("mode") || metaNode.get("mode").asText() != null) {
            ignoreMinted = metaNode.get("mode").asText().equals("mintMore");
        }

    
        if(!ignoreMinted && Layer2DBService.tokenExists(metaNode.get("tokenHash").asText())){
            System.err.println("MINT REJECTED: Token already exists with hash " + metaNode.get("tokenHash").asText());
            tx.setStatus("rejected");
            RejectedService.saveRejectedTransaction(tx);
            Node.broadcastRejection(tx.getTxHash());
            return false;
        }

        boolean hasAddy = (tx.getFrom() !=null);
        boolean hasSignature = (tx.getSignature() !=null);
        boolean correctHash = (tx.getTxHash().equals(tx.generateHash()));

        //set checks
        boolean addyMatch = false;
        boolean validOwner = false;
        boolean senderHasEnoughGas = false;
        


        if(hasAddy && hasSignature && correctHash) {
            addyMatch = TransactionVerifier.walletMatch(tx.getPublicKeyHex(), tx.getFrom());
            validOwner = TransactionVerifier.verifySHA256Transaction(tx.getPublicKeyHex(), hex.hexToBytes(tx.getTxHash()), tx.getSignature());
            senderHasEnoughGas = WalletService.hasCorrectAmount(tx.getFrom(), 0, tx.getGasFee());
            
            if(addyMatch && validOwner && senderHasEnoughGas) {
                return true;
            } else {
                System.out.println("** MINT FAILED: " + tx.getTxHash() + " VERIFICATION FAILURE **");
                tx.setStatus("rejected");
                RejectedService.saveRejectedTransaction(tx);
                Node.broadcastRejection(tx.getTxHash());
                return false;
            }

        } else {
            System.out.println("** MINT FAILED: " + tx.getTxHash() + " INFO MISMATCH **");
            tx.setStatus("rejected");
            RejectedService.saveRejectedTransaction(tx);
            Node.broadcastRejection(tx.getTxHash());
            return false;

        }
    }

    
}
