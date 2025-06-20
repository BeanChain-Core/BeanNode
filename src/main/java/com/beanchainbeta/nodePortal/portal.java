package com.beanchainbeta.nodePortal;


import java.time.LocalDateTime;

import org.springframework.boot.autoconfigure.SpringBootApplication;

import com.beanchainbeta.config.ConfigLoader;
import com.beanchainbeta.controllers.CLIManager;
import com.beanchainbeta.logger.BeanLoggerManager;
//import com.beanchainbeta.devTests.TXTestBatcher;
import com.beanchainbeta.services.MempoolSyncService;
import com.beanchainbeta.services.blockchainDB;
import com.beanchainbeta.startScripts.autoStartGPN;
import com.beanchainbeta.startScripts.autoStartPrivate;
import com.beanchainbeta.startScripts.autoStartPublic;
import org.tinylog.Logger;

@SpringBootApplication
public class portal {
    static {
        try{
            Logger.info("NODE BOOT");
            ConfigLoader.loadConfig(); // runs BEFORE static fields or main()
            System.out.println("Config-----------loaded");
        } catch (Exception e){
            System.out.println("Config-----------failed");
            e.printStackTrace();
        }
    }
    public static adminCube admin;
    public static blockchainDB beanchainTest = new blockchainDB();
    public static volatile boolean isSyncing = false;
    public static final long BOOT_TIME = System.currentTimeMillis();


    public static void setIsSyncing(boolean bool) {isSyncing = bool;}


    public static void main(String[] args) throws Exception {

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
        //TXTestBatcher.loadMemPool(); // FIXME: this is a test TX for dev testing do not leave this line in production!

        CLIManager.startConsole(); //starts CLIManager in new thread
    }

    
}
