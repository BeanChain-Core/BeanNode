package io.beanchain.factories;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.beanchain.helpers.Hasher;
import io.beanchain.logger.BeanLoggerManager;
import io.beanchain.nodePortal.portal;

import com.beanpack.Utils.hex;
import com.beanpack.crypto.SHA256TransactionSigner;
import com.beanpack.crypto.WalletGenerator;

import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.security.PrivateKey;

public class PongSender {
    private static final String RN_IP = "66.179.82.188";
    private static final int RN_PORT = 6443;

    public static void sendPongToRN(String pingNumber) {
        try {
            ObjectMapper mapper = new ObjectMapper();

            String publicKey = portal.admin.publicKeyHex;
            String hash = Hasher.generateHash(pingNumber + publicKey); 
            PrivateKey privateKey = WalletGenerator.restorePrivateKey(portal.admin.privateKeyHex);
            String signature = SHA256TransactionSigner.signSHA256Transaction(privateKey, hex.hexToBytes(hash));

            ObjectNode pong = mapper.createObjectNode();
            pong.put("type", "pong");

            ObjectNode payload = mapper.createObjectNode();
            payload.put("pingNumber", pingNumber);
            payload.put("publicKey", publicKey);
            payload.put("hash", hash);
            payload.put("signature", signature);

            pong.set("payload", payload);

            try (Socket socket = new Socket(RN_IP, RN_PORT)) {
                PrintWriter out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);
                out.println(mapper.writeValueAsString(pong));
                BeanLoggerManager.BeanLogger("[PONG] Sent pong for ping " + pingNumber + " to RN at " + RN_IP);
            }

        } catch (Exception e) {
            BeanLoggerManager.BeanLoggerError("[PONG] Failed to send pong to RN:");
            e.printStackTrace();
        }
    }
}
