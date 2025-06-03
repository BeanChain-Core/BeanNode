package com.beanchainbeta.validation;

import com.beanpack.TXs.TX;
import com.beanpack.Utils.MetaHelper;
import com.beanpack.Utils.beantoshinomics;
import com.beanchainbeta.factories.CallFactory;
import com.beanchainbeta.services.Layer2DBService;
import com.beanchainbeta.services.WalletService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class TXExecutor {

    public static boolean execute(TX tx) {
        try {
            switch (tx.getType()) {
                case "transfer":
                    return executeTransfer(tx);
                case "stake":
                    return executeStake(tx);
                case "airdrop": 
                    return executeAirdrop(tx);
                case "token":
                    return executeTokenTX(tx);
                case "mint":
                    return executeMint(tx);
                case "cen":
                    return executeFundedCallTX(tx);
                default:
                    System.out.println("**UNKNOWN TYPE ERROR**");
                    return false;
            }
        } catch (Exception e) {
            System.out.println("**EXECUTION ERROR**");
            return false;
        }
    }
    
    private static boolean executeTransfer(TX tx){
        try {
            WalletService.transfer(tx);
            return true;
        } catch (Exception e) {
            System.out.println("TX EXECUTION FAILED");
            return false;
        }
    }

    private static boolean executeStake(TX tx){
        try {
            WalletService.transfer(tx);
            CallFactory.StakeCall(tx); 
            return true;
        } catch (Exception e) {
            System.out.println("STAKE TX EXECUTION FAILED");
            return false;
        }
    }

    private static boolean executeFundedCallTX(TX tx){
        try {
            JsonNode metaNode = MetaHelper.getMetaNode(tx);
            ObjectMapper mapper = new ObjectMapper();
            JsonNode paramsNode = mapper.readTree(metaNode.get("params").asText());
            if (!paramsNode.has("cenIP")) {
                System.err.println("MISSING CENIP IN PARAMS FOR CALL TX");
                return false;
            }
            String cenIP = paramsNode.get("cenIP").asText();
            WalletService.transfer(tx);
            CallFactory.FundedCall(tx);
            return true;
        } catch (Exception e) {
            System.out.println("FUNDEDCALL TX EXECUTION FAILED");
            return false;
        }
    }

    private static boolean executeAirdrop(TX tx){
        try {
            JsonNode metaNode = MetaHelper.getMetaNode(tx);
            String fundWallet = metaNode.get("fundWallet").asText();
            TX quickBuild = new TX(fundWallet, "AIRDROPQUICK", tx.getTo(), tx.getAmount(), 0, 0);
            WalletService.transfer(quickBuild);     
            return true;
        } catch (Exception e) {
            System.out.println("TX EXECUTION FAILED");
            return false;
        }
    }

    private static boolean executeTokenTX(TX tx) throws Exception {
        JsonNode metaNode = MetaHelper.getMetaNode(tx);

        
    
        if (!metaNode.has("tokenHash")) {
            System.err.println("❌ Missing tokenHash in meta.");
            return false;
        }

        if(metaNode.has("execute") && metaNode.get("execute").asText().equals("burn") && !metaNode.has("isCen")) {
            boolean check = Layer2DBService.burnToken(tx.getFrom(), metaNode.get("tokenHash").asText(), tx.getAmount());
            if(check){
                WalletService.payGasOnly(tx.getFrom(), tx.getGasFee());
            }
            return check; 
        }

        try {
            boolean isCenTx = metaNode.has("isCen") && metaNode.get("isCen") != null && metaNode.get("isCen").asBoolean();
    
            if (isCenTx) {
                System.out.println("**TOKEN TX (CEN)**");
    
                if (!metaNode.has("caller") || metaNode.get("caller") == null) {
                    System.err.println("❌ Missing caller field in CEN token TX meta.");
                    return false;
                }

                if (metaNode.has("execute") && metaNode.get("execute").asText().equals("burn")){
                    boolean check = Layer2DBService.burnToken(metaNode.get("caller").asText(), metaNode.get("tokenHash").asText(), tx.getAmount());
                    if(check){
                        WalletService.payGasOnly(tx.getFrom(), tx.getGasFee());
                    }
                    return check; 
                }
    
                return Layer2DBService.transferToken(
                    metaNode.get("caller").asText(),
                    tx.getTo(),
                    metaNode.get("tokenHash").asText(),
                    tx.getAmount()
                );
            } else {
                System.out.println("**TOKEN TX (USER)**");
                boolean yes = Layer2DBService.transferToken(
                    tx.getFrom(),
                    tx.getTo(),
                    metaNode.get("tokenHash").asText(),
                    tx.getAmount()
                );
                if(yes){
                    WalletService.payGasOnly(tx.getFrom(), tx.getGasFee());
                    Layer2DBService.refreshWallet(tx.getFrom());
                    Layer2DBService.refreshWallet(tx.getTo());
                }
                return yes;
            }
        } catch (Exception e) {
            System.out.println("❌ TOKEN TX EXECUTION FAILED");
            e.printStackTrace();
            return false;
        }

        
    }

    private static boolean executeMint(TX tx) {
        try {
            JsonNode metaNode = MetaHelper.getMetaNode(tx);
    
            if (!metaNode.has("mode") || !metaNode.has("tokenHash")) {
                System.err.println("Mint TX missing required fields.");
                return false;
            }
    
            String token;
            String mode = metaNode.get("mode").asText();
            String tokenHash = metaNode.get("tokenHash").asText();
            if(metaNode.has("token")){
                token = metaNode.get("token").asText();
            }
            boolean check = false;
    
            if (mode.equals("create")) {
                check =  Layer2DBService.newMint(tx);
    
            } else if (mode.equals("mintMore")) {
                long formattedAmount = beantoshinomics.toBeantoshi(tx.getAmount());
                check = Layer2DBService.mintToTokenSupply(tokenHash, tx.getFrom(), formattedAmount);
            } else {
                System.out.println("Can't find mint mode: " + mode);
                return false;
            }

            if(check) {
                WalletService.payGasOnly(tx.getFrom(), tx.getGasFee());
                Layer2DBService.refreshWallet(tx.getFrom());
            }
            return check;
    
        } catch (Exception e) {
            System.err.println("Error executing Mint TX:");
            e.printStackTrace();
            return false;
        }
    }
 
}

    

