package com.beanchainbeta.nodePortal;

import java.security.PrivateKey;

import com.beanchainbeta.config.ConfigLoader;
import com.beanchainbeta.network.Node;
import com.bean_core.crypto.*;

public class adminCube {
    public String privateKeyHex;
    private PrivateKey privateKey;
    public String publicKeyHex;
    public String address;
    public String nodeIp;
    public boolean signedIn = false;
    
    public adminCube(String privateKeyHex, String publicIp) throws Exception {
    this.privateKeyHex = privateKeyHex;
    this.privateKey = WalletGenerator.restorePrivateKey(privateKeyHex);
    publicKeyHex = WalletGenerator.generatePublicKey(privateKey);
    address = WalletGenerator.generateAddress(publicKeyHex);
    nodeIp = publicIp;

    Node.initialize(nodeIp); // set static instance
    Node node = Node.getInstance(); // safe fetch

    Thread nodeThread = new Thread(() -> {
        node.start();

        // If NOT a bootstrap node, connect to bootstrap peer
        if (!ConfigLoader.isBootstrapNode) {
            try {
                System.out.println("Connecting to bootstrap node at " + ConfigLoader.bootstrapIp);
                node.connectToPeer(ConfigLoader.bootstrapIp);
            } catch (Exception e) {
                System.err.println("Failed to connect to bootstrap node: " + e.getMessage());
            }
        } else {
            System.out.println("Bootstrap node ready — listening for peers...");
        }

    }, "NodeThread");

    nodeThread.setDaemon(false);
    nodeThread.start();

}

}
