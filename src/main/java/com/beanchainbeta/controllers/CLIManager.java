package com.beanchainbeta.controllers;

import java.util.Scanner;

import com.beanchainbeta.network.Node;
import com.beanchainbeta.nodePortal.portal;

public class CLIManager {
    
    public static void startConsole() {
        Node node = Node.getInstance();
        new Thread(() -> {
            Scanner scanner = new Scanner(System.in);
            while (true) {
                System.out.print( portal.admin.address + ">> ");
                String input = scanner.nextLine().trim();

                if (input.startsWith("connect ")) {
                    String[] parts = input.split(" ");
                    if (parts.length == 3) {
                        node.connectToPeer(parts[1], Integer.parseInt(parts[2]));
                    } else {
                        System.out.println("Usage: connect <ip> <port>");
                    }
                } else if (input.equals("print peers")){
                    Node.printPeers();
                } else if (input.equals("exit")) {
                    System.out.println("Shutting down...");
                    System.exit(0);
                } else {
                    System.out.println("Unknown command.");
                }
            }
        }).start();
    }
}
