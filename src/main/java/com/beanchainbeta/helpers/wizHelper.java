package com.beanchainbeta.helpers;

import com.beanpack.Wizard.wizard;
import com.beanpack.crypto.WalletGenerator;

public class wizHelper {
    
    static String privateKey;
    static String path = "./config.docs/wiz.txt";

    public static void main(String[] args) throws Exception {
        privateKey = WalletGenerator.generatePrivateKey();
        wizard.saveKeyToWizard(privateKey, path);
    }
}
