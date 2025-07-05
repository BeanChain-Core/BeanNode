package io.beanchain.helpers;

import java.io.IOException;
import java.util.Scanner;

import io.beanchain.config.ConfigLoader;
import com.beanpack.Wizard.WizCryptHandler;
import com.beanpack.Wizard.wizard;
import com.beanpack.beanify.Color;
import com.beanpack.crypto.WalletGenerator;

/**
 * THIS IS A HELPER CLASS TO SAVE YOUR WIZ KEY
 * THE ENCRYPTED METHODS ARE CURRENTLY IN DEV AND NOT FULLY FUNCTIONAL
 * PLEASE ONLY USE OPTION 1 or 3(with pasted private key below) TO MAKE UNENCRYPTED KEYS UNTIL THE ENCRYPTION PATH IS FINISHED
 */
public class wizHelper {
    private static Scanner scanner = new Scanner(System.in);
    
    
    static String path = ConfigLoader.getPrivateKeyPath();
    public static WizCryptHandler wizCrypt;

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
        if(encryptionPass.equals(ConfigLoader.getAdminPass())){
            String privateKey = WalletGenerator.generatePrivateKey();
            WizCryptHandler.initializeWizCrypt();
            WizCryptHandler.showLoadingBarDynamic("WORKING", 5000);
            WizCryptHandler.encryptPrivateKeyHexRaw(privateKey);
        } else {
            System.out.println("INVALID ADMIN PASS");
        }
    }

    public static void userWizAndSaveEncrypted() throws Exception{
        while (true) {
            System.out.println("Enter your 64-character private key");
            String encryptedKey = scanner.nextLine().trim();

            System.out.println("Enter Encryption Pass");
            String adminEncryptPass = scanner.nextLine().trim();

            if(adminEncryptPass.equals(ConfigLoader.getAdminPass())){
                if (keyCheck(encryptedKey)){
                    WizCryptHandler.initializeWizCrypt();
                    WizCryptHandler.encryptPrivateKeyHexRaw(encryptedKey);
                    System.out.println("Key saved successfully.");
                    return;
                } else {
                    System.out.println("Invalid Key Length");
                    userWizAndSaveEncrypted();
                }
            } else {
                System.out.println("INVALID ADMIN PASS");
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
        WizCryptHandler.bootWizCrypt(pass);
        String unEncrypted = WizCryptHandler.readL2EncWizKey();
        System.out.println("Private Key Hex: " +  unEncrypted);
    }

    public static void displayUnencryptedKeyInternal(String pass) throws Exception{
        WizCryptHandler.bootWizCrypt(pass);
        String unEncrypted = WizCryptHandler.readL2EncWizKey();
        System.out.println("Private Key Hex: " +  unEncrypted);
    }

    public static void  displayKey() throws IOException{
        System.out.println("Private Key Hex: " + wizard.wizardRead(path));
    }

    

    public static void startCLI() {
        System.out.println(Color.YELLOW + """
            ============================================================
                          WIZ HELPER — SETUP TOOL ONLY
            ============================================================
            This setup tool is intended for local, private use only.

            During setup, your password and private key will be visible
            in plaintext. Do not use this tool in shared or production
            environments.

            Once setup is complete, your WizKey will be encrypted and
            usable in secure, headless mode.
            ============================================================
            """ + Color.RESET);
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
