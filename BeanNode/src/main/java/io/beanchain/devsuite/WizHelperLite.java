package io.beanchain.devsuite;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Scanner;

import com.beanpack.Wizard.WizCryptHandler;
import com.beanpack.Wizard.wizard;
import com.beanpack.beanify.Color;
import com.beanpack.crypto.WalletGenerator;
import java.io.File;

public class WizHelperLite {
    private static final String CONFIG_FOLDER = "config.docs/";
    private static final String ENC_WIZ_FILE = "wiz.txt.enc";
    private static final String KEY_PATH = CONFIG_FOLDER + "wiz.txt";
    static {
        WizCryptHandler.setConfigFolder(new File(CONFIG_FOLDER));
        WizCryptHandler.setKeyPath(KEY_PATH);
        WizCryptHandler.setWizFileEnc(ENC_WIZ_FILE);
    }
    private static Scanner scanner = new Scanner(System.in);

    public static void genWizAndSave() throws Exception {
        String privateKey = WalletGenerator.generatePrivateKey();
        wizard.saveKeyToWizard(privateKey, KEY_PATH);
    }

    public static void userWizAndSave() throws Exception {
        System.out.println("Enter your 64-character private key:");
        String key = scanner.nextLine().trim();
        if (keyCheck(key)) {
            wizard.saveKeyToWizard(key, KEY_PATH);
            System.out.println("Key saved successfully.");
        } else {
            System.out.println("Invalid key. Try again.");
            userWizAndSave();
        }
    }

    public static void genWizAndSaveEncrypted() throws Exception {
        System.out.print("Enter Encryption Pass: ");
        String pass = scanner.nextLine().trim();
        String privateKey = WalletGenerator.generatePrivateKey();
        WizCryptHandler.setPassword(pass);
        WizCryptHandler.initializeWizCrypt();
        WizCryptHandler.showLoadingBarDynamic("WORKING", 5000);
        WizCryptHandler.encryptPrivateKeyHexRaw(privateKey);
    }

    public static void userWizAndSaveEncrypted() throws Exception {
        System.out.println("Enter your 64-character private key:");
        String key = scanner.nextLine().trim();
        System.out.println("Enter Encryption Pass:");
        String pass = scanner.nextLine().trim();
        if (keyCheck(key)) {
            WizCryptHandler.setPassword(pass);
            WizCryptHandler.initializeWizCrypt();
            WizCryptHandler.encryptPrivateKeyHexRaw(key);
            System.out.println("Encrypted key saved.");
        } else {
            System.out.println("Invalid key length.");
            userWizAndSaveEncrypted();
        }
    }

    public static void displayUnencryptedKey() throws Exception {
        System.out.print("Enter Pass: ");
        String pass = scanner.nextLine().trim();
        WizCryptHandler.bootWizCrypt(pass);
        String result = WizCryptHandler.readL2EncWizKey();
        System.out.println("Decrypted Key: " + result);
    }

    public static void displayKey() throws IOException {
        if (Files.exists(Paths.get(KEY_PATH))) {
            System.out.println("WizKey: " + wizard.wizardRead(KEY_PATH));
        } else {
            System.out.println("WizKey not found at: " + KEY_PATH);
        }
    }

    public static boolean keyCheck(String key) {
        return key.length() == 64;
    }

    public static void startCLI() {
        System.out.println(Color.YELLOW + """
            ============================================================
                          WIZ HELPER — SETUP TOOL ONLY
            ============================================================
            This setup tool is intended for local, private use only.
            Do NOT use in shared or production environments.
            ============================================================
            """ + Color.RESET);
        try {
            while (true) {
                System.out.println("\nOptions:");
                System.out.println("1. Generate and save WizKey (Unencrypted)");
                System.out.println("2. Generate and save WizKey (Encrypted)");
                System.out.println("3. Enter & Save Private Key (Unencrypted)");
                System.out.println("4. Enter & Save Private Key (Encrypted)");
                System.out.println("5. Show Saved Key");
                System.out.println("6. Show Encrypted Key (Decrypt with Pass)");
                System.out.println("7. Exit");
                System.out.print("Choose: ");
                String input = scanner.nextLine().trim();
                switch (input) {
                    case "1" -> genWizAndSave();
                    case "2" -> genWizAndSaveEncrypted();
                    case "3" -> userWizAndSave();
                    case "4" -> userWizAndSaveEncrypted();
                    case "5" -> displayKey();
                    case "6" -> displayUnencryptedKey();
                    case "7" -> {
                        System.out.println("Goodbye.");
                        return;
                    }
                    default -> System.out.println("Invalid option.");
                }
            }
        } catch (Exception e) {
            System.err.println("ERROR:");
            e.printStackTrace();
        }
    }
}
