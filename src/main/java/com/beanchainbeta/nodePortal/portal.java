package com.beanchainbeta.nodePortal;


import org.springframework.boot.autoconfigure.SpringBootApplication;

import com.bean_core.Block.Block;
import com.beanchainbeta.config.ConfigLoader;
import com.beanchainbeta.services.MempoolSyncService;
import com.beanchainbeta.services.blockchainDB;
import com.beanchainbeta.startScripts.autoStartGPN;
import com.beanchainbeta.startScripts.autoStartPrivate;
import com.beanchainbeta.startScripts.autoStartPublic;

@SpringBootApplication
public class portal {
    static {
        ConfigLoader.loadConfig(); // ✅ runs BEFORE static fields or main()
        System.out.println("✅ Config loaded (from static block)");
    }
    public static final String currentVersion = "(BETA)";
    public static adminCube admin;
    public static blockchainDB beanchainTest = new blockchainDB();
    public static volatile boolean isSyncing = true;
    public static final long BOOT_TIME = System.currentTimeMillis();


    public static void setIsSyncing(boolean bool) {isSyncing = bool;}


    public static void main(String[] args) throws Exception {
        
        System.out.println("Block class version: " + Block.class.getDeclaredFields().length);

        if(ConfigLoader.isBootstrapNode()) {
            autoStartGPN.nodeStart();
            setIsSyncing(false);
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
        
        
    }

    
}
