package com.beanchainbeta.network;

public class SerializedPeer {
    public String ip;
    public int listeningPort;

    public SerializedPeer(){}

    public SerializedPeer(String ip, int listeningPort){
        this.ip = ip;
        this.listeningPort = listeningPort;

    }
    
}
