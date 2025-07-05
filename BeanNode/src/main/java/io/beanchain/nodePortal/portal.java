package io.beanchain.nodePortal;


import java.io.File;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import io.beanchain.config.ConfigLoader;
import io.beanchain.controllers.CLIManager;
import io.beanchain.helpers.SecureInputHelper;
import io.beanchain.network.Node;
import io.beanchain.services.MempoolSyncService;
import io.beanchain.services.blockchainDB;
import io.beanchain.startScripts.autoStartGPN;
import io.beanchain.startScripts.autoStartPrivate;
import io.beanchain.startScripts.autoStartPublic;
import com.beanpack.Wizard.WizCryptHandler;
import com.beanpack.beanify.Color;

import org.tinylog.Logger;

@SpringBootApplication
public class portal {
    static {
        try{
            Logger.info("NODE BOOT");
            ConfigLoader.loadConfig(); // runs BEFORE static fields or main()
            System.out.println("[*] CONFIG FILE LOADED ............ SUCCESS");
        } catch (Exception e){
            System.out.println("[!] CONFIG LOAD FAILED ............ ERROR");
            e.printStackTrace();
        }
    }

    public static adminCube admin;
    
    public static blockchainDB beanchainTest = new blockchainDB();
    public static volatile boolean isSyncing = false;
    public static final long BOOT_TIME = System.currentTimeMillis();


    public static void setIsSyncing(boolean bool) {isSyncing = bool;}
    


    public static void main(String[] args) throws Exception {

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            WizCryptHandler.decryptConfig();
            System.out.println(Color.GREEN +
"##########################################################################\n" +
"# _____   ______  ______  ______  __  __  ______  ______                 #\n" +
"#/\\  __-./\\  ___\\/\\  ___\\/\\  == \\/\\ \\_\\ \\/\\  == \\/\\__  _\\                #\n" +
"#\\ \\ \\/\\ \\ \\  __\\\\ \\ \\___\\ \\  __<\\ \\____ \\ \\  _-/\\/_/\\ \\/                #\n" +
"# \\ \\____-\\ \\_____\\ \\_____\\ \\_\\ \\_\\/\\_____\\ \\_\\     \\ \\_\\                #\n" +
"#  \\/____/ \\/_____/\\/_____/\\/_/ /_/\\/_____/\\/_/      \\/_/                #\n" +
"#                                                                        #\n" +
"# ______  ______  __   __  ______  __  ______                            #\n" +
"#/\\  ___\\/\\  __ \\/\\ \"-.\\ \\/\\  ___\\/\\ \\/\\  ___\\                           #\n" +
"#\\ \\ \\___\\ \\ \\/\\ \\ \\ \\-.  \\ \\  __\\\\ \\ \\ \\ \\__ \\                          #\n" +
"# \\ \\_____\\ \\_____\\ \\_\\\"\\_\\ \\_\\   \\ \\_\\ \\_____\\                         #\n" +
"#  \\/_____/\\/_____/\\/_/ \\/_/\\/_/    \\/_/\\/_____/                         #\n" +
"#                                                                        #\n" +
"# ______  __  __  __  __  ______    _____   ______  __     __  __   __   #\n" +
"#/\\  ___\\/\\ \\_\\ \\/\\ \\/\\ \\/\\__  _\\  /\\  __-./\\  __ \\/\\ \\  _ \\ \\/\\ \"-.\\ \\  #\n" +
"#\\ \\___  \\ \\  __ \\ \\ \\_\\ \\/_/\\ \\/  \\ \\ \\/\\ \\ \\ \\/\\ \\ \\ \\/ \".\\ \\ \\ \\-.  \\ #\n" +
"# \\/_____\\ \\_\\ \\_\\ \\_____\\ \\ \\_\\   \\ \\____-\\ \\_____\\ \\__/\".~\\_\\ \\_\\\"\\_\\ #\n" +
"#  \\/_____/\\/_/\\/_/\\/_____/  \\/_/    \\/____/ \\/_____/\\/_/   \\/_/\\/_/ \\/_/ #\n" +
"#                                                                        #\n" +
"# ______  ______  ______  ______  __   __                                #\n" +
"#/\\  ___\\/\\  == \\/\\  ___\\/\\  __ \\/\\ \"-.\\ \\                               #\n" +
"#\\ \\ \\__ \\ \\  __<\\ \\  __\\\\ \\  __ \\ \\ \\-.  \\                              #\n" +
"# \\ \\_____\\ \\_____\\ \\_____\\ \\_\\ \\_\\ \\_\\\"\\_\\                             #\n" +
"#  \\/_____/\\/_____/\\/_____/\\/_/\\/_/\\/_/ \\/_/                             #\n" +
"##########################################################################" + Color.RESET);
            Node.savePeers(); // or node.savePeers() if not static
        }));

        String mode = ConfigLoader.getMode();

        switch(mode){
            case "headless":
                String headlessPass = System.getenv("BEAN_ADMIN_PASS");
                ConfigLoader.setAdminPass(headlessPass);
                break;
            case "portal":
                if(ConfigLoader.getRequirePass()){
                    String adminPass = SecureInputHelper.promptHidden("ENTER ADMIN PASS");
                    ConfigLoader.setAdminPass(adminPass);
                }
                break;
        }

        if(ConfigLoader.getEncryptedWiz()){
            WizCryptHandler.setPassword(ConfigLoader.getAdminPass());
            WizCryptHandler.setWizFileEnc("wiz.txt.enc");
            WizCryptHandler.setConfigFolder(new File("config.docs/"));
            WizCryptHandler.setKeyPath(ConfigLoader.getPrivateKeyPath());
            WizCryptHandler.bootWizCrypt(ConfigLoader.getAdminPass());
        }

        if(ConfigLoader.isBootstrapNode()) {
            autoStartGPN.nodeStart();
        } else if(ConfigLoader.isPublicNode()) {
            autoStartPublic.nodeStart();
        } else {
            autoStartPrivate.nodeStart();
        }
        
        
        Thread memGossipThread = new Thread(() -> {
                    MempoolSyncService.start();
                }, "memGossipThread");

        memGossipThread.setDaemon(false);
        memGossipThread.start();

        Node node = Node.getInstance();
        node.loadPeers();
        
        if(ConfigLoader.getMode().equals("portal")) {
            CLIManager.startConsole(); //starts CLIManager in new thread
        }
    }    
}
