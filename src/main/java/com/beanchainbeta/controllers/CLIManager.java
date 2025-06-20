package com.beanchainbeta.controllers;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Properties;
import java.util.Scanner;

import com.beanchainbeta.helpers.DevConfig;
import com.beanchainbeta.network.Node;
import com.beanchainbeta.nodePortal.portal;

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
        new Thread(() -> {
            Scanner scanner = new Scanner(System.in);
            while (true) {
                System.out.print(rootUserName + ">> ");
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

                    case "help":
                        System.out.println("Available commands:");
                        System.out.println("  connect <ip> <port>   - Connect to a peer at the given IP and port.");
                        System.out.println("  print peers           - Print all currently connected peers.");
                        System.out.println("  devmode               - Toggle DevMode on/off (reduces console output during sync).");
                        System.out.println("  username              - Change Node portal username.");
                        System.out.println("  exit                  - Shut down the node.");
                        //System.out.println("  send bean <address> <amount> <gas fee> - Send BEAN to a wallet with specified gas fee.");
                        System.out.println("  help                  - Show this list of commands.");
                        break;

                    default:
                        System.out.println("Unknown command.");
                        break;
                }
            }
        }).start();
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
