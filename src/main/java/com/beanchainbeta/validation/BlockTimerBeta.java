package com.beanchainbeta.validation;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import com.beanpack.beanify.Branding;
import com.beanchainbeta.logger.BeanLoggerManager;
import com.beanchainbeta.nodePortal.portal;


public class BlockTimerBeta {

    private static final AtomicInteger counter = new AtomicInteger(0);

    public static void nodeFleccer(){
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

        Runnable task = () -> {
            try {
                int count = counter.incrementAndGet();
                BlockBuilderV2.blockMaker(portal.admin.privateKeyHex); 

                if(count % 10 == 0) {
                    System.out.println("\u001B[32m" + Branding.logo + "\u001B[0m");
                }
            } catch (Exception e) {
                System.err.println("Error in blockMaker: " + e.getMessage());
                e.printStackTrace();
            }
        };
        scheduler.scheduleAtFixedRate(task, 1, 1, TimeUnit.MINUTES);
        BeanLoggerManager.BeanLoggerFPrint("[nodeFleccer] Block timer started.");
    }

    
    
}
