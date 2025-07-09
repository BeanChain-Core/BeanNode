package io.beanchain.startScripts;

import java.util.HashMap;
import java.util.Map;

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
                    signedIn = true;
                } else {
                    wizKey = wizard.wizardRead(ConfigLoader.getPrivateKeyPath());
                    signedIn = true;
                } 
                // if(ConfigLoader.getEncryptedWiz()) { wizKey = wizard.decryptWizKey(wizKey, ConfigLoader.getAdminPass());}

                adminCube admin = new adminCube(wizKey, ConfigLoader.getBindAddress());
                admin.signedIn = true;
                portal.admin = admin;
                signInSuccess();
            } catch (Exception e) {
                WizCryptHandler.wizCryptMessageFactory("FAILED TO SIGN IN", "ERROR");
                Thread.sleep(3000); 
            }
        }
    }

    private static void signInSuccess(){
        if(ConfigLoader.getEncryptedWiz()){
           WizCryptHandler.encryptConfig(); 
        }
        System.out.println(":: BeanChain :: Node startup sequence initiated...");
        //System.out.println("▶ IP : " + ConfigLoader.getBindAddress());
        Thread springThread = new Thread(() -> {
            SpringApplication app = new SpringApplication(BeanChainApi.class);
            
            Map<String, Object> props = new HashMap<>();
            props.put("server.port", ConfigLoader.getSpringPort());
            
            app.setDefaultProperties(props);
            app.run();
        }, "SpringThread");

        springThread.setDaemon(false);
        springThread.start();

        try {
            Thread.sleep(8000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        
        System.out.println("SIGN IN SUCCESS");

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
