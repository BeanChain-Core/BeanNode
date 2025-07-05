package io.beanchain.startScripts;

import org.springframework.boot.SpringApplication;

import io.beanchain.BeanChainApi;
import io.beanchain.config.ConfigLoader;
import io.beanchain.nodePortal.adminCube;
import io.beanchain.nodePortal.portal;
import io.beanchain.services.CleanupService;
import io.beanchain.validation.BlockTimerBeta;
import com.beanpack.Wizard.*;
import com.beanpack.beanify.Branding;

public class autoStartGPN {
    public static WizCryptHandler wizCrypt;
    

    public static void nodeStart() throws Exception {
        String wizKey;
        
        
        System.out.println(":: BeanChain :: Node startup sequence initiated...");
        //System.out.println("▶ IP : " + ConfigLoader.getBindAddress());
        
    

        boolean signedIn = false;
        while (!signedIn) {
            try {
                if(ConfigLoader.getEncryptedWiz()) {
                    String adminPass = ConfigLoader.getAdminPass();
                    WizCryptHandler.bootWizCrypt(adminPass);
                    wizKey = WizCryptHandler.readL2EncWizKey();
                } else {
                    wizKey = wizard.wizardRead(ConfigLoader.getPrivateKeyPath());
                } 
                // if(ConfigLoader.getEncryptedWiz()) { wizKey = wizard.decryptWizKey(wizKey, ConfigLoader.getAdminPass());}

                adminCube admin = new adminCube(wizKey, ConfigLoader.getBindAddress());
                admin.signedIn = true;
                portal.admin = admin;
                signInSuccess();
                signedIn = true;
            } catch (Exception e) {
                WizCryptHandler.wizCryptMessageFactory("FAILED TO SIGN IN", "ERROR");
                Thread.sleep(3000); 
            }
        }
    }

    private static void signInSuccess(){
        WizCryptHandler.encryptConfig();
        Thread springThread = new Thread(() -> {
                    SpringApplication.run(BeanChainApi.class);
                }, "SpringThread");

        springThread.setDaemon(false);
        springThread.start();
        System.out.println("SIGN IN SUCCESS");

        try {
            Thread.sleep(4000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        System.out.print("\033[H\033[2J");  
        System.out.flush();
        System.out.println("\u001B[32m" + Branding.logo + "\u001B[0m"); 
        BlockTimerBeta.nodeFleccer();
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
