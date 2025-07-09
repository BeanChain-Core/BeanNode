package io.beanchain.helpers;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

public class Hasher {
    
    public static String generateHash(String data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data.getBytes(StandardCharsets.UTF_8));

            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                hexString.append(String.format("%02x", b));
            }
            return hexString.toString();
        } catch (Exception e) {
            throw new RuntimeException("[Pong] Error generating hash", e);
        }
    }
}
