package io.beanchain.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.Scanner;

import org.tinylog.Logger;

import io.beanchain.helpers.SecureInputHelper;



public class ConfigLoader {
    private static String configPath = "config.docs/beanchain.config.properties";
    private static String configEnc = "config.docs/beanchain.config.properties.enc";
    private static String privateKeyPath;
    private static boolean encryptedWiz;
    private static boolean requirePass;
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
    private static String keyName;
    private static String token;
    private static Boolean openLocal;
    private static String mode;

    public static void loadConfig() {
        File encConfig = new File(configEnc);
        if (encConfig.exists()){
            System.out.println("Config stuck in runtime encryption* PLEASE REMAKE 'beanchain.config.properties.");
        }
        
        File configFile = new File(configPath);
        if (!configFile.exists()) {
            System.out.println("Config file not found — generating default config...");
            createDefaultConfig(configFile);
        }
        Properties props = new Properties();
        try (FileInputStream fis = new FileInputStream(configPath)) {
            props.load(fis);

            privateKeyPath = props.getProperty("privateKeyPath", "config.docs/wiz.txt");
            keyName = props.getProperty("keyName", "wiz.txt.enc");
            encryptedWiz = Boolean.parseBoolean(props.getProperty("encryptedWiz", "false")); // defaults to a non encrypted wiz key for general safe and private use
            requirePass = Boolean.parseBoolean(props.getProperty("requirePass", "false"));
            adminPass = props.getProperty("adminPass", "admin"); // default encryption password is set to admin if left blank

            token = props.getProperty("token.cli", "beanADMIN42"); // TOKEN FOR CLI LOCAL ACCESS
            openLocal = Boolean.parseBoolean(props.getProperty("openLocal", "false")); // set access to allow or block local commands 

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

            mode = props.getProperty("mode", "portal");
            

        } catch (IOException e) {
            Logger.error("Failed to load BeanChain config: " + e.getMessage());
            //System.err.println("Failed to load BeanChain config: " + e.getMessage());
            System.exit(1);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            //e.printStackTrace();
        }
    }

    //TODO: CURRENT! UPDATE DEFAULT CONFIG BUILDER + SEPERATE TO JAR TOOL FOR EASY CONFIG SETUP

    private static void createDefaultConfig(File file) {
        Boolean encryptBool = false;
        Boolean requireBool = false;
        String pass = "admin";
        String port = "6442";
        Boolean publicBool = false;
        Scanner scanner = new Scanner(System.in);
        System.out.println("Using an encrypted wiz key? (y/n)");
        String isEncrypted = scanner.nextLine().trim();
        switch(isEncrypted){
            case "y":
                encryptBool = true;
                System.out.println("Now configured for encrypted Key");
                break;
            case "n":
                break;
            default:
                System.out.println("UNKNOWN COMMAND: set to default: 'false'");
                break;
        }

        if (encryptBool) {
            System.out.println("Do you want to hardcode your encryption password in [config] or [require] at boot (enter: 'config' or 'require')");
            String require = scanner.nextLine().trim();
            switch(require){
                case "config":
                    pass = SecureInputHelper.promptHidden("Enter Your Encryption Pass");
                    break;
                case "require":
                    requireBool = true;
                    break;
                default :
                    System.out.println("Unknown Command. Default 'admin' saved to config."); 
            } 
        }

        System.out.println("Change your nodes port from '6442' (y/anything else for no)");
        String portChange = scanner.nextLine().trim();
        switch(portChange){
            case "y":
                System.out.println("Enter Port Number");
                port = scanner.nextLine().trim();
                break;
            default:
                break;
        }

        System.out.println("Open Nodes public APIs? (y/anything else for no)");
        String publicB = scanner.nextLine().trim();
        switch(publicB){
            case "y":
                publicBool = true;
                break;
            default:
                break;
        }

        
        Properties defaults = new Properties();

        defaults.setProperty("privateKeyPath", "config.docs/wiz.txt");
        defaults.setProperty("keyName", "wiz.txt.enc");
        defaults.setProperty("encryptedWiz", String.valueOf(encryptBool));
        defaults.setProperty("requirePass", String.valueOf(requireBool));
        defaults.setProperty("adminPass", pass);
        defaults.setProperty("bindAddress", "0.0.0.0");
        defaults.setProperty("networkPort", port);
        defaults.setProperty("peerPort", "6442");
        defaults.setProperty("isBootstrapNode", "false");
        defaults.setProperty("bootstrapIp", "66.179.82.188");
        defaults.setProperty("syncMode", "FULL");
        defaults.setProperty("nodeType", "BEANNODE");
        defaults.setProperty("isPublicNode", String.valueOf(publicBool));
       
        defaults.setProperty("chainDB", "chainDB");
        defaults.setProperty("stateDB", "stateDB");
        defaults.setProperty("mempoolDB", "mempoolDB");
        defaults.setProperty("rejectedDB", "rejectedDB");
        defaults.setProperty("layer2DB", "layer2DB");
        

        try {
            file.getParentFile().mkdirs(); // Create folder if missing
            try (FileOutputStream fos = new FileOutputStream(file)) {
                defaults.store(fos, "Default BeanChain Config - auto generated");
            }
            loadConfig(); //may not need (redundant?)
        } catch (IOException e) {
            System.err.println("Failed to create default config: " + e.getMessage());
        }
    }

    
    public static String getPrivateKeyPath() { return privateKeyPath; }
    public static boolean getEncryptedWiz() {return encryptedWiz;}
    public static boolean getRequirePass() {return requirePass;}
    public static String getAdminPass() {return adminPass;}
    public static String getBindAddress() { return bindAddress; }
    public static int getNetworkPort() { return networkPort; }
    public static int getPeerPort() { return peerPort; }
    public static boolean isBootstrapNode() { return isBootstrapNode; }
    public static boolean isPublicNode() { return isPublicNode; }
    public static String getBootstrapIp() { return bootstrapIp; }
    public static String getSyncMode() { return syncMode; }
    public static String getNodeType() { return nodeType; }
    public static String getToken() { return token; }
    public static Boolean getOpenLocal() { return openLocal;}

    public static String getMode() { return mode; }

    public static String getChainDB() { return chainDB; }
    public static String getStateDB() { return stateDB; }
    public static String getMempoolDB() { return mempoolDB; }
    public static String getRejectedDB() { return rejectedDB; }
    public static String getLayer2DB() { return layer2DB; }
    public static String getKeyName() { return keyName; }

    public static void setAdminPass(String pass) {adminPass = pass;}
    public static void setRequirePass(Boolean require) {requirePass = require; }
}


