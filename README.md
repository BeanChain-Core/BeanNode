# BeanNode

**BeanNode** is the official Java-based node software that powers the decentralized **BeanChain** network. It validates blocks, gossips transactions, manages peer sync, and coordinates with system-level and contract execution nodes.

This project is maintained by the **BeanChain Core Team** under **Outlandish Tech**, a division of **Outlandish Creative LLC**.

> Actively in development — live, synced nodes (GPN, PN, PRN) are currently operating across the testnet.

---

## Node Types

The BeanNode software supports multiple operation modes based on configuration:

- **PN – Public Node**  
  Public-facing node that gossips transactions and exposes Spring Boot APIs for wallets and apps.

- **GPN – Genesis Public Node**  
  The first public node on the network, operated by the core team. Serves as the sync anchor and message propagation root.

- **PRN – Private Node**  
  A sync-only node with no exposed APIs, used internally for redundancy and mirroring.

> Note: While PNs (including the GPN) expose REST endpoints, high-volume query traffic should eventually be routed to the **Historical Node** system, which is optimized for data serving at scale.

---

## Bean Cluster (Core Team Infrastructure)

The **Bean Cluster** is the coordinated group of core nodes operated and maintained by the **BeanChain Core Team**. It defines and supports the official state of the chain.

### Components:

- **GPN – Genesis Public Node**  
  Bootstrap sync anchor and core gossip source.

- **PN – Public Node**  
  Supports the LimaBean Wallet and other first-party DApps by relaying transactions into the network.

- **RN – Reward Node** *(Live, expanding)*  
  Issues faucet payouts, airdrops, and validator gas rewards. Also being developed to track **validator trust scores** via ping/pong behavior monitoring.

- **CEN – Contract Execution Node** *(In Development)*  
  Executes off-chain smart contracts and Layer 2 logic. Hosts team-built contracts (e.g., staking) and will be released in **barebones form** to allow third-party customization and CEN development.  
  Devs can connect their CENs to open PNs to extend the network.

- **Historical Node** *(In Development)*  
  Peripheral node that stores validated blocks in a SQL database and exposes high-performance endpoints via a **Kubernetes-powered cluster of fetch nodes**.  
  Will be released in **barebones, pluggable form** for community use in DApps, explorers, or analytics tools.

> This modularity and peripheral flexibility is a major strength of BeanChain — empowering developers to extend, customize, and scale the network.

---

## Features

- Peer-to-peer networking via JSON-over-sockets
- Full transaction and block syncing
- Mempool and TX status tracking
- Smart contract routing via external CENs
- System TX generation via RN
- LevelDB storage for state, mempool, and blocks
- Configurable, extensible architecture

---

## BeanPack-Java SDK

BeanNode uses the [`BeanPack-Java`](https://github.com/BeanChain-Core/BeanPack-Java) SDK — the official Java SDK for developing BeanChain-compatible nodes, tools, and utilities.

Includes:

- Core models (TX, Block, Wallet)
- Signature, hashing, and verification tools
- Merkle root utilities
- Shared constants and builders

> Additional SDKs (Go, Python, JavaScript) are planned to enable multi-language support across the ecosystem.

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
[team@limabean.xyz](mailto:team@limabean.xyz)

---

## BaseNode (Developer Toolkit)

A lightweight, network-ready **BaseNode** project is also in development. It will provide a minimal Java node that connects to the BeanChain network and receives messages — ready to:

- Run custom logic or automation
- Act as an oracle or external data gateway
- Serve as the base for wallet apps, dashboard tools, or backend utilities

> Designed for modular integrations — BaseNode will simplify the creation of custom, event-driven logic on the BeanChain network.

---

## Contributing

We welcome forks and pull requests:

1. Fork this repo
2. Create a new branch
3. Submit a pull request with a clear summary of your changes

Issues are open for bugs, ideas, and feature suggestions.

---

## License

MIT License — See [`LICENSE`](LICENSE) for full details.

---

## Part of the BeanChain Ecosystem

BeanNode is one of several official projects within the network:

- **LimaBean Wallet** — Wallet interface  
  [github.com/BeanChain-Core/LimaBeanWallet](https://github.com/BeanChain-Core/LimaBeanWallet)

- **beanchain.io** — Network visualizer and explorer  
  [github.com/BeanChain-Core/BeanChain.io](https://github.com/BeanChain-Core/BeanChain.io)

- **Reward Node (RN)** — System reward engine 
  [github.com/BeanChain-Core/RN](https://github.com/BeanChain-Core/RN)

- **Contract Execution Node (CEN)** — Contract processor *(repo coming soon)*  
- **Historical Node** — High-throughput query node *(repo coming soon)*  
- **BaseNode** — Lightweight modular tool node 
  

---

Crafted by the **BeanChain Core Team**  
Under **Outlandish Tech**, powered by **Outlandish Creative LLC**


