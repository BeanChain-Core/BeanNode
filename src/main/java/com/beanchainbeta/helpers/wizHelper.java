package com.beanchainbeta.helpers;

import java.io.IOException;
import java.util.Scanner;

import com.beanchainbeta.config.ConfigLoader;
import com.beanpack.Wizard.wizard;
import com.beanpack.crypto.WalletGenerator;

/**
 * THIS IS A HELPER CLASS TO SAVE YOUR WIZ KEY
 * THE ENCRYPTED METHODS ARE CURRENTLY IN DEV AND NOT FULLY FUNCTIONAL
 * PLEASE ONLY USE OPTION 1 or 3(with pasted private key below) TO MAKE UNENCRYPTED KEYS UNTIL THE ENCRYPTION PATH IS FINISHED
 */
public class wizHelper {
    private static Scanner scanner = new Scanner(System.in);
    
    
    static String path = ConfigLoader.getPrivateKeyPath();

    public static void genWizAndSave() throws Exception{
        String privateKey = WalletGenerator.generatePrivateKey();
        wizard.saveKeyToWizard(privateKey, path);
    }

    public static void userWizAndSave() throws Exception {
        String key;

        while (true) {
            System.out.println("Enter your 64-character private key");
            key = scanner.nextLine().trim();

            // Check if it's exactly 64 characters
            if (keyCheck(key)){
                wizard.saveKeyToWizard(key, path);
                System.out.println("Key saved successfully.");
            } else {
                userWizAndSave();
            }
        }
    }

    public static void genWizAndSaveEncrypted() throws Exception{
        System.out.println("Enter Encryption Pass");
        String encryptionPass = scanner.nextLine().trim();
        String privateKey = WalletGenerator.generatePrivateKey();
        String encryptedWizKey = wizard.getEncryptedWizardKey(privateKey, encryptionPass);
        wizard.saveEncryptedWizKey(encryptedWizKey, path);
    }

    public static void userWizAndSaveEncrypted() throws Exception{
        while (true) {
            System.out.println("Enter your 64-character private key");
            String encryptedKey = scanner.nextLine().trim();

            System.out.println("Enter Encryption Pass");
            String adminEncryptPass = scanner.nextLine().trim();

            // Check if it's exactly 64 characters
            if (keyCheck(encryptedKey)){
                String encryptedWizKey = wizard.getEncryptedWizardKey(encryptedKey, adminEncryptPass);
                wizard.saveEncryptedWizKey(encryptedWizKey, path);
                System.out.println("Key saved successfully.");
                return;
            } else {
                userWizAndSaveEncrypted();
            }
        }
    }

    public static boolean keyCheck(String key){
        if (key.length() == 64) {
                return true;
            } else {
                System.out.println("Invalid private key length, please try again");
                return false;
            }
    }

    public static void displayUnencryptedKey() throws Exception{
        System.out.println("Enter Admin Pass");
        String pass = scanner.nextLine().trim();
        String encrypted = wizard.wizardRead(path);
        String unEncrypted = wizard.decryptWizKey(encrypted, pass);
        System.out.println("Private Key Hex: " +  unEncrypted);
    }

    public static void  displayKey() throws IOException{
        System.out.println("Private Key Hex: " + wizard.wizardRead(path));
    }

    

    public static void main(String[] args) {
        try  {
            while (true) {
                System.out.println("\nWizKey Utility:");
                System.out.println("1. Generate & Save New WizKey (unencrypted)");
                System.out.println("2. Generate & Save New WizKey (encrypted)");
                System.out.println("3. Save your own Private Key as WizKey (unencrypted)");
                System.out.println("4. Save your own Private Key as WizKey (encrypted)");
                System.out.println("5. Display your Key");
                System.out.println("6. Display encrypted key (Pass Required)");
                System.out.println("7. Exit");
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
                        displayKey();
                        break;
                    case "6":
                        displayUnencryptedKey();
                        break;
                    case "7":
                        System.out.println("Exiting WizKey Tool.");
                        return;
                    default:
                        System.out.println("Unknown option. Please try 1–7.");
                        break;
                }
            }
        } catch (Exception e) {
            System.err.println("Error:");
            e.printStackTrace();
        }
    }
}
