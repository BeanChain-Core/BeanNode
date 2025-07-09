package io.beanchain.validation;

import com.beanpack.TXs.TX;
import com.beanpack.Utils.MetaHelper;
import com.beanpack.Utils.beantoshinomics;
import io.beanchain.factories.CallFactory;
import io.beanchain.logger.BeanLoggerManager;
import io.beanchain.services.Layer2DBService;
import io.beanchain.services.WalletService;
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
                    BeanLoggerManager.BeanLoggerError("**UNKNOWN TYPE ERROR**");
                    return false;
            }
        } catch (Exception e) {
            BeanLoggerManager.BeanLoggerError("**EXECUTION ERROR**");
            return false;
        }
    }
    
    private static boolean executeTransfer(TX tx){
        try {
            WalletService.transfer(tx);
            return true;
        } catch (Exception e) {
            BeanLoggerManager.BeanLoggerError("TX EXECUTION FAILED");
            return false;
        }
    }

    private static boolean executeStake(TX tx){
        try {
            WalletService.transfer(tx);
            CallFactory.StakeCall(tx); 
            return true;
        } catch (Exception e) {
            BeanLoggerManager.BeanLoggerError("STAKE TX EXECUTION FAILED");
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
            BeanLoggerManager.BeanLoggerError("FUNDEDCALL TX EXECUTION FAILED");
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
            BeanLoggerManager.BeanLoggerError("TX EXECUTION FAILED");
            return false;
        }
    }

    private static boolean executeTokenTX(TX tx) throws Exception {
        JsonNode metaNode = MetaHelper.getMetaNode(tx);

        if (!metaNode.has("tokenHash")) {
            System.err.println("Missing tokenHash in meta.");
            return false;
        }

        boolean success = false;

        //  Burn TX (non-CEN)
        if (metaNode.has("execute") && metaNode.get("execute").asText().equals("burn") && !metaNode.has("isCen")) {
            success = Layer2DBService.burnToken(tx.getFrom(), metaNode.get("tokenHash").asText(), tx.getAmount());
        }

        // CEN token TX
        else if (metaNode.has("isCen") && metaNode.get("isCen").asBoolean()) {
            BeanLoggerManager.BeanLogger("**TOKEN TX (CEN)**");

            if (!metaNode.has("caller")) {
                System.err.println("Missing caller field in CEN token TX meta.");
                return false;
            }

            String caller = metaNode.get("caller").asText();
            String tokenHash = metaNode.get("tokenHash").asText();

            if (metaNode.has("execute") && metaNode.get("execute").asText().equals("burn")) {
                success = Layer2DBService.burnToken(caller, tokenHash, tx.getAmount());
            } else {
                success = Layer2DBService.transferToken(caller, tx.getTo(), tokenHash, tx.getAmount());
            }
        }

        //  Standard user token TX
        else {
            BeanLoggerManager.BeanLogger("**TOKEN TX (USER)**");

            success = Layer2DBService.transferToken(
                tx.getFrom(),
                tx.getTo(),
                metaNode.get("tokenHash").asText(),
                tx.getAmount()
            );

            if (success) {
                Layer2DBService.refreshWallet(tx.getFrom());
                Layer2DBService.refreshWallet(tx.getTo());
            }
        }

        //  Always pay gas if successful
        if (success) {
            WalletService.payGasOnly(tx.getFrom(), tx.getGasFee());
        }

        return success;
    }

    private static boolean executeMint(TX tx) {
        try {
            JsonNode metaNode = MetaHelper.getMetaNode(tx);

            if (!metaNode.has("mode") || !metaNode.has("tokenHash")) {
                System.err.println("Mint TX missing required fields.");
                return false;
            }

            String mode = metaNode.get("mode").asText();
            String tokenHash = metaNode.get("tokenHash").asText();

            boolean success = false;

            if (mode.equals("create")) {
                success = Layer2DBService.newMint(tx);
            } else if (mode.equals("mintMore")) {
                long formattedAmount = beantoshinomics.toBeantoshi(tx.getAmount());
                success = Layer2DBService.mintToTokenSupply(tokenHash, tx.getFrom(), formattedAmount);
            } else {
                BeanLoggerManager.BeanLoggerError("Can't find mint mode: " + mode);
                return false;
            }

            // 💰 Always pay gas and refresh if successful
            if (success) {
                WalletService.payGasOnly(tx.getFrom(), tx.getGasFee());
                Layer2DBService.refreshWallet(tx.getFrom());
            }

            return success;

        } catch (Exception e) {
            System.err.println("Error executing Mint TX:");
            e.printStackTrace();
            return false;
        }
    }
 
}

    

