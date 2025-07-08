package io.beanchain.startScripts;

import java.util.Scanner;

import java.util.Scanner;

import org.springframework.boot.SpringApplication;

import io.beanchain.BeanChainApi;
import io.beanchain.config.ConfigLoader;
import io.beanchain.helpers.wizHelper;
import io.beanchain.nodePortal.adminCube;
import io.beanchain.nodePortal.portal;
import io.beanchain.services.CleanupService;
import com.beanpack.Wizard.*;
import com.beanpack.beanify.Branding;
import com.beanpack.beanify.Color;

public class autoStartPublic {
    public static String wizKey;
    public static WizCryptHandler wizCrypt;
    public static void nodeStart() throws Exception {
        
        System.out.println(":: BeanChain :: Node startup sequence initiated...");
        //System.out.println("▶ IP : " + ConfigLoader.getBindAddress());
        Thread springThread = new Thread(() -> {
                    SpringApplication.run(BeanChainApi.class);
                }, "SpringThread");

        springThread.setDaemon(false);
        springThread.start();

        try {
            Thread.sleep(8000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        

        boolean signedIn = false;
        while (!signedIn) {
            try {
                if(ConfigLoader.getEncryptedWiz()) {
                    WizCryptHandler.bootWizCrypt(ConfigLoader.getAdminPass());
                    wizKey = WizCryptHandler.readL2EncWizKey();
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

    private static void signInSuccess(){
        System.out.println("SIGN IN SUCCESS");
        WizCryptHandler.encryptConfig();

        try {
            Thread.sleep(4000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        System.out.print("\033[H\033[2J");  
        System.out.flush();
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
    

