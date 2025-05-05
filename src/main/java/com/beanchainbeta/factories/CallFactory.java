package com.beanchainbeta.factories;


import com.beanchainbeta.config.*;
import com.bean_core.TXs.CENCALL;
import com.bean_core.TXs.TX;
import com.bean_core.Utils.MetaHelper;
import com.beanchainbeta.network.Node;
import com.fasterxml.jackson.databind.JsonNode;

public class CallFactory {
    

    public static void StakeCall(TX tx) throws Exception{
        JsonNode metaNode = MetaHelper.getMetaNode(tx);

        CENCALL call = new CENCALL(
            tx.getFrom(),
            tx.getPublicKeyHex(),
            "StakeContract",
            "STAKECONTRACTHASH(NEEDTHIS)",
            ContractRegistry.CEN_IP,
            metaNode.get("mode").asText());

        call.addParam("originalStakeTx", tx.createJSON());
        call.finalizeParams();

        Node.broadcastCENCALL(ContractRegistry.CEN_IP, call);
    }
    
}
