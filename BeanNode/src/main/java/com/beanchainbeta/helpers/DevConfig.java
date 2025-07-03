package com.beanchainbeta.helpers;

public class DevConfig {
    public static boolean devMode = false; //allows the printing or silencing of block replay during sync

    public static void toggleDevMode(){
        if(devMode){
            devMode = false;
        } else if (!devMode){
            devMode = true;
        }
    }
}
