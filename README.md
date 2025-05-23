# BeanNode

**BeanNode** is a Java-based blockchain node that powers the decentralized **BeanChain** network. It handles block validation, transaction syncing, peer-to-peer messaging, and coordination with external nodes for advanced contract and system-level logic.

This node loads from a configuration file and an encrypted private key file, and can operate as one of the following node types:

- GPN – Genesis Public Node (core chain bootstrap + sync anchor)
- PN – Public Node (API-facing gateway for wallets and apps)
- PRN – Private Node (internal syncing and redundancy layer)

> Still in development — but live, synced nodes are already running across the testnet.

---

## Features

- Peer-to-peer networking via JSON-over-sockets
- Full block and transaction syncing
- Smart contract calls via external Contract Execution Node (CEN)
- System transaction support (airdrop, faucet, validator rewards via RN)
- Mempool and transaction status management
- LevelDB-based state and block storage
- Modular, extensible codebase for layered scaling

---

## Currently In Development

- Validator selection and reward calculation
- Contract Execution Node (CEN) for Layer 2 contracts
- Staking logic, minting, and token support
- Improved sync handling between live node types

---

## Bean Cluster Core Infrastructure

The main **Bean Cluster** is maintained by the core team and consists of:

- **GPN (Genesis Public Node)** – Fully operational  
- **RN (Reward Node)** – Fully operational, handles system-level rewards like faucets, early wallets, and node incentives
    - This is a programatic reward node for system level rewarding and airdrops. The team RN should be the only reward node, other projects can develop rewarding and airdrop systems through CEN nodes and contracts.
- **CEN (Contract Execution Node)** – In development, handles execution of off-chain contract logic and Layer 2 features
    - The team run CEN is the offical team Contract node, but CEN build repo is in development and can be utilized for thrid party custom CEN creation. Official CENs will be registered, and non registered CENs will be flagged for use at the users own risk. 

This structure allows for modular expansion while keeping core chain logic clean and scalable.

---

## Prerequisites

- Java 21+
- Maven (optional for building)
- Open ports:
  - `6442` for P2P networking
  - `8080` for REST API endpoints
 
  - `6443` and `6444` are used internally in the Bean Cluster for internal P2P connections to GPN from CEN and RN

---

## BeanCore SDK (Modular Core Logic)

This node relies on the modular `bean-core` SDK for core transaction and cryptographic logic.

[BeanCore SDK Repository](https://github.com/BeanChain-Core/BeanCore)

Includes:
- Models
- Cryptographic tools (signing, verification, hashing)
- Merkle root tools
- Shared constants


---

## Running a Node

To get started running a node or participating in the BeanChain network,

Contact the dev team: [team@limabean.xyz](mailto:samfawk@limabean.xyz)

A full deployment guide will be added soon.

---

## Contributing

Pull requests and forks are welcome.

- Fork the repository
- Create a new branch for your changes
- Submit a pull request with a clear summary of your work

You can also open issues to report bugs or suggest new features.

---

## License

MIT License — see [`LICENSE`](LICENSE) for license terms.

---

## Part of the BeanChain Ecosystem

BeanNode is one of several interconnected systems powering BeanChain.

Other key components:

- **LimaBean Wallet** (wallet frontend) – [(https://github.com/BeanChain-core/LimaBeanWallet)]
- **beanchain.io** (network explorer/visualizer) – [(https://github.com/BeanChain-core/BeanChain.io)]
- **Reward Node (RN)** – [link to repo coming soon]
- **Contract Execution Node (CEN)** – [link to repo coming soon]

More modules and sidechains will be introduced as development continues.


