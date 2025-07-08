package io.beanchain.controllers;

import org.springframework.web.bind.annotation.*;

import io.beanchain.config.ConfigLoader;
import io.beanchain.factories.InternalTXFactory;
import io.beanchain.logger.BeanLoggerManager;
import io.beanchain.network.Node;
import io.beanchain.nodePortal.portal;
import io.beanchain.services.Layer2DBService;
import io.beanchain.services.WalletService;
import io.beanchain.services.blockchainDB;

import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;

import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/cli")
public class LocalAdminController {

    private String adminToken;
    private Boolean openLocal;

    @PostConstruct
    public void loadToken() {
        this.adminToken = ConfigLoader.getToken();
        this.openLocal = ConfigLoader.getOpenLocal();
    }
    private boolean isAuthorized(String token) {
        return token != null && token.equals(adminToken);
    }

    private boolean isLocal(HttpServletRequest request) {
        String remoteIP = request.getRemoteAddr();
        return remoteIP.equals("127.0.0.1")
            || remoteIP.equals("::1")
            || remoteIP.equals("0:0:0:0:0:0:0:1")     // expanded IPv6 loopback
            || remoteIP.startsWith("::ffff:127.")     // IPv4-mapped IPv6
            || remoteIP.startsWith("127.");           // full 127.0.0.0/8 range
    }


    @PostMapping("/command")
    public ResponseEntity<?> runCommand(
        @RequestParam String token,
        @RequestParam String command,
        HttpServletRequest request
    ) {
        if (!isLocal(request)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Local access only");
        }

        if (!isAuthorized(token)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid token");
        }

        if (!openLocal) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Open Local Configuration: OFF");
        }

        // Example command logic
        if (command.equalsIgnoreCase("shutdown")) {
            BeanLoggerManager.BeanLogger("Shutdown command received from LocalAdminController.");
            System.out.println("Shutting down BeanNode via CLI API...");
            new Thread(() -> {
                try { Thread.sleep(100); } catch (InterruptedException e) {}
                System.exit(0);
            }).start();
            return ResponseEntity.ok("Node is shutting down.");
        }

        return ResponseEntity.ok("Command received: " + command);
    }

    @GetMapping("/status")
    public ResponseEntity<?> getStatus(
        @RequestParam String token,
        HttpServletRequest request
    ) {
        if (!isLocal(request)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Local access only");
        }

        if (!isAuthorized(token)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid token");
        }

        if (!openLocal) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Open Local Configuration: OFF");
        }

        return ResponseEntity.ok("BeanNode CLI interface is active.");
    }

    @PostMapping("/connect")
    public ResponseEntity<?> connectToPeer(
        @RequestParam String token,
        @RequestParam String ip,
        @RequestParam int port,
        HttpServletRequest request
    ) {
        if (!isLocal(request)) return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Local access only");
        if (!isAuthorized(token)) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid token");
        if (!openLocal) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Open Local Configuration: OFF");

        try {
            Node.getInstance().connectToPeer(ip, port, false);
            return ResponseEntity.ok("Connection attempt sent to " + ip + ":" + port);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to connect: " + e.getMessage());
        }
    }


    @GetMapping("/height")
    public ResponseEntity<?> getHeight(@RequestParam String token, HttpServletRequest request) {
        if (!isLocal(request)) return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Local access only");
        if (!isAuthorized(token)) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid token");
        if (!openLocal) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Open Local Configuration: OFF");

        return ResponseEntity.ok("Local Node Height: " + blockchainDB.getHeight());
    }

    @GetMapping("/wallet")
    public ResponseEntity<?> getWallet(@RequestParam String token, HttpServletRequest request) {
        if (!isLocal(request)) return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Local access only");
        if (!isAuthorized(token)) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid token");
        if (!openLocal) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Open Local Configuration: OFF");

        return ResponseEntity.ok(WalletService.getData(portal.admin.address));
    }

    @GetMapping("/tokens")
    public ResponseEntity<?> getTokenBalances(@RequestParam String token, HttpServletRequest request) {
        if (!isLocal(request)) return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Local access only");
        if (!isAuthorized(token)) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid token");
        if (!openLocal) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Open Local Configuration: OFF");

        return ResponseEntity.ok(Layer2DBService.getOrCreateWallet(portal.admin.address).getTokenBalances());
    }

    @GetMapping("/lastblock")
    public ResponseEntity<?> getLastBlock(@RequestParam String token, HttpServletRequest request) {
        if (!isLocal(request)) return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Local access only");
        if (!isAuthorized(token)) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid token");
        if (!openLocal) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Open Local Configuration: OFF");

        return ResponseEntity.ok(blockchainDB.getLatestBlock().createJSON());
    }

    //TODO: ADDITIONS! WIP ADDITIONS
    // @GetMapping("/node.info")
    // public ResponseEntity<?> nodeInfor(@RequestParam String token, HttpServletRequest request) {
    //     if (!isLocal(request)) return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Local access only");
    //     if (!isAuthorized(token)) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid token");
    //     if (!openLocal) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Open Local Configuration: OFF");

    //     return ResponseEntity.ok("Local Node Height: " + blockchainDB.getHeight());
    // }

    @PostMapping("/send")
    public ResponseEntity<?> sendBean(
        @RequestParam String token,
        @RequestParam String to,
        @RequestParam double amount,
        @RequestParam double gas,
        HttpServletRequest request
    ) throws Exception {
        if (!isLocal(request)) return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Local access only");
        if (!isAuthorized(token)) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid token");
        if (!openLocal) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Open Local Configuration: OFF");

        boolean success = InternalTXFactory.sendAndSignTxInternal(to, amount, gas);
        return ResponseEntity.ok(success ? "TX submitted successfully." : "TX submission failed.");
    }
        
}


