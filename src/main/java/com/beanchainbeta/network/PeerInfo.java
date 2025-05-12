package com.beanchainbeta.network;

import java.net.Socket;

public class PeerInfo {
    private final Socket socket;
    private final String address;
    private final String syncMode;
    private final String nodeType;
    private final boolean isValidator;

    public PeerInfo(Socket socket, String address, String syncMode, String nodeType, boolean isValidator) {
        this.socket = socket;
        this.address = address;
        this.syncMode = syncMode;
        this.nodeType = nodeType;
        this.isValidator = isValidator;
    }

    public Socket getSocket() {
        return socket;
    }

    public String getAddress() {
        return address;
    }

    public String getSyncMode() {
        return syncMode;
    }

    public String getNodeType() {
        return nodeType;
    }

    public boolean getIsValidator() {
        return isValidator;
    }

    public boolean isFullSync() {
        return !"TX_ONLY".equalsIgnoreCase(syncMode);
    }
}