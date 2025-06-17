package com.beanchainbeta.config;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

import org.tinylog.Logger;



public class ConfigLoader {
    private static String privateKeyPath;
    private static boolean encryptedWiz;
    private static String adminPass;
    private static String bindAddress;
    private static int networkPort;
    private static int peerPort;
    private static boolean isBootstrapNode;
    private static String bootstrapIp;
    private static boolean isPublicNode;
    private static String chainDB;
    private static String stateDB;
    private static String mempoolDB;
    private static String rejectedDB;
    private static String layer2DB;
    private static String syncMode;
    private static String nodeType;

    public static void loadConfig() {
        Properties props = new Properties();
        try (FileInputStream fis = new FileInputStream("config.docs/beanchain.config.properties")) {
            props.load(fis);

            privateKeyPath = props.getProperty("privateKeyPath", "config.docs/wizard.txt");
            encryptedWiz = Boolean.parseBoolean(props.getProperty("encryptedWiz", "false")); // defaults to a non encrypted wiz key for general safe and private use
            adminPass = props.getProperty("adminPass", "admin"); // default encryption password is set to admin if left blank

            bindAddress = props.getProperty("bindAddress", "0.0.0.0");
            networkPort = Integer.parseInt(props.getProperty("networkPort", "6442"));
            peerPort = Integer.parseInt(props.getProperty("peerPort", "6442"));
            isBootstrapNode = Boolean.parseBoolean(props.getProperty("isBootstrapNode", "false"));
            isPublicNode = Boolean.parseBoolean(props.getProperty("isPublicNode", "false"));
            bootstrapIp = props.getProperty("bootstrapIp", "66.179.82.188"); //current DEVNET DEFAULT GPN
            syncMode = props.getProperty("syncMode", "FULL");
            nodeType = props.getProperty("nodeType", "BEANNODE");

            chainDB = props.getProperty("chainDB", "chainDB");
            stateDB = props.getProperty("stateDB", "stateDB");
            mempoolDB = props.getProperty("mempoolDB", "mempoolDB");
            rejectedDB = props.getProperty("rejectedDB", "rejectedDB");
            layer2DB = props.getProperty("layer2DB", "layer2DB");

        } catch (IOException e) {
            Logger.error("Failed to load BeanChain config: " + e.getMessage());
            //System.err.println("Failed to load BeanChain config: " + e.getMessage());
            System.exit(1);
        }
    }

    
    public static String getPrivateKeyPath() { return privateKeyPath; }
    public static boolean getEncryptedWiz() {return encryptedWiz;}
    public static String getAdminPass() {return adminPass;}
    public static String getBindAddress() { return bindAddress; }
    public static int getNetworkPort() { return networkPort; }
    public static int getPeerPort() { return peerPort; }
    public static boolean isBootstrapNode() { return isBootstrapNode; }
    public static boolean isPublicNode() { return isPublicNode; }
    public static String getBootstrapIp() { return bootstrapIp; }
    public static String getSyncMode() { return syncMode; }
    public static String getNodeType() { return nodeType; }

    public static String getChainDB() { return chainDB; }
    public static String getStateDB() { return stateDB; }
    public static String getMempoolDB() { return mempoolDB; }
    public static String getRejectedDB() { return rejectedDB; }
    public static String getLayer2DB() { return layer2DB; }
}


