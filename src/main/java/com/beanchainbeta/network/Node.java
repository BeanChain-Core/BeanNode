package com.beanchainbeta.network;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;


import com.beanchainbeta.config.ConfigLoader;
import com.beanchainbeta.logger.BeanLoggerManager;
import com.beanchainbeta.nodePortal.portal;
import com.beanchainbeta.services.blockchainDB;
import com.beanpack.Block.Block;
import com.beanpack.TXs.CENCALL;
import com.beanpack.TXs.TX;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.*;

public class Node {
    static {
        ConfigLoader.loadConfig(); // ✅ runs BEFORE static fields or main()
        
    }
    private int port = ConfigLoader.getNetworkPort();
    private final int peerPort = ConfigLoader.getPeerPort();
    private String ip;
    private final ServerSocket serverSocket;
    private final List<String> knownAddresses = new CopyOnWriteArrayList<>();
    private static Node instance;
    private static String syncMode = ConfigLoader.getSyncMode();
    private static String nodeType = ConfigLoader.getNodeType();
    private static ConcurrentHashMap<Socket, PeerInfo> peers = new ConcurrentHashMap<>();
    private static ConcurrentHashMap<Socket, PeerInfo> cenRegistry = new ConcurrentHashMap<>();
    private static final String SAVED_PEERS_FILE = "config.docs/saved_peers.json";

    public static void initialize(String ip) throws IOException {
        if (instance == null) {
            instance = new Node(ip);
        }
    }

    public static Node getInstance() {
        return instance;
    }

    public Node(String ip) throws IOException {
        this.ip = ip;
        InetAddress bindAddress = ip.equals("0.0.0.0") ? InetAddress.getByName("0.0.0.0") : InetAddress.getByName(ip);
        this.serverSocket = new ServerSocket(port, 100, bindAddress);
    }

    public void start() {
        BeanLoggerManager.BeanLoggerFPrint("NodeBeta listening on: " + ip + ":" + port);
        new Thread(this::listenForPeers).start();
    }

