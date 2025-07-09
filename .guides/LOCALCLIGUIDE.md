
# BeanNode Local CLI API — Usage Guide

This guide explains how to interact with the LocalAdminController `/cli` API in a headless BeanNode instance with `isPublic=true` and a configured admin token.

---

## Prerequisites

1. **Public Node**: Your node’s `config.properties` must include:
   ```
   isPublic=true
   openLocal=true
   adminPass=YOUR_TOKEN
   ```
2. **Node Running**: Launch your node with environment token set:
   ```bash
   export BEAN_ADMIN_PASS="YOUR_TOKEN"
   nohup java -jar BeanNode-v0.1.0.jar > node.log 2>&1 &
   ```

---

## CLI API Endpoints (Localhost Only)

All requests must be made from `localhost` and include:

- Query param: `token=YOUR_TOKEN`

### 1. `POST /cli/command`

**Shutdown Node**
```bash
curl -X POST "http://localhost:8080/cli/command?token=YOUR_TOKEN&command=shutdown"
```

---

### 2. `GET /cli/status`

Check if the CLI API is active.
```bash
curl "http://localhost:8080/cli/status?token=YOUR_TOKEN"
```

---

### 3. `POST /cli/connect`

Connect to a peer node:
```bash
curl -X POST "http://localhost:8080/cli/connect?token=YOUR_TOKEN&ip=192.168.1.10&port=6442"
```

---

### 4. `GET /cli/height`

Get the current block height.
```bash
curl "http://localhost:8080/cli/height?token=YOUR_TOKEN"
```

---

### 5. `GET /cli/wallet`

Returns current admin wallet info:
```bash
curl "http://localhost:8080/cli/wallet?token=YOUR_TOKEN"
```

---

### 6. `GET /cli/tokens`

Return admin wallet’s token balances.
```bash
curl "http://localhost:6480/cli/tokens?token=YOUR_TOKEN"
```

---

### 7. `GET /cli/lastblock`

View the most recent block:
```bash
curl "http://localhost:8080/cli/lastblock?token=YOUR_TOKEN"
```

---

### 8. `POST /cli/send`

Send BEAN from the admin wallet:
```bash
curl -X POST "http://localhost:8080/cli/send?token=YOUR_TOKEN&to=<recipientAddress>&amount=1.5&gas=0.01"
```

---

## Notes

- These endpoints are **only accessible from localhost** and require both:
  - Valid token
  - `openLocal=true`

- `System.exit(0)` in `/command` is run in a delayed thread to ensure graceful shutdown.

- `send` uses the in-memory `portal.admin` wallet identity. Make sure this wallet is funded.

