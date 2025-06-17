package com.beanchainbeta.helpers;

import java.util.Scanner;

import com.beanpack.Wizard.wizard;
import com.beanpack.crypto.WalletGenerator;

/**
 * THIS IS A HELPER CLASS TO SAVE YOUR WIZ KEY
 * THE ENCRYPTED METHODS ARE CURRENTLY IN DEV AND NOT FULLY FUNCTIONAL
 * PLEASE ONLY USE OPTION 1 or 3(with pasted private key below) TO MAKE UNENCRYPTED KEYS UNTIL THE ENCRYPTION PATH IS FINISHED
 */
public class wizHelper {
    private static String adminPass = "admin"; //must match the admin password in your config if you use this to generate your wiz key 
    
    static String privateKey = "no key added"; //place your unique private key here to use the save to wiz function 
    static String path = "./config.docs/wiz.txt";

    public static void genWizAndSave() throws Exception{
        privateKey = WalletGenerator.generatePrivateKey();
        wizard.saveKeyToWizard(privateKey, path);
    }

    public static void userWizAndSave() throws Exception{
        wizard.saveKeyToWizard(privateKey, path);
    }

    public static void genWizAndSaveEncrypted() throws Exception{
        privateKey = WalletGenerator.generatePrivateKey();
        String encryptedWizKey = wizard.getEncryptedWizardKey(privateKey, adminPass);
        wizard.saveEncryptedWizKey(encryptedWizKey, path);
    }

    public static void userWizAndSaveEncrypted() throws Exception{
        String encryptedWizKey = wizard.getEncryptedWizardKey(privateKey, adminPass);
        wizard.saveEncryptedWizKey(encryptedWizKey, path);
    }

    

    public static void main(String[] args) {
        try (Scanner scanner = new Scanner(System.in)) {
            while (true) {
                System.out.println("\nWizKey Utility:");
                System.out.println("1. Generate & Save New WizKey (unencrypted)");
                System.out.println("2. Generate & Save New WizKey (encrypted)");
                System.out.println("3. Save Pre-Coded Private Key as WizKey (unencrypted)");
                System.out.println("4. Save Pre-Coded Private Key as WizKey (encrypted)");
                System.out.println("5. Exit");
                System.out.print("Select an option: ");

                String input = scanner.nextLine().trim();

                switch (input) {
                    case "1":
                        genWizAndSave();
                        break;
                    case "2":
                        genWizAndSaveEncrypted();
                        break;
                    case "3":
                        userWizAndSave();
                        break;
                    case "4":
                        userWizAndSaveEncrypted();
                        break;
                    case "5":
                        System.out.println("Exiting WizKey Tool.");
                        return;
                    default:
                        System.out.println("Unknown option. Please try 1–5.");
                        break;
                }
            }
        } catch (Exception e) {
            System.err.println("❌ Error:");
            e.printStackTrace();
        }
    }
}
