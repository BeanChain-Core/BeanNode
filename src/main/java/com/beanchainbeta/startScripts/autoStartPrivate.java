package com.beanchainbeta.startScripts;

import com.beanchainbeta.config.ConfigLoader;
import com.beanchainbeta.nodePortal.adminCube;
import com.beanchainbeta.nodePortal.portal;
import com.beanchainbeta.services.CleanupService;
import com.beanchainbeta.services.MempoolService;
import com.beanchainbeta.services.WalletService;
import com.beanchainbeta.services.blockchainDB;
import com.bean_core.Wizard.*;
import com.bean_core.beanify.Branding;

public class autoStartPrivate {
    public static void nodeStart() throws Exception {
        blockchainDB chain = new blockchainDB();
        MempoolService mempoolService = new MempoolService();
        WalletService walletService = new WalletService();
        
        
        System.out.println("🫘 BeanChain Node Initializing...");
        System.out.println("▶ IP : " + ConfigLoader.bindAddress);
        

        boolean signedIn = false;
        while (!signedIn) {
            try {
                adminCube admin = new adminCube(wizard.wizardRead(ConfigLoader.privateKeyPath), ConfigLoader.bindAddress);
                admin.signedIn = true;
                portal.admin = admin;
                signInSuccess();
                signedIn = true;
            } catch (Exception e) {
                System.out.println("SIGN IN FAILED: " + e.getMessage());
                Thread.sleep(3000); // pause before retrying
            }
        }
    }

    private static void signInSuccess(){
        // Thread springThread = new Thread(() -> {
        //             SpringApplication.run(BeanChainApi.class);
        //         }, "SpringThread");

        // springThread.setDaemon(false);
        // springThread.start();
        System.out.println("SIGN IN SUCCESS");

        try {
            Thread.sleep(4000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        //System.out.print("\033[H\033[2J");  
        //System.out.flush();
        System.out.println("\u001B[32m" + Branding.logo + "\u001B[0m"); 
        // TimerFunc.nodeFleccer();
        new Thread(() -> {
            while (true) {
                try {
                    CleanupService.runFullCleanup();
                    Thread.sleep(6 * 60 * 60 * 1000); // Sleep 6 hours
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }
}
    

