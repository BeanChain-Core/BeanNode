package com.beanchainbeta.startScripts;

import java.io.File;
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
import com.beanpack.beanify.Color;

public class autoStartPrivate {
    public static WizCryptHandler wizCrypt;
    public static String wizKey;
    public static void nodeStart() throws Exception {
        blockchainDB chain = new blockchainDB();
        MempoolService mempoolService = new MempoolService();
        WalletService walletService = new WalletService();
        
        
        System.out.println(":: BeanChain :: Node startup sequence initiated...");
        //System.out.println("▶ IP : " + ConfigLoader.getBindAddress());
        //System.out.println("Working dir: " + System.getProperty("user.dir"));

        //DEV LOGGING!
        // File test = new File("config.docs/wiz.txt.enc");
        // System.out.println("[WIZ TEST] Absolute path: " + test.getAbsolutePath());
        // System.out.println("[WIZ TEST] Exists? " + test.exists());
        // System.out.println("[WIZ TEST] Can read? " + test.canRead());

        boolean signedIn = false;
        while (!signedIn) {
            try {
                if(ConfigLoader.getEncryptedWiz()) {
                    WizCryptHandler.bootWizCrypt(ConfigLoader.getAdminPass());

                    wizKey = WizCryptHandler.readL2EncWizKey();
                    //System.out.println(wizKey); PRIVATE KEY** FLAG DO NOT EXPOSE
                } else {
                    wizKey = wizard.wizardRead(ConfigLoader.getPrivateKeyPath());
                } 
                adminCube admin = new adminCube(wizKey, ConfigLoader.getBindAddress());
                admin.signedIn = true;
                portal.admin = admin;
                signInSuccess();
                signedIn = true;
            } catch (Exception e) {
                WizCryptHandler.wizCryptMessageFactory("FAILED TO SIGN IN", "ERROR");
                Scanner scanner = new Scanner(System.in);
                System.out.println(Color.PURPLE + """
                    ------------------------------------------------------------
                    Type 'wiz' to launch the WizKey Helper Setup Tool.
                    Press Enter or type anything else to retry sign-in.
                    ------------------------------------------------------------""" + Color.RESET);
                String input = scanner.nextLine().trim();
                if(input.equals("wiz")){
                    wizHelper.startCLI();
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
    

