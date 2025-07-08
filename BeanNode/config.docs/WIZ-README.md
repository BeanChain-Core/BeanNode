
# 🧙 WizKey & WizCrypt Setup Guide

This file serves as both an introduction to the **WizKey** system and a reminder that this is the expected location for your `wiz.txt` or `wiz.txt.enc` private key file. If this file is missing or invalid, your node may not launch properly in secure/headless mode.

---

## What is a WizKey?

A **WizKey** is a string that contains your private key in a secure format compatible with BeanChain node tools. You can generate and manage your WizKey using the included CLI tools provided by **DevSuite** or manually by placing a valid key file in this folder.

- `wiz.txt` — A plain, unencrypted private key wrapped in a secure WizKey format
- `wiz.txt.enc` — An encrypted WizKey file generated with `WizCrypt`
            - WizCrypt uses a double layered encryption
               - First encrypting your PrivateKey before wrapping it in a WizKey
               - Then encrypting your entire WizKey 'wiz.txt' for total lockdown

You should have **only one** of these files present. If both exist, `wiz.txt.enc` will take priority.

---

## How to Manage Your WizKey

Once you've run `make gBean` or otherwise installed your node, you can open a terminal and:

```bash
cd NodePK
make run-wiz
```

This will launch the **Wiz Helper CLI**, where you can:
- Generate a new private key and save it as a WizKey
- Save your own private key in WizKey format
- Encrypt a WizKey with a password (using WizCrypt v0.0.0)
- Decrypt and view the raw private key

---

## Compatibility

- `WizCrypt` Version: **v0.0.0**
- Compatible with: **BeanNode v0.0.4 and up**

---

## Encryption Settings for Secure Nodes

If you choose to encrypt your WizKey (using option 2 or 4 in the helper CLI), you'll need to update your config settings in `config.docs/settings.config`:

```ini
isEncrypted=true
```

Then choose one of the following setups:

1. **Fully Headless Mode**  
   Set the decryption password as a hardcoded value:
   ```ini
   adminPass=YourPasswordHere
   requirePass=false
   ```

2. **Prompted Password Mode**  
   Leave `adminPass` unset and require input on launch:
   ```ini
   requirePass=true
   ```

We are actively working on a way to pass `adminPass` as a runtime argument for enhanced headless security.

---

Keep your WizKey safe. If you lose it, your funds may be unrecoverable.

# CURRENTLY THESE FILES SHOULDNT BE RENAMED AND YOU MAY NEED TO ADJUST YOUR PRIVATEKEY PATH IN CONFIG TO AN ABSOLUTE PATH 
