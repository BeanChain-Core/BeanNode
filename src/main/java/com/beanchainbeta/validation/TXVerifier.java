package com.beanchainbeta.validation;

import com.beanchainbeta.network.Node;
import com.beanchainbeta.services.RejectedService;
import com.beanchainbeta.services.WalletService;
import com.fasterxml.jackson.databind.JsonNode;
import com.bean_core.TXs.*;
import com.bean_core.crypto.*;
import com.bean_core.Utils.*;

public class TXVerifier {
    private static final String RN_ADDRESS = "BEANX:0x5283d1e237b034c35e9ff8f586cedbe18abcccff";

    //runs a lot of boolean checks to decide if the transaction is valid and can be added to a new block 
    public static boolean verifyTransaction(TX tx) throws Exception{
        //debug
        //this.debugHashValues();
        //end-debug
        if (tx.getSignature() != null && tx.getSignature().equals("GENESIS-SIGNATURE")) {
            //System.out.println(" GENESIS TX accepted without signature verification: " + txHash);
            return true;
        }

        boolean isAirdrop = tx.getType().equals("airdrop");

        if (isAirdrop) {
            if (!tx.getFrom().equals(RN_ADDRESS)) {
                System.out.println("** TX FAILED: " + tx.getTxHash() + " INVALID RN AIRDROP **");
                tx.setStatus("rejected");
                RejectedService.saveRejectedTransaction(tx);
                Node.broadcastRejection(tx.getTxHash());
                return false;
            } 
        }

        boolean hasAddy = (tx.getFrom() !=null);
        boolean hasSignature = (tx.getSignature() !=null);
        boolean correctHash = (tx.getTxHash().equals(tx.generateHash()));
        

        //set checks
        boolean addyMatch = false;
        boolean validOwner = false;
        boolean senderHasEnough = false;

        if(hasAddy && hasSignature && correctHash) {
            addyMatch = TransactionVerifier.walletMatch(tx.getPublicKeyHex(), tx.getFrom());
            validOwner = TransactionVerifier.verifySHA256Transaction(tx.getPublicKeyHex(), hex.hexToBytes(tx.getTxHash()), tx.getSignature());

            if (isAirdrop) {
                // For Airdrops, check fund wallet instead of RN address
                JsonNode metaNode = MetaHelper.getMetaNode(tx);
                String fundWallet = metaNode.get("fundWallet").asText();
                senderHasEnough = WalletService.hasCorrectAmount(fundWallet, tx.getAmount(), 0); // Usually airdrops have gasFee=0
            } else {
                senderHasEnough = WalletService.hasCorrectAmount(tx.getFrom(), tx.getAmount(), tx.getGasFee());
            }

            if(addyMatch && validOwner && senderHasEnough) {
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