    public void listenForPeers() {
        while (true) {
            try {
                Socket peer = serverSocket.accept();
                BeanLoggerManager.BeanLoggerFPrint("New peer connected: " + peer.getInetAddress().getHostAddress());
                new Thread(() -> handleIncomingMessages(peer)).start();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void handleIncomingMessages(Socket peer) {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(peer.getInputStream()))) {
            ObjectMapper mapper = new ObjectMapper();
            MessageRouter router = new MessageRouter();
            String line;
            while ((line = in.readLine()) != null) {
                try {
                    JsonNode message = mapper.readTree(line);
                    router.route(message, peer);
                } catch (Exception e) {
                    System.err.println("Failed to parse incoming JSON: " + line);
                }
            }
        } catch (IOException e) {
            BeanLoggerManager.BeanLoggerFPrint("Connection lost with peer: " + peer.getInetAddress());
            peers.remove(peer);
        } finally {
            try {
                peer.close();
            } catch (IOException ignored) {}
            peers.remove(peer);
        }
    }

    public void broadcast(String message, ArrayList<Socket> peersToSendTo) {
        for (Socket peer : peersToSendTo) {
            try {
                if (!peer.isClosed() && peer.isConnected()) {
                    PrintWriter out = new PrintWriter(peer.getOutputStream(), true);
                    out.println(message);
                } else {
                    peers.remove(peer);
                }
            } catch (IOException e) {
                System.err.println("Failed to broadcast message to peer: " + peer.getInetAddress());
                peers.remove(peer);
            }
        }
    }

    public void broadcastGossip(String message, ArrayList<Socket> peersToSendTo, Socket senderPeer) {
        if (senderPeer == null) {
            broadcast(message, peersToSendTo);
            return;
        }

        String senderIP = senderPeer.getInetAddress().getHostAddress();
        for (Socket peer : peersToSendTo) {
            String peerIP = peer.getInetAddress().getHostAddress();

            if(peerIP.equals(senderIP)){
                BeanLoggerManager.BeanLogger("Skipped Gossiping to sender at: " + senderIP);
                continue;
            }
            
            try {
                if (!peer.isClosed() && peer.isConnected()) {
                    PrintWriter out = new PrintWriter(peer.getOutputStream(), true);
                    out.println(message);
                } else {
                    peers.remove(peer);
                }
            } catch (IOException e) {
                System.err.println("Failed to broadcast message to peer: " + peer.getInetAddress());
                peers.remove(peer);
            }
        }
    }

    public void broadcastTransaction(TX tx) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            ObjectNode message = mapper.createObjectNode();
            message.put("type", "transaction");
            message.set("payload", mapper.readTree(tx.createJSON()));
            String jsonMessage = mapper.writeValueAsString(message);
            broadcast(jsonMessage, getSocketsByNodeType("BEANNODE"));
            broadcast(jsonMessage, getSocketsByNodeType("RN"));
        } catch (Exception e) {
            System.err.println("Failed to broadcast transaction:");
            e.printStackTrace();
        }
    }

    public static void broadcastTransactionStatic(TX tx) {
        if (instance != null) {
            instance.broadcastTransaction(tx);
        }
    }

    public static void broadcastBlock(Block block) {
        if (instance != null) {
            instance.instanceBroadcastBlock(block);
        }
    }

    private void instanceBroadcastBlock(Block block) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            ObjectNode message = mapper.createObjectNode();
            message.put("type", "block");
            message.set("payload", mapper.readTree(block.createJSON()));
            String blockMessage = mapper.writeValueAsString(message);
    
            for (Map.Entry<Socket, PeerInfo> entry : peers.entrySet()) {
                Socket socket = entry.getKey();
                PeerInfo info = entry.getValue();
    
                if (!socket.isClosed() && socket.isConnected()) {
                    PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                    out.println(blockMessage);
                } else {
                    peers.remove(socket); 
                }
            }
    
        } catch (Exception e) {
            System.err.println("Failed to broadcast block:");
            e.printStackTrace();
        }
    }

    public  List<String> getKnownPeers() {
        return knownAddresses;
    }

    public void connectToPeer(String host) {
        try {
            Socket socket = new Socket(host, peerPort);
            knownAddresses.add(host + ":" + peerPort);
            BeanLoggerManager.BeanLoggerFPrint("Connected to peer: " + host + ":" + peerPort);
            sendHandshake(socket);
            new Thread(() -> handleIncomingMessages(socket)).start();
        } catch (IOException e) {
            BeanLoggerManager.BeanLoggerFPrint("Failed to connect to peer at " + host + ":" + peerPort);
        }
    }

    public void connectToPeer(String host, int newPeerPort) {
        try {
            Socket socket = new Socket(host, newPeerPort);
            knownAddresses.add(host + ":" + newPeerPort);
            BeanLoggerManager.BeanLoggerFPrint("Connected to peer: " + host + ":" + newPeerPort);
            sendHandshake(socket);
            new Thread(() -> handleIncomingMessages(socket)).start();
        } catch (IOException e) {
            BeanLoggerManager.BeanLoggerFPrint("Failed to connect to peer at " + host + ":" + newPeerPort);
        }
    }

    public void connectToPeer(String host, int newPeerPort, boolean sync) {
        try {
            Socket socket = new Socket(host, newPeerPort);
            knownAddresses.add(host + ":" + newPeerPort);
            BeanLoggerManager.BeanLoggerFPrint("Connected to peer: " + host + ":" + newPeerPort);
            sendHandshake(socket, sync);
            new Thread(() -> handleIncomingMessages(socket)).start();
        } catch (IOException e) {
            BeanLoggerManager.BeanLoggerFPrint("Failed to connect to peer at " + host + ":" + newPeerPort);
        }
    }

    private void sendHandshake(Socket socket) {
        try {
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            ObjectMapper mapper = new ObjectMapper();
            ObjectNode handshake = mapper.createObjectNode();
            handshake.put("type", "handshake");
            handshake.put("blockHeight", blockchainDB.getHeight());
            handshake.put("requestSync", true);
            handshake.put("address", portal.admin.address);
            handshake.put("syncMode", syncMode);
            handshake.put("nodeType", nodeType);
            handshake.put("networkPort", port);
            handshake.put("reply", false);
            if(nodeType.equals("BEANNODE")){
                handshake.put("isValidator", true);
            } else {
                handshake.put("isValidator", false);
            }
            out.println(mapper.writeValueAsString(handshake));
        } catch (IOException e) {
            BeanLoggerManager.BeanLoggerFPrint("Failed to send handshake");
        }
    }

    private void sendHandshake(Socket socket, boolean sync) {
        try {
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            ObjectMapper mapper = new ObjectMapper();
            ObjectNode handshake = mapper.createObjectNode();
            handshake.put("type", "handshake");
            handshake.put("blockHeight", blockchainDB.getHeight());
            handshake.put("requestSync", sync);
            handshake.put("address", portal.admin.address);
            handshake.put("syncMode", syncMode);
            handshake.put("nodeType", nodeType);
            handshake.put("networkPort", port);
            handshake.put("reply", false);
            if(nodeType.equals("BEANNODE")){
                handshake.put("isValidator", true);
            } else {
                handshake.put("isValidator", false);
            }
            out.println(mapper.writeValueAsString(handshake));
        } catch (IOException e) {
            BeanLoggerManager.BeanLoggerFPrint("Failed to send handshake");
        }
    }

    public static void registerPeer(Socket socket, PeerInfo info) {
        String ip = socket.getInetAddress().getHostAddress();
        int listeningPort = info.getListeningPort();
        appendPeer(ip, listeningPort);
        if(info.getNodeType().equals("BEANNODE")){
            peers.put(socket, info);
        } else if (info.getNodeType().equals("CEN")){
            cenRegistry.put(socket, info);
        } else if (info.getNodeType().equals("RN")){
            BeanLoggerManager.BeanLoggerError("RN IS CONNECTED AS PEER (THIS NODE SHOULD BE GPN ONLY)");
            peers.put(socket, info);
        }
        
    }

    public static Collection<PeerInfo> getConnectedPeers() {
        return peers.values();
    }

    public static Collection<PeerInfo> getConnectedCENs(){
        return cenRegistry.values();
    }

    public static List<PeerInfo> getActiveValidators() {
    return peers.values().stream()
        .filter(PeerInfo::getIsValidator)
        .collect(Collectors.toList());
    }

    public static ArrayList<Socket> getSocketsByNodeType(String nodeType) {
        return new ArrayList<>(
            peers.entrySet().stream()
                .filter(entry -> entry.getValue().getNodeType().equalsIgnoreCase(nodeType))
                .map(Map.Entry::getKey)
                .collect(Collectors.toList())
        );
    }


    

    public static void broadcastRejection(String txHash) {
        if (instance != null) {
            instance.broadcastRejectionInternal(txHash);
        }
    }

    private void broadcastRejectionInternal(String txHash) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            ObjectNode message = mapper.createObjectNode();
            message.put("type", "tx_rejected");
    
            ObjectNode payload = mapper.createObjectNode();
            payload.put("txHash", txHash);
    
            message.set("payload", payload);
            String jsonMessage = mapper.writeValueAsString(message);
    
            broadcast(jsonMessage, getSocketsByNodeType("BEANNODE"));
            BeanLoggerManager.BeanLogger("Broadcasted rejection for TX: " + txHash);
        } catch (Exception e) {
            BeanLoggerManager.BeanLoggerError("Failed to broadcast rejection gossip:");
            e.printStackTrace();
        }
    }

    public static void broadcastCENCALL(String cenIP, CENCALL call) {
        if (instance != null) {
            instance.broadcastCENCALLInternal(cenIP, call);
        }
    }

    public static void printPeers() {
        if (peers.isEmpty()) {
            System.out.println("No connected peers.");
            return;
        }

        System.out.println("Connected Peers:");
        for (Map.Entry<Socket, PeerInfo> entry : peers.entrySet()) {
            Socket socket = entry.getKey();
            PeerInfo info = entry.getValue();

            String ip = socket.getInetAddress().getHostAddress();
            int port = info.getListeningPort();

            System.out.println("- " + ip + ":" + port + " | Info: " + info.stringInfo());
        }
    }

    public static void savePeers() {
        List<SerializedPeer> savablePeers = new ArrayList<>();

        //checks if empty or only connected to bootstrap
        if (peers.isEmpty() || peers.size() == 1) {
            System.out.println("No connected peers.");
            return;
        }

        try{
            System.out.println("[PEERLIST] Saving Peers...");
            for (Map.Entry<Socket, PeerInfo> entry : peers.entrySet()) {
                Socket socket = entry.getKey();
                PeerInfo info = entry.getValue();

                String ip = socket.getInetAddress().getHostAddress();
                int port = info.getListeningPort();
                if(ip.equals(ConfigLoader.getBootstrapIp())){
                    continue;
                }

                SerializedPeer serializedPeer = new SerializedPeer(ip, port);
                savablePeers.add(serializedPeer);
            }
            new ObjectMapper().writeValue(new File(SAVED_PEERS_FILE), savablePeers);
            BeanLoggerManager.BeanLoggerFPrint("[PEERLIST] Saved " + savablePeers.size() + " peers.");

        } catch (Exception e) {
            BeanLoggerManager.BeanLoggerFPrint("[PEERLIST] Failed to Save Peers");
            e.printStackTrace();
        }

    }

    public  void loadPeers() {
        try {
            File file = new File(SAVED_PEERS_FILE);

            if (!file.exists()) {
                BeanLoggerManager.BeanLogger("[PEERLIST] No saved peers found.");
                return;
            }

            ObjectMapper mapper = new ObjectMapper();
            List<SerializedPeer> savedPeers = Arrays.asList(mapper.readValue(file, SerializedPeer[].class));

            if (savedPeers.isEmpty()) {
                BeanLoggerManager.BeanLogger("[PEERLIST] Saved peer list is empty.");
                return;
            }

            System.out.println("[PEERLIST] Attempting to reconnect to " + savedPeers.size() + " saved peers...");
            for (SerializedPeer peer : savedPeers) {
                try {
                    connectToPeer(peer.ip, peer.listeningPort, false);
                } catch (Exception e) {
                    System.err.println("[PEERLIST] Failed to connect to: " + peer.ip + ":" + peer.listeningPort);
                }
            }

        } catch (Exception e) {
            BeanLoggerManager.BeanLoggerFPrint("[PEERLIST] Failed to load saved peers.");
            e.printStackTrace();
        }
    }

    public static void appendPeer(String ip, int port) {
        if(ip.equals(ConfigLoader.getBootstrapIp())){
            System.out.println("Peer is Bootstrap, not saved in peer list.");
            return;
        }
        try {
            File file = new File(SAVED_PEERS_FILE);
            ObjectMapper mapper = new ObjectMapper();
            List<SerializedPeer> savedPeers = new ArrayList<>();

            if (file.exists()) {
                savedPeers = new ArrayList<>(Arrays.asList(mapper.readValue(file, SerializedPeer[].class)));
                for (SerializedPeer peer : savedPeers) {
                    if (peer.ip.equals(ip) && peer.listeningPort == port) {
                        BeanLoggerManager.BeanLogger("[PEERLIST] Peer already in list: " + ip + ":" + port);
                        return;
                    }
                }
            }

            savedPeers.add(new SerializedPeer(ip, port));
            mapper.writeValue(file, savedPeers);
            BeanLoggerManager.BeanLogger("[PEERLIST] Appended new peer: " + ip + ":" + port);

        } catch (Exception e) {
            BeanLoggerManager.BeanLoggerFPrint("[PEERLIST] Failed to append peer.");
            e.printStackTrace();
        }
    }


    public int getPort(){
        return port;
    }


    //TODO:need to add a mempool or DB fallback for failed CENCALLs to retry then dropoff when timeout (should probably also record failed cencalls)
    private void broadcastCENCALLInternal(String cenIP, CENCALL call) {
        try {
            Socket socket = new Socket(cenIP, 6444);
    
            if (socket == null || socket.isClosed()) {
                System.err.println("No open connection to peer at " + cenIP);
                return;
            }
    
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
    
            ObjectMapper mapper = new ObjectMapper();
            ObjectNode message = mapper.createObjectNode();
            message.put("type", "cencall");
            message.set("payload", mapper.readTree(call.toJSON()));
    
            String jsonMessage = mapper.writeValueAsString(message);
            out.println(jsonMessage);
    
            BeanLoggerManager.BeanLogger("Sent CENCALL to peer at " + cenIP);
        } catch (Exception e) {
            BeanLoggerManager.BeanLoggerFPrint("Failed to send CENCALL to peer at " + cenIP);
            e.printStackTrace();
        }
    }
}

