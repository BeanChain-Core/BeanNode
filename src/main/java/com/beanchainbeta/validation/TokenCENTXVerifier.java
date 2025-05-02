package com.beanchainbeta.validation;


import com.beanchainbeta.network.Node;
import com.beanchainbeta.services.Layer2DBService;
import com.beanchainbeta.services.RejectedService;
import com.beanchainbeta.services.WalletService;
import com.fasterxml.jackson.databind.JsonNode;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import com.bean_core.TXs.*;
import com.bean_core.crypto.*;
import com.bean_core.Utils.*;

public class TokenCENTXVerifier {
    

    public static boolean verifyTransaction(TX tx) throws Exception{
        JsonNode metaNode = MetaHelper.getMetaNode(tx);

        //check CEN signature is valid
        boolean hasAddy = (tx.getFrom() !=null);
        boolean hasSignature = (tx.getSignature() !=null);
        boolean correctHash = (tx.getTxHash().equals(tx.generateHash()));

        if(!Layer2DBService.walletExists(metaNode.get("caller").asText())){
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
        

        //set main CEN Check 
        boolean CENSignatureValid = false;

        if(hasAddy && hasSignature && correctHash) {
            addyMatch = TransactionVerifier.walletMatch(tx.getPublicKeyHex(), tx.getFrom());
            validOwner = TransactionVerifier.verifySHA256Transaction(tx.getPublicKeyHex(), hex.hexToBytes(tx.getTxHash()), tx.getSignature());
            senderHasEnoughGas = WalletService.hasCorrectAmount(tx.getFrom(), 0, tx.getGasFee());
            if(addyMatch && validOwner && senderHasEnoughGas) {
                CENSignatureValid = true;
            } else {
                System.out.println("** TX FAILED: " + tx.getTxHash() + "CEN VERIFICATION FAILURE **");
                tx.setStatus("rejected");
                RejectedService.saveRejectedTransaction(tx);
                Node.broadcastRejection(tx.getTxHash());
                return false;
            }

        } else {
            System.out.println("** TX FAILED: " + tx.getTxHash() + " CEN INFO MISMATCH **");
            tx.setStatus("rejected");
            RejectedService.saveRejectedTransaction(tx);
            Node.broadcastRejection(tx.getTxHash());
            return false;

        }

        //real caller check 
        boolean callerHasAddy = (metaNode.get("caller").asText() != null);
        boolean hasCallSignature = (metaNode.get("callSignature").asText() != null);

        String validHash = genHash(
            metaNode.get("caller").asText(),
            metaNode.get("contract").asText(),
            metaNode.get("contractHash").asText(),
            metaNode.get("callMethod").asText()
        );
        boolean hasCorrectCallHash = (metaNode.get("callHash").asText().equals(validHash));

        boolean senderHasEnoughTokens = false;
        boolean validCaller = false;

        if(callerHasAddy && hasCallSignature && hasCorrectCallHash) {
            senderHasEnoughTokens = Layer2DBService.hasEnoughTokens(metaNode.get("caller").asText(), metaNode.get("tokenHash").asText(), tx.getAmount());
            byte[] callHashBytes = hex.hexToBytes(metaNode.get("callHash").asText());
            validCaller = TransactionVerifier.verifySHA256Transaction(metaNode.get("callerPublicKey").asText(), callHashBytes, metaNode.get("callSignature").asText());
            if(senderHasEnoughTokens && validCaller) {
                return true;
            } else {
                System.out.println("** TX FAILED: " + tx.getTxHash() + "CALLER VERIFICATION FAILURE **");
                tx.setStatus("rejected");
                RejectedService.saveRejectedTransaction(tx);
                Node.broadcastRejection(tx.getTxHash());
                return false;
            }

        } else {
            System.out.println("** TX FAILED: " + tx.getTxHash() + " CALLER INFO MISTMATCH");
            tx.setStatus("rejected");
            RejectedService.saveRejectedTransaction(tx);
            Node.broadcastRejection(tx.getTxHash());
            return false;
        }
    }

    public static String genHash(String caller, String contract, String contractHash, String method){
        try {
            String data = caller + contract + contractHash + method;
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data.getBytes(StandardCharsets.UTF_8));

            StringBuilder hexString = new StringBuilder();
            for(byte b: hash){
                hexString.append(String.format("%02x", b));
            }
            return hexString.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


}
