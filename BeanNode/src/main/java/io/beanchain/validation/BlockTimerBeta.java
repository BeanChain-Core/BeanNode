package io.beanchain.validation;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import com.beanpack.beanify.Branding;
import io.beanchain.logger.BeanLoggerManager;
import io.beanchain.nodePortal.portal;


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
