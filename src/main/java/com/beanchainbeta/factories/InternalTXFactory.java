package com.beanchainbeta.factories;

import com.beanchainbeta.logger.BeanLoggerManager;
import com.beanchainbeta.network.Node;
import com.beanchainbeta.nodePortal.portal;
import com.beanchainbeta.services.WalletService;
import com.beanpack.TXs.TX;
import com.beanpack.Utils.beantoshinomics;
import com.beanpack.crypto.WalletGenerator;

public class InternalTXFactory {
    
    public static boolean sendAndSignTxInternal(String to, double amount, double gasfeeDouble) throws Exception {
        long gasfee = beantoshinomics.convertToBeantoshi(String.valueOf(gasfeeDouble));
        TX adminTX = new TX(portal.admin.address, portal.admin.publicKeyHex, to, amount, WalletService.getNonce(portal.admin.address), gasfee);
        adminTX.sign(WalletGenerator.restorePrivateKey(portal.admin.privateKeyHex));

        try {
            Node.broadcastTransactionStatic(adminTX);
            return true;
        } catch (Exception e){
            BeanLoggerManager.BeanLoggerError("[TX] FAILED TO SEND TX FROM CLI: " + adminTX.createJSON());
            e.printStackTrace();
            return false;
        }

    }
}
