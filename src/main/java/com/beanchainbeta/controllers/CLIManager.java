package com.beanchainbeta.controllers;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputFilter.Config;
import java.util.Properties;
import java.util.Scanner;

import com.beanchainbeta.config.ConfigLoader;
import com.beanchainbeta.factories.InternalTXFactory;
import com.beanchainbeta.helpers.DevConfig;
import com.beanchainbeta.helpers.wizHelper;
import com.beanchainbeta.logger.BeanLoggerManager;
import com.beanchainbeta.network.Node;
import com.beanchainbeta.nodePortal.portal;
import com.beanchainbeta.services.Layer2DBService;
import com.beanchainbeta.services.WalletService;
import com.beanchainbeta.services.blockchainDB;
import com.beanpack.Wizard.wizard;

public class CLIManager {
    public static String rootUserName = portal.admin.address;
    
    public static void startConsole() {
        if (System.console() == null) {
            System.out.println("[BeanLog] *!* No interactive console detected. Skipping CLI manager startup.");
            return;
        }
        Node node = Node.getInstance();
        String fetched = getUsername();
        if (fetched != null) {
            rootUserName = fetched;
        }
        Thread cliThread = new Thread(() -> {
            Scanner scanner = new Scanner(System.in);
            while (true) {
                System.out.println("\u001B[32m");
                System.out.print(rootUserName + ">> \u001B[0m");

                String input = scanner.nextLine().trim();

                String[] parts = input.split(" ");
                String command = parts[0];

                switch (command) {
                    case "connect":
                        if (parts.length == 3) {
                            try {
                                String ip = parts[1];
                                int port = Integer.parseInt(parts[2]);
                                node.connectToPeer(ip, port, false);
                            } catch (NumberFormatException e) {
                                System.out.println("Port must be a number.");
                            }
                        } else {
                            System.out.println("Usage: connect <ip> <port>");
                        }
                        break;

                    case "print":
                        if (parts.length == 2 && parts[1].equals("peers")) {
                            Node.printPeers();
                        } else {
                            System.out.println("Usage: print peers");
                        }
                        break;

                    case "devmode":
                        DevConfig.toggleDevMode();
                        System.out.println("DevMode is now " + DevConfig.devMode);
                        break;

                    case "exit":
                        System.out.println("Shutting down...");
                        scanner.close();
                        System.exit(0);
                        break;

                    case "username":
                        System.out.print("Enter new node portal UserName: ");
                        String newName = scanner.nextLine().trim();
                        if (!newName.isBlank()) {
                            rootUserName = newName;
                            saveUsername(newName);
                        } else {
                            System.out.println("Username cannot be blank.");
                        }
                        break;

                    case "wallet": 
                        System.out.println(WalletService.getData(portal.admin.address));
                        break;

                    case "tokens": 
                        System.out.println(Layer2DBService.getOrCreateWallet(portal.admin.address).getTokenBalances());
                        break;

                    case "height":
                        System.out.println("Local Node Height: " + blockchainDB.getHeight());
                        break;

                    case "lastblock":
                        System.out.println(blockchainDB.getLatestBlock().createJSON());

                    case "send":
                        if (parts.length == 5 && parts[1].equalsIgnoreCase("bean")) {
                            String to = parts[2];
                            try {
                                double amount = Double.parseDouble(parts[3]);
                                double gas = Double.parseDouble(parts[4]);

                                // Example: you might already have some TX sender logic
                                boolean success = InternalTXFactory.sendAndSignTxInternal(to, amount, gas);
                                if (success) {
                                    System.out.println("TX submitted successfully.");
                                } else {
                                    System.out.println("TX submission failed.");
                                }
                            } catch (Exception e) {
                                System.out.println("Amount and gas fee must be valid numbers.");
                                e.printStackTrace();
                            }
                        } else {
                            System.out.println("Usage: send bean <address> <amount> <gas-fee>");
                        }
                        break;
                    case "display-key":
                        if(ConfigLoader.getEncryptedWiz()){
                            if(ConfigLoader.getRequirePass()){
                                try {
                                    System.out.println("Enter Admin Pass");
                                    String adminPass = scanner.nextLine().trim();
                                    String encrypted = wizard.wizardRead(ConfigLoader.getPrivateKeyPath());
                                    String unEncrypted = wizard.decryptWizKey(encrypted, adminPass);
                                    System.out.println("Private Key Hex: " +  unEncrypted);
                                } catch (Exception e) {
                                    System.out.println("ERROR* Failed to display encrypted key");
                                    e.printStackTrace();
                                }  
                            } else {
                                try {
                                    String encrypted = wizard.wizardRead(ConfigLoader.getPrivateKeyPath());
                                    String unEncrypted = wizard.decryptWizKey(encrypted, ConfigLoader.getAdminPass());
                                    System.out.println("Private Key Hex: " +  unEncrypted);
                                } catch (Exception e) {
                                    System.out.println("ERROR* Failed to display encrypted key");
                                    e.printStackTrace();
                                }  
                            }

                        } else {
                            try {
                                System.out.println("Private Key Hex: " +  wizard.wizardRead(ConfigLoader.getPrivateKeyPath()));
                            } catch (IOException e) {
                                System.out.println("ERROR* Failed to display key");
                                e.printStackTrace();
                            }
                        }
                        break;

                    case "help":
                        System.out.println("\n=== Available CLI Commands ===");
                        System.out.println(String.format("  %-40s %s", "connect <ip> <port>", "Connect to a peer at the given IP and port."));
                        System.out.println(String.format("  %-40s %s", "print peers", "Print all currently connected peers."));
                        System.out.println(String.format("  %-40s %s", "devmode", "Toggle DevMode on/off (less logging during sync)."));
                        System.out.println(String.format("  %-40s %s", "username", "Change Node portal username."));
                        System.out.println(String.format("  %-40s %s", "send bean <address> <amount> <gas-fee>", "Send BEAN to a wallet with specified gas fee."));
                        System.out.println(String.format("  %-40s %s", "wallet", "View your this node's Bean balance (in beantoshi)"));
                        System.out.println(String.format("  %-40s %s", "display-key", "Display your private key"));
                        System.out.println(String.format("  %-40s %s", "tokens", "View your this node's Token Balances (by hash, in Beantoshi)"));
                        System.out.println(String.format("  %-40s %s", "height", "View your local Node chain height."));
                        System.out.println(String.format("  %-40s %s", "lastblock", "View the last block stored in local chain."));
                        System.out.println(String.format("  %-40s %s", "exit", "Shut down the node."));
                        System.out.println(String.format("  %-40s %s", "help", "Show this list of commands."));
                        System.out.println(); // newline for clean output
                        break;

                    default:
                        System.out.println("Unknown command.");
                        break;
                }
            }
        }, "CLI");
        cliThread.start();
    }

    //Node Op helpers 

    public static void saveUsername(String username) {
        try {
            Properties props = new Properties();
            props.setProperty("username", username);

            try (FileWriter writer = new FileWriter("config.docs/nodeOp.properties")) {
                props.store(writer, "Node Operator Settings");
            }

            System.out.println("Username saved.");
        } catch (IOException e) {
            System.err.println("Failed to save username:");
            e.printStackTrace();
        }
    }

    public static String getUsername() {
        Properties props = new Properties();
        File configFile = new File("config.docs/nodeOp.properties");
        if (!configFile.exists()) {
            return null; // No username saved yet
        }
        try (FileReader reader = new FileReader("config.docs/nodeOp.properties")) {
            props.load(reader);
            return props.getProperty("username", null);
        } catch (IOException e) {
            System.err.println("Failed to load username:");
            e.printStackTrace();
            return "undefined";
        }
    }

}
