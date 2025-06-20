package com.beanchainbeta.logger;

import org.tinylog.Logger;

import com.beanchainbeta.helpers.DevConfig;

public class BeanLoggerManager {
    
    /**
     * takes 'log' message 
     * prints to console in DevMode 
     * logs as 'INFO' in log file
     * @param log
     */
    public static void BeanLogger(String log) {
        if (DevConfig.devMode) {
            System.out.println(log);
        }
        Logger.info(log);
    }

    /**
     * takes 'log' message 
     * Force prints to console ignores devMode settings
     * logs as 'INFO' in log file
     * @param log
     */
    public static void BeanLoggerFPrint(String log) {
        System.out.println(log);
        Logger.info(log);
    }

    /**
     * takes 'log' message 
     * prints to console in DevMode 
     * NO LOG to FILE
     * @param log
     */
    public static void BeanPrinter(String log) {
        if (DevConfig.devMode) {
            System.out.println(log);
        }
    }

    /**
     * takes 'log' message 
     * prints to console in DevMode 
     * logs as 'ERROR' in log file
     * @param log
     */
    public static void BeanLoggerError(String log) {
        if (DevConfig.devMode) {
            System.out.println(log);
        }
        Logger.info(log);
    }
}
