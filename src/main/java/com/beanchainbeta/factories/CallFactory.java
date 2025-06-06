package com.beanchainbeta.factories;


import com.beanchainbeta.config.*;

import com.beanchainbeta.network.Node;
import com.beanpack.TXs.CENCALL;
import com.beanpack.TXs.TX;
import com.beanpack.Utils.MetaHelper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

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

    public static void FundedCall(TX tx) throws Exception{
        JsonNode metaNode = MetaHelper.getMetaNode(tx);
        ObjectMapper mapper = new ObjectMapper();
        JsonNode paramNode = mapper.readTree(metaNode.get("params").asText());
        String cenIP = paramNode.get("cenIP").asText();

        CENCALL call = new CENCALL(
            tx.getFrom(),
            tx.getPublicKeyHex(),
            paramNode.get("contractName").asText(),
            paramNode.get("contractHash").asText(),
            cenIP,
            metaNode.get("method").asText());
        call.setParams(metaNode.get("params").asText());
        call.addParam("originalStakeTx", tx.createJSON());
        call.finalizeParams();
        
        Node.broadcastCENCALL(cenIP, call);

    }
    
}
