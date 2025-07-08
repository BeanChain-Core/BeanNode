# BeanNode

**BeanNode** is the official Java-based node software that powers the decentralized **BeanChain** network. It validates blocks, gossips transactions, manages peer sync, and coordinates with system-level and contract execution nodes.

This project is maintained by the **BeanChain Core Team** under **Outlandish Tech**, a division of **Outlandish Creative LLC**.

> Actively in development — live, synced nodes (GPN, PN, PRN) are currently operating across the testnet.



---

## 🚀 New in `v0.0.3`

This version introduces new features, dev tools, and a critical bug fix:

- 🧪 **Dev Suite Internal (Beta)** – When no config is found, the node auto-prompts CLI questions and builds a minimal valid config file on startup.
- 🔐 **Encrypted Wiz Key Support** – When no `wiz.key` file is found, the node prompts the user via the `wizHelper` CLI to either:
  - Generate a new key and save to disk.
  - Paste a private key and save as an encrypted or plaintext wizkey.
- 💻 **CLI Enhancements** – Interactive CLI wizards now streamline setup and startup for developers and early node ops.
- 🛠️ **Bug Fix** – Fixed a rejection/mempool cleaning bug that allowed invalid TX `type`s to infinitely gossip through the network.

> Tip: Remove your existing `wiz.key` file to trigger the new `wizHelper` key setup flow.

---

## Node Types

The BeanNode software supports multiple operation modes based on configuration:

- **PN – Public Node**  
  Public-facing node that gossips transactions and exposes Spring Boot APIs for wallets and apps.

- **GPN – Genesis Public Node**  
  The first public node on the network, operated by the core team. Serves as the sync anchor and message propagation root.

- **PRN – Private Node**  
  A sync-only node with no exposed APIs, used internally for redundancy and mirroring.

> While PNs expose REST endpoints, high-volume query traffic should eventually be routed to the **Historical Node** system for scale.

---

## Bean Cluster (Core Team Infrastructure)

The **Bean Cluster** is the coordinated group of core nodes operated and maintained by the **BeanChain Core Team**. It defines and supports the official state of the chain.

### Components

- **GPN – Genesis Public Node**  
  Bootstrap sync anchor and core gossip source.

- **PN – Public Node**  
  Supports the LimaBean Wallet and other first-party DApps.

- **RN – Reward Node** *(Live, expanding)*  
  Handles faucet payouts, airdrops, and validator gas rewards. Will also monitor validator trust scores via ping/pong signals.

- **CEN – Contract Execution Node** *(In Development)*  
  Executes off-chain smart contracts and Layer 2 logic. Will be released in **barebones** form for third-party contract execution.

- **Historical Node** *(In Development)*  
  High-throughput, SQL-based query node exposed through a Kubernetes-powered fetch node cluster.

---

## Features

- Peer-to-peer networking via JSON-over-sockets
- Full transaction and block syncing
- Mempool and TX status tracking
- Smart contract routing via external CENs
- System TX generation via RN
- LevelDB storage for state, mempool, and blocks
- Configurable, extensible architecture
- CLI-driven auto-config and key setup

---

## BeanPack-Java SDK

BeanNode uses the [`BeanPack-Java`](https://github.com/BeanChain-Core/BeanPack-Java) SDK — the official Java SDK for developing BeanChain-compatible nodes, tools, and utilities.

Includes:

- Core models (TX, Block, Wallet)
- Signature, hashing, and verification tools
- Merkle root utilities
- Shared constants and builders

> Additional SDKs (Go, Python, JavaScript) are planned.

---

## Requirements

- Java 21+
- Maven (for build and dependency management)
- Open Ports:
  - `6442` – P2P communication
  - `8080` – Optional REST API (for PNs only)

---

## Running a Node

> A full deployment guide is coming soon.

To join the team or get involved:  
📬 [team@limabean.xyz](mailto:team@limabean.xyz)

[👉 Join our Discord](https://discord.gg/t64HF9B33T)

---

## BaseNode (Developer Toolkit)

A lightweight, network-ready **BaseNode** is also in development — perfect for:

- Oracles and automation
- Custom node logic
- Wallet backends or dashboard tools

> BaseNode is modular and ready for custom events or data flows.

---

## Contributing

We welcome forks and pull requests!

1. Fork this repo
2. Create a new branch
3. Submit a pull request with a clear summary of your changes

   

---

## License

MIT License — See [`LICENSE`](LICENSE)

---

## Part of the BeanChain Ecosystem

- **LimaBean Wallet** — [github.com/BeanChain-Core/LimaBeanWallet](https://github.com/BeanChain-Core/LimaBeanWallet)
- **beanchain.io** — [github.com/BeanChain-Core/BeanChain.io](https://github.com/BeanChain-Core/BeanChain.io)
- **Reward Node (RN)** — [github.com/BeanChain-Core/RN](https://github.com/BeanChain-Core/RN)
- **Contract Execution Node (CEN)** — *(repo coming soon)*
- **Historical Node** — *(repo coming soon)*
- **BaseNode** — [github.com/BeanChain-Core/BaseNode](https://github.com/BeanChain-Core/BaseNode)

---

Crafted with ☕ by the **BeanChain Core Team**  
Under **Outlandish Tech**, powered by **Outlandish Creative LLC**


