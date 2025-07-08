
# BeanNode Headless Mode Launch Guide

This guide explains how to launch a BeanNode in `headless` mode on different terminals and operating systems.

---

## 1. Setup Your `config.properties`

Ensure the following values exist:

```properties
mode=headless

isEncrypted=true            # tells the app to check for encrypted pass 
requirePass=true            # tells the app you are using a non hardcoded adminPass
openLocal=true              # enables /cli/* API for local-only admin access
token=mySuperSecretToken    # used to authenticate local CLI API calls
```

---

## 2. Set the Admin Password (Environment Variable)

Your node must access the admin password at boot time via `BEAN_ADMIN_PASS`.

### Linux / macOS

#### Temporary (only for this terminal session):

```bash
export BEAN_ADMIN_PASS="yourAdminPassword"
java -jar BeanNode-(version).jar
```

#### Run in background (persistent process):

```bash
export BEAN_ADMIN_PASS="yourAdminPassword"
nohup java -jar BeanNode-(version).jar > node.log 2>&1 &
```

#### Using `screen`:

```bash
screen -S mynode
export BEAN_ADMIN_PASS="yourAdminPassword"
java -jar BeanNode-(version).jar
# Press Ctrl+A then D to detach
```

---

### Windows CMD

```cmd
set BEAN_ADMIN_PASS=yourAdminPassword
java -jar BeanNode-(version).jar
```

---

### Windows PowerShell

```powershell
$env:BEAN_ADMIN_PASS="yourAdminPassword"
java -jar BeanNode-(version).jar
```

---

## 3. Access Local CLI API (Optional)

```bash
curl "http://localhost:8080/cli/status?token=mySuperSecretToken"
curl -X POST "http://localhost:8080/cli/shutdown?token=mySuperSecretToken"
```

---

## Flushing the Variable (Optional Security)

If you're in an open session and want to flush the password variable after launch:

### Linux/macOS

```bash
unset BEAN_ADMIN_PASS
```

### PowerShell

```powershell
Remove-Item Env:BEAN_ADMIN_PASS
```

---

## Best Practices

- NEVER hardcode `BEAN_ADMIN_PASS` in scripts without protection.
- Use `.env` files only with proper permissions (600).
- Prefer launching with one-liner:

```bash
BEAN_ADMIN_PASS="yourPassword" java -jar BeanNode-(version).jar
```

This avoids keeping the password in your session.

---

Happy headless node running!
