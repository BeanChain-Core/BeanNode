# 📝 BeanNode Config Guide — `beanchain.config.properties`

This guide explains how to create a working configuration file for a BeanNode instance (v0.1.0 and up).

---

## 🔧 Basic Setup

Create a new file:

```
beanchain.config.properties
```

Paste in the following template and modify it as needed:

```properties
##############################################################
#  Official BeanChain Node Configuration File (BEANNODE)     #
#  Version: v0.1.0                                            #
#  Node Role: BEANNODE (Standard Validator/Network Node)     #
##############################################################

########## SECURITY & ACCESS ##########

mode=portal
privateKeyPath=config.docs/wiz.txt
encryptedWiz=true
requirePass=true
adminPass=
openLocal=true
token.cli=beanabean42

########## NETWORK SETTINGS ##########

bindAddress=0.0.0.0
networkPort=6442
peerPort=6442
isBootstrapNode=false
bootstrapIp=66.179.82.188
syncMode=FULL
nodeType=BEANNODE
isPublicNode=true

########## DATABASE SETTINGS ##########

chainDB=chainDB
stateDB=stateDB
mempoolDB=mempoolDB
rejectedDB=rejectedDB
layer2DB=layer2DB
```

---

## ⚙️ Node Role Notes

- **GPN (Genesis Public Node(CHAINSTARTER))**:
  - Set `isBootstrapNode=true`
  - Remove or comment out `bootstrapIp`

- **PN (Public Node)**:
  - Set `isBootstrapNode=false`
  - Use a valid GPN IP for `bootstrapIp`

- **CEN / RN**:
  - Set `nodeType=CEN` or `RN` as appropriate
  - Adjust paths and behavior according to node logic

---

## 🛠 CLI Access

Enable secure local admin control:

```properties
openLocal=true
token.cli=YOUR_TOKEN
```

Use with:
```
curl "http://localhost:8080/cli/status?token=YOUR_TOKEN"
```
_For details on using the local CLI API, see:_  
[`LOCALCLIGUIDE.md`](LOCALCLIGUIDE.md)
---

## ✅ Final Tips

- Always keep `token.cli` private.
- Use different `data/` folders for each node to avoid corruption.
- Consider using the `WizCrypt` helper to encrypt `wiz.txt`.

---