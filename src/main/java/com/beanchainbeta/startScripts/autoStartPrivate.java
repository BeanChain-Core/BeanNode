package com.beanchainbeta.startScripts;

import java.util.Scanner;

import com.beanchainbeta.config.ConfigLoader;
import com.beanchainbeta.helpers.wizHelper;
import com.beanchainbeta.nodePortal.adminCube;
import com.beanchainbeta.nodePortal.portal;
import com.beanchainbeta.services.CleanupService;
import com.beanchainbeta.services.MempoolService;
import com.beanchainbeta.services.WalletService;
import com.beanchainbeta.services.blockchainDB;
import com.beanpack.Wizard.*;
import com.beanpack.beanify.Branding;

public class autoStartPrivate {
    public static void nodeStart() throws Exception {
        blockchainDB chain = new blockchainDB();
        MempoolService mempoolService = new MempoolService();
        WalletService walletService = new WalletService();
        
        
        System.out.println("🫘 BeanChain Node Initializing...");
        System.out.println("▶ IP : " + ConfigLoader.getBindAddress());
        

        boolean signedIn = false;
        while (!signedIn) {
            try {
                String wizKey = wizard.wizardRead(ConfigLoader.getPrivateKeyPath());
                if(ConfigLoader.getEncryptedWiz()) { wizKey = wizard.decryptWizKey(wizKey, ConfigLoader.getAdminPass());}
                adminCube admin = new adminCube(wizKey, ConfigLoader.getBindAddress());
                admin.signedIn = true;
                portal.admin = admin;
                signInSuccess();
                signedIn = true;
            } catch (Exception e) {
                System.out.println("SIGN IN FAILED: " + e.getMessage());
                Scanner scanner = new Scanner(System.in);
                System.out.println("Type 'wiz' to launch the WizKey helper. (anything else to retry sign in)");
                String input = scanner.nextLine().trim();
                if(input.equals("wiz")){
                    wizHelper.main(null);
                }
                Thread.sleep(3000); // pause before retrying
            }
        }
    }

    private static void signInSuccess() throws Exception{
        System.out.println("SIGN IN SUCCESS");

        try {
            Thread.sleep(4000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        System.out.println("\u001B[32m" + Branding.logo + "\u001B[0m"); 
        Thread cleanUp = new Thread(() -> {
            while (true) {
                try {
                    CleanupService.runFullCleanup();
                    Thread.sleep(6 * 60 * 60 * 1000); // Sleep 6 hours***** test and possibly adjust 
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }, "CleanUp");
        cleanUp.start();
    }
}
    

